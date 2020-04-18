package com.github.forax.pro.plugin.compiler;

import static com.github.forax.pro.api.MutableConfig.derive;
import static com.github.forax.pro.api.helper.OptionAction.actionMaybe;
import static com.github.forax.pro.api.helper.OptionAction.exists;
import static com.github.forax.pro.api.helper.OptionAction.gatherAll;
import static com.github.forax.pro.api.helper.OptionAction.rawValues;
import static com.github.forax.pro.api.helper.OptionAction.toPrettyString;
import static com.github.forax.pro.helper.FileHelper.deleteAllFiles;
import static com.github.forax.pro.helper.FileHelper.pathFilenameEndsWith;
import static com.github.forax.pro.helper.FileHelper.pathFilenameEquals;
import static com.github.forax.pro.helper.FileHelper.walkAndFindCounterpart;
import static com.github.forax.pro.helper.FileHelper.walkIfNecessary;
import static com.github.forax.pro.helper.ModuleHelper.mergeModuleDescriptor;
import static com.github.forax.pro.helper.ModuleHelper.moduleDescriptorToSource;
import static com.github.forax.pro.helper.ModuleSourceLayout.JDK_LAYOUT;
import static com.github.forax.pro.helper.util.Unchecked.getUnchecked;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.walk;
import static java.nio.file.Files.write;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.util.function.Function.identity;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;
import com.github.forax.pro.api.helper.CmdLine;
import com.github.forax.pro.api.helper.OptionAction;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.helper.FileHelper;
import com.github.forax.pro.helper.Log;
import com.github.forax.pro.helper.ModuleHelper;
import com.github.forax.pro.helper.ModuleHelper.ResolverListener;
import com.github.forax.pro.helper.ModuleSourceLayout;
import java.io.File;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

public class CompilerPlugin implements Plugin {
  @Override
  public String name() {
    return "compiler";
  }

  @Override
  public void init(MutableConfig config) {
    var compilerConf = config.getOrUpdate(name(), CompilerConf.class);
    compilerConf.processorModuleSourcePath(List.of());
    compilerConf.processorModuleTestPath(List.of());
  }
  
  @Override
  public void configure(MutableConfig config) {
    var compilerConf = config.getOrUpdate(name(), CompilerConf.class);
    var convention = config.getOrThrow("convention", ConventionFacade.class);
    
    // inputs
    derive(compilerConf, CompilerConf::moduleDependencyPath, convention, ConventionFacade::javaModuleDependencyPath);
    derive(compilerConf, CompilerConf::moduleSourcePath, convention, ConventionFacade::javaModuleSourcePath);
    derive(compilerConf, CompilerConf::moduleSourceResourcesPath, convention, ConventionFacade::javaModuleSourceResourcesPath);
    derive(compilerConf, CompilerConf::moduleTestPath, convention, ConventionFacade::javaModuleTestPath);
    derive(compilerConf, CompilerConf::moduleTestResourcesPath, convention, ConventionFacade::javaModuleTestResourcesPath);
    
    // outputs
    derive(compilerConf, CompilerConf::moduleExplodedSourcePath, convention, c -> c.javaModuleExplodedSourcePath().get(0));
    derive(compilerConf, CompilerConf::moduleMergedTestPath, convention, c -> c.javaModuleMergedTestPath().get(0));
    derive(compilerConf, CompilerConf::moduleExplodedTestPath, convention, c -> c.javaModuleExplodedTestPath().get(0));
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
    var compilerConf = config.getOrThrow(name(), CompilerConf.class);
    compilerConf.moduleDependencyPath().forEach(registry::watch);
    compilerConf.moduleSourcePath().forEach(registry::watch);
    compilerConf.moduleSourceResourcesPath().forEach(registry::watch);
    compilerConf.processorModuleSourcePath().forEach(registry::watch);
    compilerConf.moduleTestPath().forEach(registry::watch);
    compilerConf.moduleTestResourcesPath().forEach(registry::watch);
    compilerConf.processorModuleTestPath().forEach(registry::watch);
  }
  
