package com.github.forax.pro.api.impl;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.github.forax.pro.api.Plugin;

public class Plugins {
  private static Stream<Provider<Plugin>> findDynamicPlugins(Path path) {
    ModuleFinder finder = ModuleFinder.of(path);
    Collection<String> moduleNames = finder.findAll().stream().map(ref -> ref.descriptor().name()).collect(toSet());
    
    ModuleLayer boot = ModuleLayer.boot();
    Configuration cf = boot.configuration().resolve(finder, ModuleFinder.of(), moduleNames);

    ClassLoader classLoader = new ClassLoader(Plugins.class.getClassLoader()) { /* empty */ };
    ModuleLayer layer = boot.defineModulesWithOneLoader(cf, classLoader);

    ServiceLoader<Plugin> serviceLoader = ServiceLoader.load(layer, Plugin.class);
    return serviceLoader.stream();
  }
  
  public static List<Plugin> getAllPlugins() {
    // load core plugins
    ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class, Plugin.class.getClassLoader());
    HashMap<Class<?>, Plugin> pluginMap = new HashMap<>();
    Consumer<Provider<Plugin>> addToMap = provider -> pluginMap.computeIfAbsent(provider.type(), __ -> provider.get());
    loader.stream().forEach(addToMap);
    
    // load dynamic plugins
    Path bootLibraryPath = Paths.get(
        System.getProperty("sun.boot.library.path"));
    Path pluginPath = bootLibraryPath.resolveSibling("plugins");
    if (Files.isDirectory(pluginPath)) { 
      try(Stream<Path> stream = Files.list(pluginPath)) {
        stream.flatMap(Plugins::findDynamicPlugins).forEach(addToMap);
      } catch(IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    
    return pluginMap.values().stream()
        .sorted(Comparator.comparing(Plugin::name))   // have a stable order
        .collect(toList());
  }
}
