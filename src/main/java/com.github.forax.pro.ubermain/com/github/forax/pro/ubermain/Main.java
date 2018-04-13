package com.github.forax.pro.ubermain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleDescriptor.Modifier;
import java.lang.module.ModuleDescriptor.Opens;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Main {
  private static URI baseURI(URI uri) {
    var uriName = uri.toString();
    var index = uriName.indexOf('!');
    return URI.create(uriName.substring(0, index));
  }
  
  static String packageOf(String className) {
    var index = className.lastIndexOf('.');
    return className.substring(0, index);
  }
  
  public static void main(String[] args) throws IOException, URISyntaxException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
    List<String> lines;
    try(var input = Main.class.getResourceAsStream("/modules.txt");
        var reader = new InputStreamReader(input);
        var bufferedReader = new BufferedReader(reader);
        var stream = bufferedReader.lines()) {
      lines = stream.collect(Collectors.toList());
    }
    
    var tokens = lines.get(0).split("/");
    var mainModule = tokens[0];
    var mainClassName = tokens[1];
    var moduleFileNames = lines.subList(1, lines.size());
    
    //System.out.println(moduleFileNames);
    var firstFileName = moduleFileNames.get(0);
    var baseURI = baseURI(Main.class.getResource("/" + firstFileName).toURI());
    
    System.out.println(baseURI);
    
    // uncompress into a temporary directory
    var tmp = Files.createTempDirectory("uberjar");
    try(var zipfs = FileSystems.newFileSystem(baseURI, Map.of())) {
      for(var moduleFileName: moduleFileNames) {
        var modulePath = zipfs.getPath("/" + moduleFileName);
        Files.copy(modulePath, tmp.resolve(modulePath.getFileName().toString()));
      }
    }
    
    var finder = ModuleFinder.of(tmp);
    //System.out.println(finder.findAll().stream().map(ref -> ref.descriptor().name()).collect(Collectors.toList()));
    
    var patchedFinder = new ModuleFinder() {
      private ModuleReference main;
      
      @Override
      public Set<ModuleReference> findAll() {
        return finder.findAll().stream().map(ref -> find(ref.descriptor().name()).get()).collect(Collectors.toSet());
      }
      
      @Override
      public Optional<ModuleReference> find(String name) {
        var moduleOpt = finder.find(name);
        if (!moduleOpt.isPresent() || !name.equals(mainModule)) {
          return moduleOpt;
        }
        if (main == null) {
          var ref = moduleOpt.get();
          var descriptor = ref.descriptor();
          
          var modifiers = descriptor.isOpen()? Set.of(Modifier.OPEN):
            descriptor.isAutomatic()? Set.of(Modifier.AUTOMATIC): Set.<Modifier>of();
          var builder = ModuleDescriptor.newModule(name, modifiers);
          descriptor.version().ifPresent(builder::version);
          
          descriptor.requires().forEach(builder::requires);
          descriptor.exports().forEach(builder::exports);
          builder.exports(packageOf(mainClassName), Set.of("com.github.forax.pro.uberbooter"));   // patch !
          descriptor.opens().forEach(builder::opens);
          
          descriptor.uses().forEach(builder::uses);
          descriptor.provides().forEach(builder::provides);
          
          var packages = new HashSet<>(descriptor.packages());
          packages.removeAll(descriptor.exports().stream().map(Exports::source).collect(Collectors.toSet()));
          packages.removeAll(descriptor.opens().stream().map(Opens::source).collect(Collectors.toSet()));
          packages.remove(packageOf(mainClassName));
          builder.packages(packages);
          
          main = new ModuleReference(builder.build(), ref.location().get()) {
            @Override
            public ModuleReader open() throws IOException {
              return ref.open();
            }
          };
        }
        return Optional.of(main);
      }
    };
    
    var parent = ModuleLayer.boot();
    var configuration = parent.configuration().resolveAndBind(patchedFinder, ModuleFinder.of(), List.of("com.github.forax.pro.uberbooter", mainModule));
    var layer = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(parent), ClassLoader.getSystemClassLoader()).layer();
    
    var loader = layer.findLoader("com.github.forax.pro.uberbooter");
    var booterClass = loader.loadClass("com.github.forax.pro.uberbooter.Booter");
    
    var mainLoader = layer.findLoader(mainModule);
    var mainClass = mainLoader.loadClass(mainClassName);
    
    var lookup = MethodHandles.lookup();
    try {
      var mh = lookup.findStatic(booterClass, "main", MethodType.methodType(void.class, Class.class, String[].class));
      mh.invokeExact(mainClass, args);
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw e;
    } catch (RuntimeException | Error e) {
      throw e;
    } catch (Throwable e) {
      throw new UndeclaredThrowableException(e);
    }
  }
}
