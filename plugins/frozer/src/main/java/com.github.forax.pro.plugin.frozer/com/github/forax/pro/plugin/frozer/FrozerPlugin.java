package com.github.forax.pro.plugin.frozer;

import static com.github.forax.pro.api.MutableConfig.derive;
import static java.util.function.Predicate.not;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.helper.Log;
import com.github.forax.pro.helper.ModuleHelper;
import com.github.forax.pro.helper.ModuleHelper.ResolverListener;
import com.github.forax.pro.helper.util.StableList;

public class FrozerPlugin implements Plugin {
  @Override
  public String name() {
    return "frozer";
  }

  @Override
  public void init(MutableConfig config) {
    config.getOrUpdate(name(), FrozerConf.class);
  }
  
  @Override
  public void configure(MutableConfig config) {
    var frozerConf = config.getOrUpdate(name(), FrozerConf.class);
    var convention = config.getOrThrow("convention", ConventionFacade.class); 
    
    // inputs
    derive(frozerConf, FrozerConf::modulePath, convention,
        c -> StableList.of(c.javaModuleArtifactSourcePath())
          .appendAll(c.javaModuleDependencyPath())
          .appendAll(c.javaModuleExplodedSourcePath()));
    
    // output
    derive(frozerConf, FrozerConf::moduleFrozenArtifactSourcePath, convention, ConventionFacade::javaModuleFrozenArtifactSourcePath);
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
    var frozerConf = config.getOrThrow(name(), FrozerConf.class);
    frozerConf.modulePath().forEach(registry::watch);
  }
  
  @Override
  public int execute(Config config) throws IOException {
    var log = Log.create(name(), config.getOrThrow("pro", ProConf.class).loglevel());
    var frozerConf = config.getOrThrow(name(), FrozerConf.class);
    log.debug(config, conf -> "config " + frozerConf);
    
    String rootModuleName = frozerConf.rootModule().orElseThrow(() -> new IllegalArgumentException("no root module specified"));
    
    ModuleFinder finder = ModuleFinder.of(frozerConf.modulePath().toArray(new Path[0]));
    
    var rootModule = finder.find(rootModuleName).orElseThrow(() -> new IllegalStateException("no root module " + rootModuleName + " not found"));
    
    var dependencies = new LinkedHashSet<String>();
    var requires = new LinkedHashSet<String>();
    ModuleHelper.resolveOnlyRequires(finder, List.of(rootModuleName), new ResolverListener() {
      @Override
      public void module(String moduleName) {
        dependencies.add(moduleName);
      }
      
      @Override
      public void dependencyNotFound(String moduleName, String dependencyChain) {
        requires.add(moduleName);
        //System.out.println("not found " + moduleName + " deps " + dependencyChain);
      }
    });
    
    // remove unresolved dependencies
    dependencies.removeAll(requires);
    
    //System.out.println("dependencies " + dependencies);
    //System.out.println("notFounds " + requires);
    
    // packages that need to be renamed
    var packages = new HashSet<String>();
    
    // create the new module descriptor
    var builder = ModuleDescriptor.newOpenModule(rootModuleName);
    var rootModuleDescriptor = rootModule.descriptor();
    
    rootModuleDescriptor.version().ifPresent(builder::version);
    requires.forEach(builder::requires);
    rootModuleDescriptor.exports().forEach(builder::exports);
    rootModuleDescriptor.opens().forEach(builder::opens);
    builder.packages(rootModuleDescriptor.packages());
    rootModuleDescriptor.provides().forEach(builder::provides);
    
    dependencies.stream()
      .filter(not(rootModuleName::equals))
      .flatMap(name -> finder.find(name).stream())
      .map(ModuleReference::descriptor)
      .forEach(descriptor -> {
        //descriptor.exports().forEach(builder::exports);
        //descriptor.opens().forEach(builder::opens);
        
        builder.packages(descriptor.packages());
        packages.addAll(descriptor.packages());
        descriptor.provides().forEach(builder::provides);
      });
    var moduleDescriptor = builder.build();
    
    rewrite(rootModule, dependencies, packages, finder, moduleDescriptor, frozerConf.moduleFrozenArtifactSourcePath());
    return 0;
  }

  private static void rewrite(ModuleReference rootModule, LinkedHashSet<String> dependencies, HashSet<String> packages, ModuleFinder finder, ModuleDescriptor moduleDescriptor, Path destination) throws IOException {
    // create a map old package name -> new package name
    var rootModuleName = rootModule.descriptor().name();
    var internalRootModuleName = rootModuleName.replace('.', '/');
    var internalPackageNameMap = packages.stream()
        .map(name -> name.replace('.', '/'))
        .collect(Collectors.toMap(name -> name, name -> internalRootModuleName + '/' + name));
    //System.out.println(internalPackageNameMap);
    
    Path path = destination.resolve(rootModuleName + ".jar");
    Files.createDirectories(path.getParent());
    try(var output = Files.newOutputStream(path);
        var outputStream = new JarOutputStream(output)) {
      
      // insert new module-info and interpolate it
      outputStream.putNextEntry(new JarEntry("module-info.class"));
      outputStream.write(rewriteBytecode(internalPackageNameMap, ModuleHelper.moduleDescriptorToBinary(moduleDescriptor)));
      
      // rewrite all the classes of the dependencies
      var entryNames = new HashSet<>();
      for(var dependency: dependencies) {
        var reference = finder.find(dependency).orElseThrow();
        try(var reader = reference.open()) {
          for(String filename: (Iterable<String>)reader.list()::iterator) {
            //System.out.println("filename " + filename);
            
            if (filename.equals("module-info.class") ||  // skip module-info
                filename.endsWith("/") ||                // skip all directories
                filename.startsWith("META-INF")) {       // skip META-INF
              continue;
            }
            
            if (!entryNames.add(filename)) {
              System.out.println("duplicate entry " + filename + " skip it !");
              continue;
            }
            
            try(var inputStream = reader.open(filename).orElseThrow(() -> new IOException("can not read " + filename))) {
              if (!filename.endsWith(".class")) {  // only rewrite .class
                                                   // otherwise copy the resources
                outputStream.putNextEntry(new JarEntry(filename));
                inputStream.transferTo(outputStream);
                continue;
              }
            
              var newFilename = interpolate(internalPackageNameMap, filename);
              outputStream.putNextEntry(new JarEntry(newFilename));
              outputStream.write(rewriteBytecode(internalPackageNameMap, inputStream.readAllBytes()));
            }
          }
        }
      }
    }
  }
  
  private static String interpolate(Map<String, String> internalPackageNameMap, String internalName) {
    var index = internalName.lastIndexOf('/');
    if (index == -1) {
      return internalName;
    }
    var internalPackageName = internalName.substring(0, index);
    return Optional.ofNullable(internalPackageNameMap.get(internalPackageName))
        .map(packageName -> packageName + '/' + internalName.substring(index + 1))
        .orElse(internalName);
  }
  
  private static byte[] rewriteBytecode(Map<String, String> internalPackageNameMap, byte[] bytecode) {
    ClassReader classReader = new ClassReader(bytecode);
    ClassWriter classWriter = new ClassWriter(0);
    classReader.accept(new ClassRemapper(classWriter, new Remapper() {
      @Override
      public String map(String internalName) {
        return interpolate(internalPackageNameMap, internalName);
      }
      @Override
      public String mapPackageName(String internalPackageName) {
        return Optional.ofNullable(internalPackageNameMap.get(internalPackageName))
            .orElse(internalPackageName);
      }
    }), 0);
    return classWriter.toByteArray();
  }
}
