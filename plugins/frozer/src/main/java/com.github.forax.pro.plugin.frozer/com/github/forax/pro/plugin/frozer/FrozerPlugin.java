package com.github.forax.pro.plugin.frozer;

import static com.github.forax.pro.api.MutableConfig.derive;
import static java.lang.Character.isJavaIdentifierStart;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    var packages = dependencies.stream()
      .filter(not(rootModuleName::equals))
      .flatMap(name -> finder.find(name).stream())
      .map(ModuleReference::descriptor)
      .flatMap(descriptor -> Stream.<String>concat(descriptor.exports().stream().map(Exports::source), descriptor.packages().stream()))
      .collect(Collectors.toSet());
    
    rewrite(rootModule, dependencies, requires, packages, finder, frozerConf.moduleFrozenArtifactSourcePath());
    return 0;
  }

  private static void rewrite(ModuleReference rootModule, LinkedHashSet<String> dependencies, LinkedHashSet<String> requires, Set<String> packages, ModuleFinder finder, Path destination) throws IOException {
    // create a map old package name -> new package name
    var rootModuleName = rootModule.descriptor().name();
    var internalRootModuleName = rootModuleName.replace('.', '/');
    var internalPackageNameMap = packages.stream()
        .map(name -> name.replace('.', '/'))
        .collect(Collectors.toMap(name -> name, name -> internalRootModuleName + '/' + name));
    //System.out.println(internalPackageNameMap);
    
    var newPackages = new HashSet<String>();
    
    Path path = destination.resolve(rootModuleName + ".jar");
    Files.createDirectories(path.getParent());
    try(var output = Files.newOutputStream(path);
        var outputStream = new JarOutputStream(output)) {
      
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
                
                getPackage(filename).ifPresent(newPackages::add);
                
                outputStream.putNextEntry(new JarEntry(filename));
                inputStream.transferTo(outputStream);
                continue;
              }
            
              var newFilename = interpolateInternalName(internalPackageNameMap, filename);
              getPackage(newFilename).ifPresent(newPackages::add);
              outputStream.putNextEntry(new JarEntry(newFilename));
              outputStream.write(rewriteBytecode(internalPackageNameMap, inputStream.readAllBytes()));
            }
          }
        }
      }
      
      // then insert the new module-info and interpolate it
      outputStream.putNextEntry(new JarEntry("module-info.class"));
      
      var builder = ModuleDescriptor.newOpenModule(rootModuleName);
      var rootModuleDescriptor = rootModule.descriptor();
      
      rootModuleDescriptor.version().ifPresent(builder::version);
      requires.forEach(builder::requires);
      rootModuleDescriptor.exports().forEach(builder::exports);
      rootModuleDescriptor.opens().forEach(builder::opens);
      builder.packages(rootModuleDescriptor.packages());
      
      dependencies.stream()
        .flatMap(name -> finder.find(name).stream())
        .map(ModuleReference::descriptor)
        .forEach(descriptor -> {
          descriptor.provides().forEach(provides -> {
            builder.provides(
                interpolateClassName(internalPackageNameMap, provides.service()),
                provides.providers().stream().map(provider -> interpolateClassName(internalPackageNameMap, provider)).collect(toUnmodifiableList())
                );
          });
          descriptor.uses().forEach(uses -> builder.uses(interpolateClassName(internalPackageNameMap, uses)));
        });
      builder.packages(newPackages);
      var moduleDescriptor = builder.build();
      
      outputStream.write(ModuleHelper.moduleDescriptorToBinary(moduleDescriptor));
    }
  }
  
  private static Optional<String> getPackage(String resource) {
    return Optional.of(resource.lastIndexOf('/')).filter(index -> index != -1).map(index -> resource.substring(0, index).replace('/', '.'));
  }
  
  private static String interpolateInternalName(Map<String, String> internalPackageNameMap, String internalName) {
    var index = internalName.lastIndexOf('/');
    if (index == -1) {
      return internalName;
    }
    var internalPackageName = internalName.substring(0, index);
    return matchInternalPackagePrefix(internalPackageNameMap, internalPackageName)
        .map(packageName -> packageName + '/' + internalName.substring(index + 1))
        .orElse(internalName);
  }
  
  private static String interpolateClassName(Map<String, String> internalPackageNameMap, String className) {
    var index = className.lastIndexOf('.');
    if (index == -1) {
      return className;
    }
    var internalPackageName = className.substring(0, index).replace('.', '/');
    return matchInternalPackagePrefix(internalPackageNameMap, internalPackageName)
        .map(packageName -> packageName.replace('/', '.') + '.' + className.substring(index + 1))
        .orElse(className);
  }
  
  private static byte[] rewriteBytecode(Map<String, String> internalPackageNameMap, byte[] bytecode) {
    ClassReader classReader = new ClassReader(bytecode);
    ClassWriter classWriter = new ClassWriter(0);
    classReader.accept(new ClassRemapper(classWriter, new Remapper() {
      @Override
      public String map(String internalName) {
        return interpolateInternalName(internalPackageNameMap, internalName);
      }
      @Override
      public String mapPackageName(String internalPackageName) {
        return matchInternalPackagePrefix(internalPackageNameMap, internalPackageName)
            .orElse(internalPackageName);
      }
      @Override
      public Object mapValue(Object value) {
        if (!(value instanceof String)) {  
          return super.mapValue(value);
        }

        // try to patch if it's like a class name (dot form and slash form)
        var string = (String)value;
        
        if (lookLikeAClassName(string)) {
          var index = string.lastIndexOf('.');
          return matchInternalPackagePrefix(internalPackageNameMap, string.substring(0, index).replace('.', '/'))
              .map(packageName -> packageName.replace('/', '.') + '.' + string.substring(index + 1))
              .map(result -> { System.out.println("literal constant string " + string +" as " + result); return result;})
              .orElse(string);
        }
        return string;
      }
    }), 0);
    return classWriter.toByteArray();
  }
  
  private static Optional<String> matchInternalPackagePrefix(Map<String, String> internalPackageNameMap, String internalPackageName) {
    var newPackageName = internalPackageNameMap.get(internalPackageName);
    if (newPackageName != null) {
      return Optional.of(newPackageName);
    }
    var index = internalPackageName.lastIndexOf('/');
    if (index == -1) {
      return Optional.empty();
    }
    return matchInternalPackagePrefix(internalPackageNameMap, internalPackageName.substring(0, index))
        .map(name -> name + '/' + internalPackageName.substring(index + 1));
  }
  
  private static boolean lookLikeAClassName(String name) {
    var index = name.indexOf('.');
    if (index == -1 || index == name.length() - 1) {
      return false;
    }
    var tokens = name.split("\\.");
    if (tokens.length == 0) {
      return false;
    }
    return Arrays.stream(tokens).allMatch(token -> isAJavaIdentifier(token)) && startWithAnUpperCaseLetter(tokens[tokens.length - 1]);
  }
  
  private static boolean isAJavaIdentifier(String token) {
    return token.length() >=1 && isJavaIdentifierStart(token.charAt(0)) && token.chars().skip(1).allMatch(Character::isJavaIdentifierPart);
  }
  private static boolean startWithAnUpperCaseLetter(String token) {
    return Character.isUpperCase(token.charAt(0));
  }
}
