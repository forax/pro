package com.github.forax.pro.ubermain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleDescriptor.Opens;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.reflect.Layer;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
  private static URI baseURI(URI uri) {
    String uriName = uri.toString();
    int index = uriName.indexOf('!');
    return URI.create(uriName.substring(0, index));
  }
  
  static String packageOf(String className) {
    int index = className.lastIndexOf('.');
    return className.substring(0, index);
  }
  
  public static void main(String[] args) throws IOException, URISyntaxException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
    List<String> lines;
    try(InputStream input = Main.class.getResourceAsStream("/modules.txt");
        Reader reader = new InputStreamReader(input);
        BufferedReader bufferedReader = new BufferedReader(reader);
        Stream<String> stream = bufferedReader.lines()) {
      
      lines = stream.collect(Collectors.toList());
    }
    
    String[] tokens = lines.get(0).split("/");
    String mainModule = tokens[0];
    String mainClassName = tokens[1];
    List<String> moduleFileNames = lines.subList(1, lines.size());
    
    //System.out.println(moduleFileNames);
    String firstFileName = moduleFileNames.get(0);
    URI baseURI = baseURI(Main.class.getResource("/" + firstFileName).toURI());
    
    System.out.println(baseURI);
    
    // uncompress into a temporary directory
    Path tmp = Files.createTempDirectory("uberjar");
    try(FileSystem zipfs = FileSystems.newFileSystem(baseURI, Map.of())) {
      for(String moduleFileName: moduleFileNames) {
        Path modulePath = zipfs.getPath("/" + moduleFileName);
        Files.copy(modulePath, tmp.resolve(modulePath.getFileName().toString()));
      }
    }
    
    ModuleFinder finder = ModuleFinder.of(tmp);
    //System.out.println(finder.findAll().stream().map(ref -> ref.descriptor().name()).collect(Collectors.toList()));
    
    ModuleFinder patchedFinder = new ModuleFinder() {
      private ModuleReference main;
      
      @Override
      public Set<ModuleReference> findAll() {
        return finder.findAll().stream().map(ref -> find(ref.descriptor().name()).get()).collect(Collectors.toSet());
      }
      
      @Override
      public Optional<ModuleReference> find(String name) {
        Optional<ModuleReference> moduleOpt = finder.find(name);
        if (!moduleOpt.isPresent() || !name.equals(mainModule)) {
          return moduleOpt;
        }
        if (main == null) {
          ModuleReference ref = moduleOpt.get();
          ModuleDescriptor descriptor = ref.descriptor();
          ModuleDescriptor.Builder builder = descriptor.isOpen()? ModuleDescriptor.openModule(name):
            descriptor.isAutomatic()? ModuleDescriptor.automaticModule(name):
              ModuleDescriptor.module(name);
          descriptor.version().ifPresent(builder::version);
          
          descriptor.requires().forEach(builder::requires);
          descriptor.exports().forEach(builder::exports);
          builder.exports(packageOf(mainClassName), Set.of("com.github.forax.pro.uberbooter"));   // patch !
          descriptor.opens().forEach(builder::opens);
          
          descriptor.uses().forEach(builder::uses);
          descriptor.provides().forEach(builder::provides);
          
          HashSet<String> packages = new HashSet<>(descriptor.packages());
          packages.removeAll(descriptor.exports().stream().map(Exports::source).collect(Collectors.toSet()));
          packages.removeAll(descriptor.opens().stream().map(Opens::source).collect(Collectors.toSet()));
          packages.remove(packageOf(mainClassName));
          packages.forEach(builder::contains);
          
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
    
    Layer parent = Layer.boot();
    Configuration cf = parent.configuration().resolveRequiresAndUses(patchedFinder, ModuleFinder.of(), List.of("com.github.forax.pro.uberbooter", mainModule));
    Layer layer = Layer.defineModulesWithOneLoader(cf, List.of(parent), ClassLoader.getSystemClassLoader()).layer();
    
    ClassLoader loader = layer.findLoader("com.github.forax.pro.uberbooter");
    Class<?> booterClass = loader.loadClass("com.github.forax.pro.uberbooter.Booter");
    
    ClassLoader mainLoader = layer.findLoader(mainModule);
    Class<?> mainClass = mainLoader.loadClass(mainClassName);
    
    Lookup lookup = MethodHandles.lookup();
    try {
      MethodHandle mh = lookup.findStatic(booterClass, "main", MethodType.methodType(void.class, Class.class, String[].class));
      mh.invokeExact(mainClass, args);
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw e;
    } catch (Throwable e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException)e;
      }
      if (e instanceof Error) {
        throw (Error)e;
      }
      throw new UndeclaredThrowableException(e);
    }
  }
}