  private enum JavacOption {
    RELEASE(actionMaybe("--release", Javac::release)),
    VERBOSE(exists("-verbose", Javac::verbose)),
    LINT(javac -> javac.lint().map(lint -> line -> line.add("-Xlint:" + lint))),
    ENABLE_PREVIEW(exists("--enable-preview", Javac::enablePreview)),
    RAW_ARGUMENTS(rawValues(Javac::rawArguments)),
    DESTINATION(actionMaybe("-d", Javac::destination)),
    MODULE_SOURCE_PATH(actionMaybe("--module-source-path", Javac::moduleSourcePath, File.pathSeparator)),
    SOURCE_PATH(actionMaybe("-sourcepath", Javac::sourcePath, File.pathSeparator)),
    MODULE(actionMaybe("--module", Javac::module)),
    ROOT_MODULES(actionMaybe("--add-modules", Javac::rootModules, ",")),
    UPGRADE_MODULE_PATH(actionMaybe("--upgrade-module-path", Javac::upgradeModulePath, File.pathSeparator)),
    PROCESSOR_MODULE_PATH(actionMaybe("--processor-module-path", Javac::processorModulePath, File.pathSeparator)),
    MODULE_PATH(actionMaybe("--module-path", Javac::modulePath, File.pathSeparator)),
    
    CLASS_PATH(actionMaybe("-classpath", Javac::classPath, File.pathSeparator)),
    PROCESSOR_PATH(actionMaybe("--processor-path", Javac::processorPath, File.pathSeparator))
    ;
    
    final OptionAction<Javac> action;
    
    JavacOption(OptionAction<Javac> action) {
      this.action = action;
    }
  }

  private static List<Path> concat(List<Path> path1, Path path2) {
    return Stream.concat(path1.stream(), Stream.of(path2)).filter(Files::exists).collect(toList());
  }
  private static List<Path> concat(List<Path> path1, List<Path> path2) {
    return Stream.concat(path1.stream(), path2.stream()).filter(Files::exists).collect(toList());
  }


  private static List<Path> computeModulePath(Optional<List<Path>> modulePath, Supplier<List<Path>> defaultModulePath) {
    return modulePath
        .or(() -> Optional.of(defaultModulePath.get()))
        .orElseGet(List::of)
        .stream().filter(Files::exists)
        .collect(toUnmodifiableList());
  }

  private static List<Path> expandPath(List<Path> paths) {
    return paths.stream().flatMap(p -> FileHelper.expand(p).stream()).filter(Files::exists).collect(toUnmodifiableList());
  }

