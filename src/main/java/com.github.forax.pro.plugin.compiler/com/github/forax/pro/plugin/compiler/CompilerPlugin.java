package com.github.forax.pro.plugin.compiler;

import static com.github.forax.pro.api.helper.OptionAction.action;
import static com.github.forax.pro.api.helper.OptionAction.actionMaybe;
import static com.github.forax.pro.api.helper.OptionAction.exists;
import static com.github.forax.pro.api.helper.OptionAction.gatherAll;
import static com.github.forax.pro.helper.FileHelper.deleteAllFiles;
import static com.github.forax.pro.helper.FileHelper.pathFilenameEndsWith;
import static com.github.forax.pro.helper.FileHelper.walkIfNecessary;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;
import com.github.forax.pro.api.helper.CmdLine;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.api.helper.OptionAction;
import com.github.forax.pro.helper.FileHelper;
import com.github.forax.pro.helper.Log;
import com.github.forax.pro.helper.ModuleHelper;
import com.github.forax.pro.helper.util.StableList;

public class CompilerPlugin implements Plugin {
  @Override
  public String name() {
    return "compiler";
  }

  @Override
  public void init(MutableConfig config) {
    CompilerConf compiler = config.getOrUpdate(name(), CompilerConf.class);
    compiler.release(9);
  }
  
  @Override
  public void configure(MutableConfig config) {
    CompilerConf compiler = config.getOrUpdate(name(), CompilerConf.class);
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class);
    
    // inputs
    compiler.moduleDependencyPath(convention.javaModuleDependencyPath());
    compiler.moduleSourcePath(convention.javaModuleSourcePath());
    compiler.moduleTestPath(convention.javaModuleTestPath());
    
