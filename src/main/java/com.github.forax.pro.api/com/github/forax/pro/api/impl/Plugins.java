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
import java.util.ArrayList;
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
  public static Path defaultPluginDir() {
    return Paths.get(System.getProperty("sun.boot.library.path")).resolveSibling("plugins");
  }
  
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
  
  private static void loadDynamicPlugins(Path dynamicPluginDir, Consumer<? super Provider<Plugin>> consumer) {
    if (Files.isDirectory(dynamicPluginDir)) { 
      try(Stream<Path> stream = Files.list(dynamicPluginDir)) {
        stream.flatMap(Plugins::findDynamicPlugins).forEach(consumer);
      } catch(IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  } 
  
  public static List<Plugin> getDynamicPlugins(Path dynamicPluginDir) {
    ArrayList<Plugin> plugins = new ArrayList<>();
    loadDynamicPlugins(dynamicPluginDir, provider -> plugins.add(provider.get()));
    plugins.sort(Comparator.comparing(Plugin::name));   // have a stable order
    return plugins;
  }
  
  public static List<Plugin> getAllPlugins(Path dynamicPluginDir) {
    HashMap<Class<?>, Plugin> pluginMap = new HashMap<>();
    Consumer<Provider<Plugin>> addToMap = provider -> pluginMap.computeIfAbsent(provider.type(), __ -> provider.get());
    
    // load core plugins
    ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class, Plugin.class.getClassLoader());
    loader.stream().forEach(addToMap);
    
    // load dynamic plugins
    loadDynamicPlugins(dynamicPluginDir, addToMap);
    
    return pluginMap.values().stream()
        .sorted(Comparator.comparing(Plugin::name))   // have a stable order
        .collect(toList());
  }
}