  @Override
  public int execute(Config config) throws IOException {
    var log = Log.create(name(), config.getOrThrow("pro", ProConf.class).loglevel());
    log.debug(config, conf -> "config " + config);
    
    var javacTool = ToolProvider.findFirst("javac")
        .orElseThrow(() -> new IllegalStateException("can not find javac"));
    var compiler = config.getOrThrow(name(), CompilerConf.class);

    // find layout
    var sourceLayoutFactory = ModuleSourceLayout.Factory.of(ModuleSourceLayout::lookupForJdkLayout)
        .or(ModuleSourceLayout::lookupForMavenMultiLayout);
    var layoutOpt = sourceLayoutFactory.createLayout(Path.of("."));
    if (layoutOpt.isEmpty()) {
      log.error(null, __ -> "no source layout found");
      return 1; //FIXME
    }
    var layout = layoutOpt.orElseThrow();
    log.verbose(layout, _layout -> layout + " detected");

    // find source modules and source module finder
    var sourceModuleRefs = ModuleHelper.topologicalSort(layout.findModuleRefs(compiler.moduleSourcePath()));
    var moduleSourceFinder = ModuleHelper.moduleFinder(sourceModuleRefs);
    log.verbose(moduleSourceFinder, finder -> "module source finder: " + finder);
    if (!sourceModuleRefs.isEmpty()) {
      // find module source path, processor source path, module path an resource path
      var compilerModuleSourcePath = layout.toAllPath(compiler.moduleSourcePath());
      var compilerProcessorModuleSourcePath = compiler.processorModuleSourcePath();
      var compilerModuleSourceResourcePath = compiler.moduleSourceResourcesPath();
      var compilerModulePath = computeModulePath(compiler.modulePath(), compiler::moduleDependencyPath);

      log.verbose(compilerModuleSourcePath, path -> "compilerModuleSourcePath: " + path);
      log.verbose(compilerProcessorModuleSourcePath,
          path -> "compilerProcessorModuleSourcePath: " + path);
      log.verbose(compilerModuleSourceResourcePath,
          path -> "compilerModuleSourceResourcePath: " + path);
      log.verbose(compilerModulePath, path -> "compilerModulePath: " + path);

      var errorCode = compile(log, javacTool,
          sourceModuleRefs,
          moduleSourceFinder,
          compiler.sourceRelease(),
          compiler,
          compilerModuleSourcePath,
          compilerProcessorModuleSourcePath,
          compilerModulePath,
          compilerModuleSourceResourcePath,
          layout,
          compiler.moduleExplodedSourcePath(),
          "source:");
      if (errorCode != 0) {
        return errorCode;
      }
    } else {
      // no source
      log.info(null, __ -> "no source found");
    }

    // find test modules and finder
    var testModuleRefs = layout.findModuleRefs(compiler.moduleTestPath());
    if (!testModuleRefs.isEmpty()) {
      var compilerModuleMergedTestPath = compiler.moduleMergedTestPath();
      var compilerProcessorModuleTestPath = concat(compiler.processorModuleSourcePath(), compiler.processorModuleTestPath());
      var compilerModuleTestResourcePath = concat(compiler.moduleSourceResourcesPath(), compiler.moduleTestResourcesPath());
      var compilerModulePath = computeModulePath(compiler.modulePath(),
          () -> concat(compiler.moduleDependencyPath(), compiler.moduleExplodedSourcePath()));

      log.verbose(compilerModuleMergedTestPath, path -> "compilerModuleMergedTestPath: " + path);
      log.verbose(compilerProcessorModuleTestPath, path -> "compilerProcessorModuleTestPath: " + path);
      log.verbose(compilerModuleTestResourcePath, path -> "compilerModuleTestResourcePath: " + path);
      log.verbose(compilerModulePath, path -> "compilerModulePath: " + path);

      deleteAllFiles(compilerModuleMergedTestPath, false);
      merge(log, moduleSourceFinder, testModuleRefs, compilerModuleMergedTestPath);

      // reload testModuleRefs but from merged to get the module-infos right
      var mergedTestModuleRefs = ModuleHelper.topologicalSort(JDK_LAYOUT.findModuleRefs(List.of(compilerModuleMergedTestPath)));
      // find test module finder
      var moduleMergedTestFinder = ModuleHelper.moduleFinder(mergedTestModuleRefs);
      log.verbose(moduleMergedTestFinder, finder -> "module test merged finder: " + finder);

      return compile(log, javacTool,
          mergedTestModuleRefs,
          moduleMergedTestFinder,
          compiler.testRelease().or(compiler::sourceRelease),
          compiler,
          List.of(compilerModuleMergedTestPath),
          compilerProcessorModuleTestPath,
          compilerModulePath,
          compilerModuleTestResourcePath,
          layout,
          compiler.moduleExplodedTestPath(),
          "test:");
    } else {
      // no test
      log.info(null, __ -> "no test found");
    }

    return 0;
  }

  private static boolean resolveModuleGraph(Log log, ModuleFinder moduleFinder, List<Path> compilerModulePath,  String pass) {
    var dependencyFinder = ModuleFinder.compose(
        compilerModulePath.stream()
            .map(ModuleFinder::of)
            .toArray(ModuleFinder[]::new));
    var systemFinder = ModuleHelper.systemModulesFinder();

    log.debug(moduleFinder, finder -> pass + " modules " + finder.findAll().stream().map(ref -> ref.descriptor().name()).sorted().collect(joining(", ")));
    log.debug(dependencyFinder, finder -> pass + " dependency modules " + finder.findAll().stream().map(ref -> ref.descriptor().name()).sorted().collect(joining(", ")));
    log.debug(systemFinder, finder -> pass + " system modules " + finder.findAll().stream().map(ref -> ref.descriptor().name()).sorted().collect(joining(", ")));

    return ModuleHelper.resolveOnlyRequires(
        ModuleFinder.compose(moduleFinder, dependencyFinder, systemFinder),
        moduleFinder.findAll().stream().map(ref -> ref.descriptor().name()).collect(toList()),
        new ResolverListener() {
          @Override
          public void module(String moduleName) {
            // empty
          }
          @Override
          public void dependencyNotFound(String moduleName, String dependencyChain) {
            log.error(null, __ -> pass + " can not resolve " + moduleName + " from " + dependencyChain);
          }
        });
  }

