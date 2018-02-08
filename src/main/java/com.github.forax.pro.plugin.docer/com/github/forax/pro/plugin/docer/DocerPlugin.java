package com.github.forax.pro.plugin.docer;


import static com.github.forax.pro.api.MutableConfig.derive;
import static com.github.forax.pro.api.helper.OptionAction.action;
import static com.github.forax.pro.api.helper.OptionAction.actionMaybe;
import static com.github.forax.pro.api.helper.OptionAction.gatherAll;
import static com.github.forax.pro.api.helper.OptionAction.rawValues;
import static com.github.forax.pro.helper.FileHelper.deleteAllFiles;
import static com.github.forax.pro.helper.FileHelper.pathFilenameEndsWith;
import static com.github.forax.pro.helper.FileHelper.walkIfNecessary;

import java.io.File;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

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
import com.github.forax.pro.helper.util.StableList;

public class DocerPlugin implements Plugin {
  @Override
  public String name() {
    return "docer";
  }

  @Override
  public void init(MutableConfig config) {
    DocerConf docer = config.getOrUpdate(name(), DocerConf.class);
    docer.generateTestDoc(false);
  }
  
  @Override
  public void configure(MutableConfig config) {
    DocerConf compiler = config.getOrUpdate(name(), DocerConf.class);
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class);
    
    // inputs
    derive(compiler, DocerConf::moduleDependencyPath, convention, ConventionFacade::javaModuleDependencyPath);
    derive(compiler, DocerConf::moduleSourcePath, convention, ConventionFacade::javaModuleSourcePath);
    derive(compiler, DocerConf::moduleTestPath, convention, ConventionFacade::javaModuleTestPath);
    
