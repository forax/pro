package com.github.forax.pro.plugin.linker;

import static com.github.forax.pro.api.helper.OptionAction.action;
import static com.github.forax.pro.api.helper.OptionAction.exists;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.helper.CmdLine;
import com.github.forax.pro.api.helper.OptionAction;
import com.github.forax.pro.helper.FileHelper;
import com.github.forax.pro.helper.ModuleHelper;

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
    //System.out.println("execute " + config);
    
    ToolProvider jlinkTool = ToolProvider.findFirst("jlink")
        .orElseThrow(() -> new IllegalStateException("can not find jlink"));
    Linker linker = config.getOrThrow(name(), Linker.class);
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class);
    
    Path javaSystemModule = convention.javaHome().resolve("jmods");
    if (!(Files.exists(javaSystemModule))) {
      throw new IOException("unable to find Java system module at " + javaSystemModule);
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
          .orElseGet(() -> Stream.of(
                Stream.of(systemModulePath(convention)),
                FileHelper.pathFromFilesThatExist(linker.moduleDependencyPath()).stream(),
                Stream.of(convention.javaModuleArtifactSourcePath()))
            .flatMap(x -> x)
            .collect(Collectors.toList()));
    
    //System.out.println("rootModules " + rootModules);
    Jlink jlink = new Jlink(linker, rootModules, modulePath);
    
    Path destination = linker.destination();
    FileHelper.deleteAllFiles(destination);
    
    String[] arguments = OptionAction.gatherAll(JlinkOption.class, option -> option.action).apply(jlink, new CmdLine()).toArguments();
    
    //System.out.println("jlink " + String.join(" ", arguments));
    return jlinkTool.run(System.out, System.err, arguments);
  }
}