  private static int compile(Log log, ToolProvider javacTool,
      Set<ModuleReference> moduleRefs,
      ModuleFinder moduleFinder,
      Optional<Integer> release,
      CompilerConf compiler,
      List<Path> compilerModuleSourcePath,
      List<Path> compilerProcessorModulePath,
      List<Path> compilerModulePath,
      List<Path> resourcesPath,
      ModuleSourceLayout layout,
      Path destination,
      String pass) throws IOException {

    // try to resolve the module graph to see if something is missing ?
    var resolved = resolveModuleGraph(log, moduleFinder, compilerModulePath,  pass);
    if (!resolved) {
      return 1;  //FIXME
    }
    
    deleteAllFiles(destination, false);
    
    var javac = new Javac();
    release.ifPresent(javac::release);
    compiler.verbose().ifPresent(javac::verbose);
    compiler.lint().ifPresent(javac::lint);
    compiler.enablePreview().ifPresent(javac::enablePreview);
    compiler.rawArguments().ifPresent(javac::rawArguments);
    compiler.upgradeModulePath().ifPresent(javac::upgradeModulePath);
    compiler.module().ifPresent(javac::module);
    compiler.rootModules().ifPresent(javac::rootModules);

    var compatibilityMode = release.map(_release -> _release <= 8).orElse(false);
    if (!compatibilityMode) {
      // module mode, compile all java files at once using moduleSourcePath
      javac.moduleSourcePath(compilerModuleSourcePath);
      Optional.of(compilerModulePath).filter(not(List::isEmpty)).ifPresent(javac::modulePath);
      Optional.of(compilerProcessorModulePath).filter(not(List::isEmpty)).ifPresent(javac::processorModulePath);
      javac.destination(destination);

      var files = compiler.files()
          .orElseGet(() -> walkIfNecessary(expandPath(compilerModuleSourcePath), pathFilenameEndsWith(".java")));

      var errorCode = compileAllFiles(log, javacTool, javac, release, files);
      if (errorCode != 0) {
        return errorCode;
      }
    } else {
      // compatibility mode, compile modules one by one, moduleRefs is already in topological order
      var classPath = compilerModulePath.stream().flatMap(CompilerPlugin::asClassPath).collect(toUnmodifiableList());
      var processorPath = compilerProcessorModulePath.stream().flatMap(CompilerPlugin::asClassPath).collect(toUnmodifiableList());
      Optional.of(classPath).filter(not(List::isEmpty)).ifPresent(javac::classPath);
      Optional.of(processorPath).filter(not(List::isEmpty)).ifPresent(javac::processorPath);

      for(var moduleRef: moduleRefs) {
        // compile modules one by one using the sourcePath, without the module-info
        var sourcePath = layout.toModulePath(moduleRef, compiler.moduleSourcePath());
        javac.sourcePath(sourcePath);
        javac.destination(destination.resolve(moduleRef.descriptor().name()));
        var moduleInfo = pathFilenameEquals("module-info.java");
        var files = compiler.files()
            .orElseGet(() -> walkIfNecessary(sourcePath, pathFilenameEndsWith(".java").and(not(moduleInfo))));
        var errorCode = compileAllFiles(log, javacTool, javac, release, files);
        if (errorCode != 0) {
          return errorCode;
        }
        
        // compile the module-info with release 9
        javac.classPath(null); // reset classpath
        Optional.of(compilerModulePath).filter(not(List::isEmpty)).ifPresent(javac::modulePath);  // use modulePath instead
        files = walkIfNecessary(sourcePath, moduleInfo);
        errorCode = compileAllFiles(log, javacTool, javac, Optional.of(9), files);
        if (errorCode != 0) {
          return errorCode;
        }
      }
    }
    
    //copy all resources
    copyAllResources(log, moduleRefs, resourcesPath, layout, destination);

    // declare all services for the classpath
    declareAllServicesForTheClassPath(moduleRefs, destination);

    return 0;
  }


  private static int compileAllFiles(Log log, ToolProvider javacTool, Javac javac, Optional<Integer> release, List<Path> files) {
    release.ifPresent(javac::release);
    var cmdLine = gatherAll(JavacOption.class, option -> option.action).apply(javac, new CmdLine());
    files.forEach(cmdLine::add);

    var arguments = cmdLine.toArguments();
    log.verbose(files, fs -> toPrettyString(JavacOption.class, option -> option.action).apply(javac, "javac") + "\n" + fs.stream().map(Path::toString).collect(joining(" ")));

    return javacTool.run(System.out, System.err, arguments);
  }

