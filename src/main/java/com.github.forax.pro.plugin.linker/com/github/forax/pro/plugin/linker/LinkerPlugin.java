package com.github.forax.pro.plugin.linker;

import static com.github.forax.pro.api.MutableConfig.derive;
import static com.github.forax.pro.api.helper.OptionAction.action;
import static com.github.forax.pro.api.helper.OptionAction.actionLoop;
import static com.github.forax.pro.api.helper.OptionAction.exists;
import static com.github.forax.pro.api.helper.OptionAction.rawValues;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.io.File;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

public class LinkerPlugin implements Plugin {
  @Override
  public String name() {
    return "linker";
  }

  @Override
  public void init(MutableConfig config) {
    var linkerConf = config.getOrUpdate(name(), LinkerConf.class);
    linkerConf.compressLevel(0);
    linkerConf.stripDebug(false);
    linkerConf.stripNativeCommands(false);
    linkerConf.includeSystemJMODs(false);
    linkerConf.ignoreSigningInformation(false);
  }
  
  @Override
  public void configure(MutableConfig config) {
    var linkerConf = config.getOrUpdate(name(), LinkerConf.class);
    var convention = config.getOrThrow("convention", ConventionFacade.class);
    
    // inputs
    derive(linkerConf, LinkerConf::systemModulePath,
        convention, c -> c.javaHome().resolve("jmods"));
    derive(linkerConf, LinkerConf::moduleArtifactSourcePath, convention, ConventionFacade::javaModuleArtifactSourcePath);
    derive(linkerConf, LinkerConf::moduleDependencyPath, convention, ConventionFacade::javaModuleDependencyPath);
    
    // outputs
    derive(linkerConf, LinkerConf::destination, convention, ConventionFacade::javaLinkerImagePath);
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
    var linkerConf = config.getOrThrow(name(), LinkerConf.class);
    
    registry.watch(linkerConf.systemModulePath());
    linkerConf.moduleDependencyPath().forEach(registry::watch);
    registry.watch(linkerConf.moduleArtifactSourcePath());
  }
  
  enum JlinkOption {
    MODULE_PATH(action("--module-path", Jlink::modulePath, File.pathSeparator)),
    ROOT_MODULES(action("--add-modules", Jlink::rootModules, ",")),
    LAUNCHER(actionLoop("--launcher", Jlink::launchers)),
    COMPRESS(action("--compress", Jlink::compressLevel)),
    STRIP_DEBUG(exists("--strip-debug", Jlink::stripDebug)),
    STRIP_NATIVE_COMMANDS(exists("--strip-native-commands", Jlink::stripNativeCommands)),
    IGNORE_SIGNING_INFO(exists("--ignore-signing-information", Jlink::ignoreSigningInformation)),
    RAW_ARGUMENTS(rawValues(Jlink::rawArguments)),
    OUPUT(action("--output", Jlink::destination))
    ;
    
    final OptionAction<Jlink> action;
    
    private JlinkOption(OptionAction<Jlink> action) {
      this.action = action;
    }
  }
  
  private static List<String> findLaunchersFromMainClasses(Set<String> rootModules, ModuleFinder moduleFinder) {
    return rootModules.stream()
             .flatMap(root -> moduleFinder.find(root).stream())
             .map(ModuleReference::descriptor)
             .flatMap(desc -> desc.mainClass().map(main -> desc.name() + '=' + desc.name() + '/' + main).stream())
             .collect(toUnmodifiableList());
  }
  
  @Override
  public int execute(Config config) throws IOException {
    var log = Log.create(name(), config.getOrThrow("pro", ProConf.class).loglevel());
    log.debug(config, conf -> "config " + config);
    
    var jlinkTool = ToolProvider.findFirst("jlink").orElseThrow(() -> new IllegalStateException("can not find jlink"));
    var linkerConf = config.getOrThrow(name(), LinkerConf.class);
    
    var systemModulePath = linkerConf.systemModulePath();
    if (!(Files.exists(systemModulePath))) {
      throw new IOException("unable to find system modules at " + systemModulePath);
    }
    
    var moduleFinder = ModuleFinder.of(linkerConf.moduleArtifactSourcePath());
    var rootModules = linkerConf.rootModules().map(HashSet::new).orElseGet(() -> {
      return moduleFinder.findAll().stream()
          .map(reference -> reference.descriptor().name())
          .collect(Collectors.toCollection(HashSet::new));
    });
    linkerConf.serviceNames().ifPresent(serviceNames -> {
      ModuleFinder rootFinder = ModuleFinder.compose(moduleFinder, ModuleHelper.systemModulesFinder());
      ModuleHelper.findAllModulesWhichProvideAService(serviceNames, rootFinder)
        .map(ref -> ref.descriptor().name())
        .forEach(rootModules::add);
    });
    
    // find launchers
    var launchers = linkerConf.launchers().orElseGet(() -> findLaunchersFromMainClasses(rootModules, moduleFinder));
    if (launchers.isEmpty()) {
      log.error(null, __ -> "no launcher found and no main classes defined in the root modules");
      return 1; //FIXME
    }
    
    var modulePath =
        linkerConf.modulePath()
          .orElseGet(() -> StableList.of(linkerConf.moduleArtifactSourcePath())
                .appendAll(FileHelper.pathFromFilesThatExist(linkerConf.moduleDependencyPath()))
                .append(systemModulePath));
    
    log.debug(rootModules, roots -> "rootModules " + roots);
    log.debug(launchers, launcherMains -> "launchers " + launcherMains);
    var jlink = new Jlink(linkerConf, rootModules, launchers, modulePath);
    
    var destination = linkerConf.destination();
    FileHelper.deleteAllFiles(destination, true);
    
    var arguments = OptionAction.gatherAll(JlinkOption.class, option -> option.action).apply(jlink, new CmdLine()).toArguments();
    log.verbose(null, __ -> OptionAction.toPrettyString(JlinkOption.class, option -> option.action).apply(jlink, "jlink"));
    
    var errorCode = jlinkTool.run(System.out, System.err, arguments);
    if (errorCode != 0) {
      return errorCode; 
    }
    
    if (linkerConf.includeSystemJMODs()) {
      var jmods = destination.resolve("jmods");
      Files.createDirectories(jmods);
      try(var directoryStream = Files.newDirectoryStream(systemModulePath)) {
        for(var path: directoryStream) {
          Files.copy(path, jmods.resolve(path.getFileName()));
        }
      }
    }
    return 0;
  }
}
