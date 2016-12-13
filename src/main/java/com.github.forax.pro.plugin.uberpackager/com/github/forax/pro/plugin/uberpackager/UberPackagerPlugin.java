package com.github.forax.pro.plugin.uberpackager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.reflect.Module;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.helper.FileHelper;
import com.github.forax.pro.ubermain.Main;

public class UberPackagerPlugin implements Plugin {
  @Override
  public String name() {
    return "uberpackager";
  }

  @Override
  public void init(MutableConfig config) {
    config.getOrUpdate(name(), UberPackager.class);
  }
  
  @Override
  public void configure(MutableConfig config) {
    UberPackager packager = config.getOrUpdate(name(), UberPackager.class);
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class); 
    packager.moduleUberPath(convention.javaModuleUberPath());
    packager.moduleUberExplodedPath(convention.javaModuleUberExplodedPath());
  }
  
  @Override
  public int execute(Config config) throws IOException {
    //System.out.println("execute " + config);
    
    ToolProvider jarTool = ToolProvider.findFirst("jar")
        .orElseThrow(() -> new IllegalStateException("can not find jar"));
    UberPackager packager = config.getOrThrow(name(), UberPackager.class);
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class); 
    
    Path uberExplodedPath = packager.moduleUberExplodedPath();
    FileHelper.deleteAllFiles(uberExplodedPath);
    Files.createDirectories(uberExplodedPath);
    
    Class<Main> mainClass = com.github.forax.pro.ubermain.Main.class;
    Module uberjarModule = mainClass.getModule();
    ModuleReference uberJarRef = uberjarModule.getLayer().configuration().findModule(uberjarModule.getName()).get().reference();
    Path uberJarModulePath;
    try(ModuleReader moduleReader = uberJarRef.open()) {
      for(String filename: List.of(mainClass.getName().replace('.', '/') + ".class", mainClass.getName().replace('.', '/') + "$1.class")) {
        Path path = uberExplodedPath.resolve(filename);
        Files.createDirectories(path.getParent());
        Files.copy(moduleReader.open(filename).get(), path);
      }
      
      Path uberJarPath = Paths.get(uberJarRef.location().get());
      uberJarModulePath = uberJarPath.getFileName();
      Files.copy(uberJarPath, uberExplodedPath.resolve(uberJarModulePath));
    }
    
    List<Path> modulePaths = Stream.of(
        Stream.of(convention.javaModuleArtifactSourcePath()),
        convention.javaModuleDependencyPath().stream())
        .flatMap(x -> x)
        .collect(Collectors.toList());
    
    try(BufferedWriter writer = Files.newBufferedWriter(uberExplodedPath.resolve("modules.txt"))) {
      writer.write("com.github.forax.pro.main/com.github.forax.pro.main.Main");
      writer.newLine();
      writer.write(uberJarModulePath.toString());
      writer.newLine();
      for(Path modulePath: modulePaths) {
        try(Stream<Path> modularJars = Files.list(modulePath)) {
          for(Path modularJar: (Iterable<Path>)modularJars::iterator){
            //System.out.println("modularJar " + modularJar);
            writer.write(modularJar.getFileName().toString());
            writer.newLine();
          }
        }
      }
    }
    
    Path uberjar = packager.moduleUberPath().resolve("uber.jar");
    
    jarTool.run(System.out, System.err,
        "--create",
        "--file", uberjar.toString(),
        "--main-class", mainClass.getName(),
        "-C", uberExplodedPath.toString(),
        "."
        );
    
    for(Path modulePath: modulePaths) {
      jarTool.run(System.out, System.err,
          "--update",
          "--file", uberjar.toString(),
          "-C", modulePath.toString(),
          "."
          );
    }
    
    return 0;
  }
}
