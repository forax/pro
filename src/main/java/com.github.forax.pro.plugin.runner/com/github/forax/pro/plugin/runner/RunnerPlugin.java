package com.github.forax.pro.plugin.runner;

import static com.github.forax.pro.api.helper.OptionAction.action;
import static com.github.forax.pro.api.helper.OptionAction.actionMaybe;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;
import com.github.forax.pro.api.helper.CmdLine;
import com.github.forax.pro.api.helper.OptionAction;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.helper.Log;
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
    RunnerConf runner = config.getOrUpdate(name(), RunnerConf.class);
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class); 
    
    // inputs
    runner.modulePath(StableList.of(convention.javaModuleArtifactSourcePath())
        .appendAll(convention.javaModuleDependencyPath())
        .appendAll(convention.javaModuleExplodedSourcePath()));
    runner.javaCommand(convention.javaHome().resolve("bin/java"));
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
    RunnerConf runner = config.getOrThrow(name(), RunnerConf.class);
    runner.modulePath().forEach(registry::watch);
  }
  
  enum RunnerOption {
    MODULE_PATH(action("--module-path", Java::modulePath, ":")),
    ROOT_MODULES(actionMaybe("--add-modules", Java::rootModules, ",")),
    MODULE(action("--module", Java::module))
    ;
    
    final OptionAction<Java> action;
    
    private RunnerOption(OptionAction<Java> action) {
      this.action = action;
    }
  }
  
  private static Optional<String> findMainModule(List<Path> modulePath, Log log) {
    for(Path path: modulePath) {
      ModuleFinder finder = ModuleFinder.of(path);
      
      Set<String> set = finder.findAll().stream()
        .map(ModuleReference::descriptor)
        .flatMap(desc -> desc.mainClass().map(mainClass -> desc.name() + '/' + mainClass).stream())
        .collect(Collectors.toSet());
      
      switch(set.size()) {
      case 0:
        break;
      case 1:
        return Optional.of(set.iterator().next());
      default:
        log.error(set, mainClasses -> "several main classes found " + String.join(", ", mainClasses));
        return Optional.empty();
      }
    }
    
    log.error(null, __ -> "no main class found");
    return Optional.empty();
  }
  
  @Override
  public int execute(Config config) throws IOException {
    Log log = Log.create(name(), config.getOrThrow("pro", ProConf.class).loglevel());
    log.debug(config, conf -> "config " + config);
    
    RunnerConf runner = config.getOrThrow(name(), RunnerConf.class);
    
    Optional<String> moduleName = runner.module()
        .or(() -> findMainModule(runner.modulePath(), log));
    if (!moduleName.isPresent()) {
      return 1;  //FIXME
    }
    
    Java java = new Java(runner.javaCommand(), runner.modulePath(), moduleName.get());
    runner.rootModules().ifPresent(java::rootModules);
    
    String[] arguments = OptionAction.gatherAll(RunnerOption.class, option -> option.action).apply(java, new CmdLine()).toArguments();
    log.verbose(java, _java -> OptionAction.toPrettyString(RunnerOption.class, option -> option.action).apply(_java, "java"));
    
    Path javaCommand = java.getJavaCommand();
    if (!Files.exists(javaCommand)) {
      log.error(javaCommand, javaPath -> "command java " + javaPath + " not found");
      return 1; //FIXME
    }
    
    Process process = new ProcessBuilder(StableList.of(javaCommand.toString()).appendAll(arguments))
      .inheritIO()
      .start();
    
    try {
      return process.waitFor();
    } catch (InterruptedException e) {
      return 1; // FIXME
    }
  }
}
