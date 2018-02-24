package com.github.forax.pro.plugin.docer;


import static com.github.forax.pro.api.MutableConfig.derive;
import static com.github.forax.pro.api.helper.OptionAction.*;
import static com.github.forax.pro.api.helper.OptionAction.actionMaybe;
import static com.github.forax.pro.api.helper.OptionAction.gatherAll;
import static com.github.forax.pro.api.helper.OptionAction.rawValues;
import static com.github.forax.pro.helper.FileHelper.pathFilenameEndsWith;
import static com.github.forax.pro.helper.FileHelper.walkIfNecessary;

import java.io.File;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    docer.quiet(false);
    docer.html5(true);
  }
  
  @Override
  public void configure(MutableConfig config) {
    DocerConf compiler = config.getOrUpdate(name(), DocerConf.class);
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class);
    
    // inputs
    derive(compiler, DocerConf::moduleDependencyPath, convention, ConventionFacade::javaModuleDependencyPath);
    derive(compiler, DocerConf::moduleSourcePath, convention, ConventionFacade::javaModuleSourcePath);
    derive(compiler, DocerConf::moduleMergedTestPath, convention, ConventionFacade::javaModuleMergedTestPath);
    
    // outputs
    derive(compiler, DocerConf::moduleDocSourcePath, convention, ConventionFacade::javaModuleDocSourcePath);
    derive(compiler, DocerConf::moduleDocTestPath, convention, ConventionFacade::javaModuleDocTestPath);
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
    DocerConf docer = config.getOrThrow(name(), DocerConf.class);
    docer.moduleDependencyPath().forEach(registry::watch);
    docer.moduleSourcePath().forEach(registry::watch);
    docer.moduleMergedTestPath().forEach(registry::watch);
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
    QUIET(exists("-quiet", Javadoc::quiet)),
    HTML5(exists("-html5", Javadoc::html5)),
    LINK(actionMaybe("-link", Javadoc::link))
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
    
    List<Path> moduleSourcePath = FileHelper.pathFromFilesThatExist(docer.moduleSourcePath());
    ModuleFinder moduleSourceFinder = ModuleHelper.sourceModuleFinders(moduleSourcePath);
    int errorCode = generateAll(moduleSourceFinder, docer.moduleDocSourcePath(),
        (input, output) -> generateDoc(log, javadocTool, docer, input, output));
    if (errorCode != 0) {
      return errorCode;
    }
    List<Path> moduleTestPath = FileHelper.pathFromFilesThatExist(docer.moduleMergedTestPath());
    if (!docer.generateTestDoc() || moduleTestPath.isEmpty()) {
      return 0;
    }
    
    ModuleFinder moduleTestFinder = ModuleHelper.sourceModuleFinders(moduleTestPath);
    return generateAll(moduleTestFinder, docer.moduleDocTestPath(),
        (input, output) -> generateDoc(log, javadocTool, docer, input, output));
  }

  interface Action {
    int apply(Path input, Path output);
  }
  
  private static int generateAll(ModuleFinder finder, Path output, Action action) throws IOException {
    FileHelper.deleteAllFiles(output, false);
    Files.createDirectories(output);
    
    for(ModuleReference module: finder.findAll()) {
      Optional<URI> location = module.location();
      if (!location.isPresent()) {
        continue;
      }
      int exitCode = action.apply(Paths.get(location.get()), output);
      if (exitCode != 0) {
        return exitCode;
      }
    }
    return 0;
  }
  
  private static int generateDoc(Log log, ToolProvider javadocTool, DocerConf docer, Path input, Path output) {
    Javadoc javadoc = new Javadoc(output.resolve(input.getFileName().toString()), docer.moduleSourcePath());
    docer.rawArguments().ifPresent(javadoc::rawArguments);
    javadoc.modulePath(docer.moduleDependencyPath());
    docer.upgradeModulePath().ifPresent(javadoc::upgradeModulePath);
    docer.rootModules().ifPresent(javadoc::rootModules);
    javadoc.quiet(docer.quiet());
    javadoc.html5(docer.html5());
    docer.link().ifPresent(javadoc::link);
    
    CmdLine cmdLine = gatherAll(JavadocOption.class, option -> option.action).apply(javadoc, new CmdLine());
    List<Path> files = docer.files().orElseGet(
        () -> walkIfNecessary(List.of(input), pathFilenameEndsWith(".java")));  //FIXME, use rootNames ??
    files.forEach(cmdLine::add);
    String[] arguments = cmdLine.toArguments();
    log.verbose(files, fs -> OptionAction.toPrettyString(JavadocOption.class, option -> option.action).apply(javadoc, "javadoc") + "\n" + fs.stream().map(Path::toString).collect(Collectors.joining(" ")));
    
    return javadocTool.run(System.out, System.err, arguments);
  }
}
