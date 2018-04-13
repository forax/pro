package com.github.forax.pro.plugin.uberpackager;

import static com.github.forax.pro.api.MutableConfig.derive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.spi.ToolProvider;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;
import com.github.forax.pro.api.helper.CmdLine;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.helper.FileHelper;
import com.github.forax.pro.helper.Log;
import com.github.forax.pro.helper.util.StableList;

public class UberPackagerPlugin implements Plugin {
  @Override
  public String name() {
    return "uberpackager";
  }

  @Override
  public void init(MutableConfig config) {
    config.getOrUpdate(name(), UberPackagerConf.class);
  }
  
  @Override
  public void configure(MutableConfig config) {
    var uberPackagerConf = config.getOrUpdate(name(), UberPackagerConf.class);
    var convention = config.getOrThrow("convention", ConventionFacade.class); 
    
    // inputs
    derive(uberPackagerConf, UberPackagerConf::moduleArtifactSourcePath, convention, ConventionFacade::javaModuleArtifactSourcePath);
    derive(uberPackagerConf, UberPackagerConf::moduleDependencyPath, convention, ConventionFacade::javaModuleDependencyPath);
    
    // outputs
    derive(uberPackagerConf, UberPackagerConf::moduleUberPath, convention, ConventionFacade::javaModuleUberPath);
    derive(uberPackagerConf, UberPackagerConf::moduleUberExplodedPath, convention, ConventionFacade::javaModuleUberExplodedPath);
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
    var uberPackagerConf = config.getOrThrow(name(), UberPackagerConf.class);
    uberPackagerConf.moduleArtifactSourcePath().forEach(registry::watch);
    uberPackagerConf.moduleDependencyPath().forEach(registry::watch);
  }
  
  @Override
  public int execute(Config config) throws IOException {
    var log = Log.create(name(), config.getOrThrow("pro", ProConf.class).loglevel());
    log.debug(config, conf -> "config " + config);
    
    log.info(null, __ -> "WARNING: this feature is highly experimental !");
    
    var jarTool = ToolProvider.findFirst("jar").orElseThrow(() -> new IllegalStateException("can not find the command jar"));
    var uberPackagerConf = config.getOrThrow(name(), UberPackagerConf.class);
    
    var uberExplodedPath = uberPackagerConf.moduleUberExplodedPath();
    FileHelper.deleteAllFiles(uberExplodedPath, false);
    Files.createDirectories(uberExplodedPath);
    
    var mainClass = com.github.forax.pro.ubermain.Main.class;
    var uberjarModule = mainClass.getModule();
    var uberJarRef = uberjarModule.getLayer().configuration().findModule(uberjarModule.getName()).get().reference();
    try(var moduleReader = uberJarRef.open()) {
      var mainClassName = mainClass.getName().replace('.', '/');
      for(var filename: List.of(mainClassName + ".class", mainClassName + "$1.class", mainClassName + "$1$1.class")) {
        var path = uberExplodedPath.resolve(filename);
        Files.createDirectories(path.getParent());
        Files.copy(moduleReader.open(filename).get(), path);
      }
    }
    
    //FIXME add the class of module uberbooter
    
    var modulePaths = StableList.of(uberPackagerConf.moduleArtifactSourcePath())
        .appendAll(uberPackagerConf.moduleDependencyPath());
    
    try(var writer = Files.newBufferedWriter(uberExplodedPath.resolve("modules.txt"))) {
      writer.write("com.github.forax.pro.main/com.github.forax.pro.main.Main");
      writer.newLine();
      //writer.write(uberbooterModule);
      //writer.newLine();
      for(var modulePath: modulePaths) {
        try(var modularJars = Files.list(modulePath)) {
          for(var modularJar: (Iterable<Path>)modularJars::iterator){
            //System.out.println("modularJar " + modularJar);
            writer.write(modularJar.getFileName().toString());
            writer.newLine();
          }
        }
      }
    }
    
    var uberjar = uberPackagerConf.moduleUberPath().resolve("uber.jar");
    var cmdLine = new CmdLine().addAll(
        "--create",
        "--file", uberjar.toString(),
        "--main-class", mainClass.getName(),
        "-C", uberExplodedPath.toString(),
        "."
        );
    var arguments = cmdLine.toArguments();
    log.verbose(arguments, args -> "jar " + String.join(" ", args));
    jarTool.run(System.out, System.err, arguments);
    
    for(var modulePath: modulePaths) {
      cmdLine = new CmdLine().addAll(
          "--update",
          "--file", uberjar.toString(),
          "-C", modulePath.toString(),
          "."
          );
      arguments = cmdLine.toArguments();
      log.verbose(arguments, args -> "jar " + String.join(" ", args));
      jarTool.run(System.out, System.err, arguments);
    }
    
    return 0;
  }
}
