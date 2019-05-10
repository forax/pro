package com.github.forax.pro.plugin.runner;

import static com.github.forax.pro.api.MutableConfig.derive;
import static com.github.forax.pro.api.helper.OptionAction.action;
import static com.github.forax.pro.api.helper.OptionAction.actionMaybe;
import static com.github.forax.pro.api.helper.OptionAction.exists;
import static com.github.forax.pro.api.helper.OptionAction.rawValues;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.io.File;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;
import com.github.forax.pro.api.helper.CmdLine;
import com.github.forax.pro.api.helper.OptionAction;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.helper.Log;
import com.github.forax.pro.helper.Platform;
import com.github.forax.pro.helper.util.StableList;

public class RunnerPlugin implements Plugin {
  @Override
  public String name() {
    return "runner";
  }

  @Override
  public void init(MutableConfig config) {
    config.getOrUpdate(name(), RunnerConf.class);
  }
  
  @Override
  public void configure(MutableConfig config) {
    var runnerConf = config.getOrUpdate(name(), RunnerConf.class);
    var convention = config.getOrThrow("convention", ConventionFacade.class); 
    
    // inputs
    derive(runnerConf, RunnerConf::modulePath, convention,
        c -> StableList.of(c.javaModuleArtifactSourcePath())
          .appendAll(c.javaModuleDependencyPath())
          .appendAll(c.javaModuleExplodedSourcePath()));
    derive(runnerConf, RunnerConf::javaCommand, convention,
        c -> c.javaHome().resolve("bin").resolve(Platform.current().javaExecutableName()));
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
    var runnerConf = config.getOrThrow(name(), RunnerConf.class);
    runnerConf.modulePath().forEach(registry::watch);
  }
  
  enum RunnerOption {
    MODULE_PATH(action("--module-path", Java::modulePath, File.pathSeparator)),
    UPGRADE_MODULE_PATH(actionMaybe("--upgrade-module-path", Java::upgradeModulePath, File.pathSeparator)),
    ROOT_MODULES(actionMaybe("--add-modules", Java::rootModules, ",")),
    ENABLE_PREVIEW(exists("--enable-preview", Java::enablePreview)),
    RAW_ARGUMENTS(rawValues(Java::rawArguments)),
    MODULE_NAME(action("--module", Java::moduleName)),
    MAIN_ARGUMENTS(rawValues(Java::mainArguments)),
    ;
    
    final OptionAction<Java> action;
    
    private RunnerOption(OptionAction<Java> action) {
      this.action = action;
    }
  }
  
  private static Optional<String> findMainModule(List<Path> modulePath, Log log) {
    for(var path: modulePath) {
      var finder = ModuleFinder.of(path);
      
      var mainClasses = finder.findAll().stream()
        .map(ModuleReference::descriptor)
        .flatMap(desc -> desc.mainClass().map(mainClass -> desc.name() + '/' + mainClass).stream())
        .collect(toUnmodifiableSet());
      
      switch(mainClasses.size()) {
      case 0:
        break;
      case 1:
        return Optional.of(mainClasses.iterator().next());
      default:
        log.error(mainClasses, _mainClasses -> "several main classes found " + String.join(", ", _mainClasses));
        return Optional.empty();
      }
    }
    
    log.error(modulePath, _modulePath -> "no main class found in " + _modulePath.stream().map(Path::toString).collect(joining(":")));
    return Optional.empty();
  }
  
  @Override
  public int execute(Config config) throws IOException {
    var log = Log.create(name(), config.getOrThrow("pro", ProConf.class).loglevel());
    var runnerConf = config.getOrThrow(name(), RunnerConf.class);
    log.debug(config, conf -> "config " + runnerConf);
    
    var moduleNameOpt = runnerConf.module().or(() -> findMainModule(runnerConf.modulePath(), log));
    if (!moduleNameOpt.isPresent()) {
      return 1;  //FIXME
    }
    
    var java = new Java(runnerConf.javaCommand(), runnerConf.modulePath(), moduleNameOpt.orElseThrow());
    runnerConf.upgradeModulePath().ifPresent(java::upgradeModulePath);
    runnerConf.rootModules().ifPresent(java::rootModules);
    runnerConf.enablePreview().ifPresent(java::enablePreview);
    runnerConf.rawArguments().ifPresent(java::rawArguments);
    runnerConf.mainArguments().ifPresent(java::mainArguments);
    
    var arguments = OptionAction.gatherAll(RunnerOption.class, option -> option.action).apply(java, new CmdLine()).toArguments();
    log.verbose(java, _java -> OptionAction.toPrettyString(RunnerOption.class, option -> option.action).apply(_java, "java"));
    
    var javaCommand = java.getJavaCommand();
    if (!Files.exists(javaCommand)) {
      log.error(javaCommand, javaPath -> "command java " + javaPath + " not found");
      return 1; //FIXME
    }
    
    Process process;
    try {
      process = new ProcessBuilder(StableList.of(javaCommand.toString()).appendAll(arguments))
          .redirectErrorStream(true)
          .start();
    } catch(IOException e) {
      throw new IOException(e.getMessage() + " while trying to execute " + javaCommand.toString() + " " + String.join(" ", arguments), e);
    }
    
    process.getInputStream().transferTo(System.out);
    
    try {
      return process.waitFor();
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }
}
