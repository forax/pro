package com.github.forax.pro.plugin.linker;

import static com.github.forax.pro.api.helper.OptionAction.action;
import static com.github.forax.pro.api.helper.OptionAction.exists;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.helper.CmdLine;
import com.github.forax.pro.api.helper.OptionAction;
import com.github.forax.pro.helper.FileHelper;
import com.github.forax.pro.helper.ModuleHelper;
import com.github.forax.pro.helper.StableList;

public class LinkerPlugin implements Plugin {
  @Override
  public String name() {
    return "linker";
  }

  @Override
  public void init(MutableConfig config) {
    Linker linker = config.getOrUpdate(name(), Linker.class);
    linker.compressLevel(0);
    linker.stripDebug(false);
    linker.stripNativeCommands(false);
    linker.includeSystemJMODs(false);
  }
  
  @Override
  public void configure(MutableConfig config) {
    Linker linker = config.getOrUpdate(name(), Linker.class);
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class);
    
    linker.destination(convention.javaLinkerImagePath());
    linker.moduleDependencyPath(convention.javaModuleDependencyPath());
  }
  
  enum JlinkOption {
    MODULE_PATH(action("--module-path", Jlink::modulePath, ":")),
    ADD_MODULES(action("--add-modules", Jlink::rootModules, ",")),
    COMPRESS(action("--compress", Jlink::compressLevel)),
    STRIP_DEBUG(exists("--strip-debug", Jlink::stripDebug)),
    STRIP_NATIVE_COMMANDS(exists("--strip-native-commands", Jlink::stripNativeCommands)),
    OUPUT(action("--output", Jlink::destination))
    ;
    
    final OptionAction<Jlink> action;
    
    private JlinkOption(OptionAction<Jlink> action) {
      this.action = action;
    }
  }
  
  static Path systemModulePath(ConventionFacade convention) {
    Path javaSystemModulePath = convention.javaHome().resolve("jmods");
    return Optional.of(javaSystemModulePath)
             .filter(Files::exists)
             .orElseThrow(() -> new IllegalStateException("unable to find Java system module at " + javaSystemModulePath));
  }
  
  @Override
  public int execute(Config config) throws IOException {
    //System.out.println("execute linker " + config);
    
    ToolProvider jlinkTool = ToolProvider.findFirst("jlink")
        .orElseThrow(() -> new IllegalStateException("can not find jlink"));
    Linker linker = config.getOrThrow(name(), Linker.class);
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class);
    
    Path javaSystemModulePath = systemModulePath(convention);
    if (!(Files.exists(javaSystemModulePath))) {
      throw new IOException("unable to find Java system module at " + javaSystemModulePath);
    }
    
    ModuleFinder moduleFinder = ModuleFinder.of(convention.javaModuleArtifactSourcePath());
    List<String> rootModules = linker.rootModules().orElseGet(() -> {
      return moduleFinder.findAll().stream()
          .map(reference -> reference.descriptor().name())
          .collect(Collectors.toList());
    });
    linker.serviceNames().ifPresent(serviceNames -> {
      ModuleFinder rootFinder = ModuleFinder.compose(moduleFinder, ModuleFinder.ofSystem());
      rootModules.addAll(ModuleHelper.findAllModulesWhichProvideAService(serviceNames, rootFinder));
    });
    
    List<Path> modulePath =
        linker.modulePath()
          .orElseGet(() -> new StableList<Path>()
                .append(javaSystemModulePath)
                .appendAll(FileHelper.pathFromFilesThatExist(linker.moduleDependencyPath()))
                .append(convention.javaModuleArtifactSourcePath()));
    
    //System.out.println("rootModules " + rootModules);
    Jlink jlink = new Jlink(linker, rootModules, modulePath);
    
    Path destination = linker.destination();
    FileHelper.deleteAllFiles(destination);
    
    String[] arguments = OptionAction.gatherAll(JlinkOption.class, option -> option.action).apply(jlink, new CmdLine()).toArguments();
    
    //System.out.println("jlink " + String.join(" ", arguments));
    int errorCode = jlinkTool.run(System.out, System.err, arguments);
    if (errorCode != 0) {
      return errorCode; 
    }
    
    if (linker.includeSystemJMODs()) {
      Path jmods = destination.resolve("jmods");
      Files.createDirectories(jmods);
      try(DirectoryStream<Path> stream = Files.newDirectoryStream(javaSystemModulePath)) {
        for(Path path: stream) {
          Files.copy(path, jmods.resolve(path.getFileName()));
        }
      }
    }
    return 0;
  }
}