  private static void copyAllResources(Log log, Set<ModuleReference> moduleRefs,
      List<Path> resourcesPath, ModuleSourceLayout layout, Path destination) {
    for(var moduleRef: moduleRefs) {
      for(var resourceDir : layout.toModulePath(moduleRef, resourcesPath)) {
        var destinationDir = destination.resolve(moduleRef.descriptor().name());
        log.verbose(null, __ -> "copy resources from directory " + resourceDir + " to " + destinationDir);
        walkAndFindCounterpart(resourceDir, destinationDir, identity(), (src, dest) -> {
          if (isDirectory(src) && isDirectory(dest)) { // do not overwrite directory
            return;
          }
          copy(src, dest);
        });
      }
    }
  }

  private static void declareAllServicesForTheClassPath(Set<ModuleReference> moduleRefs,
      Path destination) throws IOException {
    for(var moduleRef: moduleRefs) {
      var descriptor = moduleRef.descriptor();
      var moduleFolder = destination.resolve(descriptor.name());
      var servicesPath = moduleFolder.resolve("META-INF/services/");
      var provides = descriptor.provides();
      if (!provides.isEmpty()) {
        Files.createDirectories(servicesPath);
      }
      for(var provide: provides) {
        var servicePath = servicesPath.resolve(provide.service());
        write(servicePath, (Iterable<String>)provide.providers().stream()::iterator, CREATE_NEW);
      }
    }
  }

  private static Stream<Path> asClassPath(Path path) {
    // IOExceptions suppressed
    return getUnchecked(() -> {
      if (isDirectory(path)) {
        var jars = walk(path).filter(p -> p.getFileName().toString().endsWith(".jar")).collect(toUnmodifiableList());
        if (!jars.isEmpty()) {
          return jars.stream();  
        }
      }
      return Stream.of(path);
    });
  }
  
  private static void merge(Log log, ModuleFinder moduleSourceFinder,
      Set<ModuleReference> testModuleRefs, Path moduleMergedTestPath) throws IOException {
    Files.createDirectories(moduleMergedTestPath);

    log.verbose(testModuleRefs, __ -> "merge testModuleRefs: " + testModuleRefs);
    log.verbose(moduleSourceFinder, __ -> "merge moduleSourceFinder: " + moduleSourceFinder);

    for(var testRef: testModuleRefs) {
      var testModuleName = testRef.descriptor().name();
      var testModuleDestination = moduleMergedTestPath.resolve(testModuleName);

      Predicate<Path> predicate;
      var sourceRefOpt = moduleSourceFinder.find(testModuleName);
      if (sourceRefOpt.isPresent()) {
        var sourceRef = sourceRefOpt.orElseThrow();

        var sourcePath = Path.of(sourceRef.location().orElseThrow());
        var skipModuleInfoDotJava = not(pathFilenameEquals("module-info.java"));

        log.verbose(null, __ -> "copy source from directory " + sourcePath + " to " + testModuleDestination);
        walkAndFindCounterpart(sourcePath, testModuleDestination,
            stream -> stream.filter(skipModuleInfoDotJava),
            (source, target) -> {
              log.debug(null, __ -> "copy file " + source + " to " + target);
              Files.copy(source, target);
            });


        var descriptor = mergeModuleDescriptor(sourceRef.descriptor(), testRef.descriptor());
        write(testModuleDestination.resolve("module-info.java"), List.of(moduleDescriptorToSource(descriptor)));
        
        predicate = skipModuleInfoDotJava;
        
      } else {
        predicate = __ -> true;
      }

      var testPath = Path.of(testRef.location().orElseThrow());
      log.verbose(null, __ -> "copy test from directory " + testPath + " to " + testModuleDestination);
      walkAndFindCounterpart(testPath, testModuleDestination, stream -> stream.filter(predicate),
          (srcPath, dstPath) -> {
            if (exists(dstPath) && isDirectory(dstPath)) {
              return;  // skip existing path
            }
            log.debug(null, __ -> "copy file " + srcPath + " to " + dstPath);
            copy(srcPath, dstPath);
          });

    }
  }
}