    // outputs
    derive(compiler, DocerConf::moduleDocSourcePath, convention, ConventionFacade::javaModuleDocSourcePath);
    derive(compiler, DocerConf::moduleDocTestPath, convention, ConventionFacade::javaModuleDocTestPath);
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
    DocerConf docer = config.getOrThrow(name(), DocerConf.class);
    docer.moduleDependencyPath().forEach(registry::watch);
    docer.moduleSourcePath().forEach(registry::watch);
    docer.moduleTestPath().forEach(registry::watch);
  }
  
  static Optional<List<Path>> modulePathOrDependencyPath(Optional<List<Path>> modulePath, List<Path> moduleDependencyPath, List<Path> additionnalPath) {
    return modulePath
             .or(() -> Optional.of(
                    StableList.from(moduleDependencyPath).appendAll(additionnalPath)))
             .map(FileHelper.unchecked(FileHelper::pathFromFilesThatExist))
             .filter(list -> !list.isEmpty());
  }
  
  enum JavadocOption {
    RAW_ARGUMENTS(rawValues(Javadoc::rawArguments)),
    DESTINATION(action("-d", Javadoc::destination)),
    MODULE_SOURCE_PATH(action("--module-source-path", Javadoc::moduleSourcePath, File.pathSeparator)),
    ROOT_MODULES(actionMaybe("--add-modules", Javadoc::rootModules, ",")),
    UPGRADE_MODULE_PATH(actionMaybe("--upgrade-module-path", Javadoc::upgradeModulePath, File.pathSeparator)),
    MODULE_PATH(actionMaybe("--module-path", Javadoc::modulePath, File.pathSeparator)),
    ;
    
    final OptionAction<Javadoc> action;
    
    private JavadocOption(OptionAction<Javadoc> action) {
      this.action = action;
    }
  }
  
  @Override
  public int execute(Config config) throws IOException {
    Log log = Log.create(name(), config.getOrThrow("pro", ProConf.class).loglevel());
    log.debug(config, conf -> "config " + config);
    
    ToolProvider javadocTool = ToolProvider.findFirst("javadoc")
        .orElseThrow(() -> new IllegalStateException("can not find javadoc"));
    DocerConf docer = config.getOrThrow(name(), DocerConf.class);
    
    
    ModuleFinder moduleSourceFinder = ModuleHelper.sourceModuleFinders(docer.moduleSourcePath());
    int errorCode = generateDoc(log, javadocTool, docer,
        docer.moduleSourcePath(),
        moduleSourceFinder,
        docer.moduleDocSourcePath(),
        "source:");
    if (errorCode != 0) {
      return errorCode;
    }
    List<Path> moduleTestPath = FileHelper.pathFromFilesThatExist(docer.moduleTestPath());
    if (!docer.generateTestDoc() || moduleTestPath.isEmpty()) {
      return 0;
    }
    
    ModuleFinder moduleTestFinder = ModuleHelper.sourceModuleFinders(docer.moduleTestPath());
    if (moduleTestFinder.findAll().isEmpty()) {
      log.info(docer.moduleTestPath(), testPath -> "test: can not find any test modules in " + testPath.stream().map(Path::toString).collect(Collectors.joining(", ")));
      return 0;
    }
    
    return generateDoc(log, javadocTool, docer,
        docer.moduleTestPath(),
        moduleTestFinder,
        docer.moduleDocTestPath(),
        "test:");
  }

  private static int generateDoc(Log log, ToolProvider javadocTool, DocerConf docer, List<Path> moduleSourcePath, ModuleFinder moduleFinder, Path destination, String pass) throws IOException {
    Optional<List<Path>> modulePath = modulePathOrDependencyPath(docer.modulePath(),
        docer.moduleDependencyPath(), List.of());
    
    ModuleFinder dependencyFinder = ModuleFinder.compose(
        modulePath
            .stream()
            .flatMap(List::stream)
            .map(ModuleFinder::of)
            .toArray(ModuleFinder[]::new));
    List<String> rootSourceNames = moduleFinder.findAll().stream()
            .map(ref -> ref.descriptor().name())
            .collect(Collectors.toList());
    if (rootSourceNames.isEmpty()) {
      log.error(moduleSourcePath, sourcePath -> pass + " can not find any modules in " + sourcePath.stream().map(Path::toString).collect(Collectors.joining(", ")));
      return 1; //FIXME
    }
    
    ModuleFinder systemFinder = ModuleHelper.systemModulesFinder();
    
    log.debug(moduleFinder, finder -> pass + " modules " + finder.findAll().stream().map(ref -> ref.descriptor().name()).sorted().collect(Collectors.joining(", ")));
    log.debug(dependencyFinder, finder -> pass + " dependency modules " + finder.findAll().stream().map(ref -> ref.descriptor().name()).sorted().collect(Collectors.joining(", ")));
    log.debug(systemFinder, finder -> pass + " system modules " + finder.findAll().stream().map(ref -> ref.descriptor().name()).sorted().collect(Collectors.joining(", ")));
    
    boolean resolved = ModuleHelper.resolveOnlyRequires(
        ModuleFinder.compose(moduleFinder, dependencyFinder, systemFinder),
        rootSourceNames,
        (moduleName, dependencyChain) -> {
          log.error(null, __ -> pass + " can not resolve " + moduleName + " from " + dependencyChain);
        });
    if (!resolved) {
      return 1;  //FIXME
    }
    
    deleteAllFiles(destination, false);
    
    Javadoc javadoc = new Javadoc(destination, moduleSourcePath);
    docer.rawArguments().ifPresent(javadoc::rawArguments);
    modulePath.ifPresent(javadoc::modulePath);
    docer.upgradeModulePath().ifPresent(javadoc::upgradeModulePath);
    docer.rootModules().ifPresent(javadoc::rootModules);
    
    
    CmdLine cmdLine = gatherAll(JavadocOption.class, option -> option.action).apply(javadoc, new CmdLine());
    List<Path> files = docer.files().orElseGet(
        () -> walkIfNecessary(moduleSourcePath, pathFilenameEndsWith(".java")));  //FIXME, use rootNames ??
    files.forEach(cmdLine::add);
    String[] arguments = cmdLine.toArguments();
    log.verbose(files, fs -> OptionAction.toPrettyString(JavadocOption.class, option -> option.action).apply(javadoc, "javadoc") + "\n" + fs.stream().map(Path::toString).collect(Collectors.joining(" ")));
    
    return javadocTool.run(System.out, System.err, arguments);
  }
}