    // outputs
    compiler.moduleExplodedSourcePath(convention.javaModuleExplodedSourcePath().get(0));
    compiler.moduleMergedTestPath(convention.javaModuleMergedTestPath().get(0));
    compiler.moduleExplodedTestPath(convention.javaModuleExplodedTestPath().get(0));
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
    CompilerConf compiler = config.getOrThrow(name(), CompilerConf.class);
    compiler.moduleDependencyPath().forEach(registry::watch);
    compiler.moduleSourcePath().forEach(registry::watch);
    compiler.moduleTestPath().forEach(registry::watch);
  }
  
  static Optional<List<Path>> modulePathOrDependencyPath(Optional<List<Path>> modulePath, List<Path> moduleDependencyPath, List<Path> additionnalPath) {
    return modulePath
             .or(() -> Optional.of(
                    new StableList<Path>().appendAll(moduleDependencyPath).appendAll(additionnalPath)))
             .map(FileHelper.unchecked(FileHelper::pathFromFilesThatExist))
             .filter(list -> !list.isEmpty());
  }
  
  enum JavacOption {
    RELEASE(action("--release", Javac::release)),
    VERBOSE(exists("--verbose", Javac::verbose)),
    DESTINATION(action("-d", Javac::destination)),
    MODULE_SOURCE_PATH(action("--module-source-path", Javac::moduleSourcePath, ":")),
    MODULE_PATH(actionMaybe("--module-path", Javac::modulePath, ":")),
    ;
    
    final OptionAction<Javac> action;
    
    private JavacOption(OptionAction<Javac> action) {
      this.action = action;
    }
  }
  
  @Override
  public int execute(Config config) throws IOException {
    Log log = Log.create(name(), config.getOrThrow("pro", ProConf.class).loglevel());
    log.debug(config, conf -> "config " + config);
    
    ToolProvider javacTool = ToolProvider.findFirst("javac")
        .orElseThrow(() -> new IllegalStateException("can not find javac"));
    CompilerConf compiler = config.getOrThrow(name(), CompilerConf.class);
    
    
    ModuleFinder moduleSourceFinder = ModuleHelper.sourceModuleFinders(compiler.moduleSourcePath());
    int errorCode = compile(log, javacTool, compiler, compiler.moduleSourcePath(), moduleSourceFinder, List.of(), compiler.moduleExplodedSourcePath(), "source:");
    if (errorCode != 0) {
      return errorCode;
    }
    List<Path> moduleTestPath = FileHelper.pathFromFilesThatExist(compiler.moduleTestPath());
    if (moduleTestPath.isEmpty()) {
      return 0;
    }
    
    Path moduleMergedTestPath = compiler.moduleMergedTestPath();
    deleteAllFiles(moduleMergedTestPath, false);
    
    ModuleFinder moduleTestFinder = ModuleHelper.sourceModuleFinders(compiler.moduleTestPath());
    errorCode = merge(moduleSourceFinder, moduleTestFinder, moduleMergedTestPath);
    if (errorCode != 0) {
      return errorCode;
    }
    
    ModuleFinder moduleMergedTestFinder = ModuleHelper.sourceModuleFinder(compiler.moduleMergedTestPath());
    return compile(log, javacTool, compiler, List.of(moduleMergedTestPath), moduleMergedTestFinder, List.of(compiler.moduleExplodedSourcePath()), compiler.moduleExplodedTestPath(), "test:");
  }

  private static int compile(Log log, ToolProvider javacTool, CompilerConf compiler, List<Path> moduleSourcePath, ModuleFinder moduleFinder, List<Path> additionalSourcePath, Path destination, String pass) throws IOException {
    Optional<List<Path>> modulePath = modulePathOrDependencyPath(compiler.modulePath(),
        compiler.moduleDependencyPath(), additionalSourcePath);
    
    ModuleFinder dependencyFinder = ModuleFinder.compose(
        modulePath
            .stream()
            .flatMap(List::stream)
            .map(ModuleFinder::of)
            .toArray(ModuleFinder[]::new));
    List<String> rootSourceNames = moduleFinder.findAll().stream()
            .map(ref -> ref.descriptor().name())
            .collect(Collectors.toList());
    
    /*
    Configuration.resolveRequires(ModuleFinder.compose(sourceModuleFinder, dependencyFinder),
        List.of(Layer.boot().configuration()), ModuleFinder.of(), rootNames);
    */
    boolean resolved = ModuleHelper.resolveOnlyRequires(
        ModuleFinder.compose(moduleFinder, dependencyFinder, ModuleFinder.ofSystem()),
        rootSourceNames,
        (moduleName, dependencyChain) -> {
          log.error(null, __ -> pass + " can not resolve " + moduleName + " from " + dependencyChain);
        });
    if (!resolved) {
      return 1;  //FIXME
    }
    
    deleteAllFiles(destination, false);
    
    Javac javac = new Javac(compiler.release(), destination, moduleSourcePath);
    compiler.verbose().ifPresent(javac::verbose);
    modulePath.ifPresent(javac::modulePath);
    
    CmdLine cmdLine = gatherAll(JavacOption.class, option -> option.action).apply(javac, new CmdLine());
    List<Path> files = compiler.files().orElseGet(
        () -> walkIfNecessary(moduleSourcePath, pathFilenameEndsWith(".java")));  //FIXME, use rootNames ??
    files.forEach(cmdLine::add);
    String[] arguments = cmdLine.toArguments();
    
    log.verbose(arguments, args -> "javac " + String.join(" ", args));
    return javacTool.run(System.out, System.err, arguments);
  }
  
  private static int merge(ModuleFinder moduleSourceFinder, ModuleFinder moduleTestFinder,
                           Path moduleMergedTestPath) throws IOException {
    Files.createDirectories(moduleMergedTestPath);
    
    Predicate<Path> skipModuleInfoDotJava = FileHelper.pathFilenameEquals("module-info.java").negate();
    
    for(ModuleReference testRef: moduleTestFinder.findAll()) {
      String moduleName = testRef.descriptor().name();
      Path moduleRoot = moduleMergedTestPath.resolve(moduleName);
      
      Predicate<Path> predicate;
      Optional<ModuleReference> sourceRefOpt = moduleSourceFinder.find(moduleName);
      if (sourceRefOpt.isPresent()) {
        ModuleReference sourceRef = sourceRefOpt.get();
        
        Path sourcePath = Paths.get(sourceRef.location().get());
        FileHelper.walkAndFindCounterpart(sourcePath, moduleRoot,
            stream -> stream.filter(skipModuleInfoDotJava),
            Files::copy);
        
        ModuleDescriptor descriptor = ModuleHelper.mergeModuleDescriptor(sourceRef.descriptor(), testRef.descriptor());
        Files.write(moduleRoot.resolve("module-info.java"), List.of(ModuleHelper.moduleDescriptorToSource(descriptor)));
        
        predicate = skipModuleInfoDotJava;
        
      } else {
        predicate = __ -> true;
      }
      
      Path testPath = Paths.get(testRef.location().get());
      FileHelper.walkAndFindCounterpart(testPath, moduleRoot, stream -> stream.filter(predicate),
          (srcPath, dstPath) -> {
            if (Files.exists(dstPath) && Files.isDirectory(dstPath)) {
              return;  // skip existing path
            }
            Files.copy(srcPath, dstPath);
          });
    }
    return 0;
  }
}
