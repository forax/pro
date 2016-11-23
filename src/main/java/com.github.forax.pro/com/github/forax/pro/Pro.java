package com.github.forax.pro;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.impl.Configs;
import com.github.forax.pro.api.impl.DefaultConfig;
import com.github.forax.pro.api.impl.Plugins;

public class Pro {
  private Pro() {
    throw new AssertionError();
  }
  
  private static final DefaultConfig CONFIG;
  private static final Map<String, Plugin> PLUGINS;
  static {
    Object root = Configs.root();
    DefaultConfig config = new DefaultConfig(root);
    
    List<Plugin> plugins = Plugins.getAllPlugins();
    //System.out.println("registered plugins " + plugins.stream().map(Plugin::name).collect(joining(", ")));
    
    
    plugins.forEach(plugin -> plugin.init(config.asChecked(plugin.name())));
    plugins.forEach(plugin -> plugin.configure(config.asChecked(plugin.name())));
    
    CONFIG = config;
    PLUGINS = plugins.stream()
        .collect(toMap(Plugin::name, identity()));
  }
  
  @SuppressWarnings("unchecked")  // emulate dynamic behavior here
  public static <T> T $(String key) {
    return (T)get(key, Object.class);
  }
  
  public static void set(String key, Object value) {
    CONFIG.set(key, value);
  }
  
  public static <T> T get(String key, Class<T> type) {
    return CONFIG.get(key, type)
        .orElseThrow(() -> new IllegalStateException("unknown key " + key));
  }
  
  public static void scope(Runnable runnable) {
    CONFIG.enter(runnable);
  }
  
  public static Path location(String location) {
    return Paths.get(location);
  }
  public static Path location(String first, String... more) {
    return Paths.get(first, more);
  }
  
  public static List<Path> path(String... locations) {
    return list(Arrays.stream(locations).map(Pro::location).toArray(Path[]::new));
  }
  public static List<Path> path(Path... locations) {
    return list(locations);
  }
  
  public static PathMatcher regex(String regex) {
    return globRegex(regex);
  }
  public static PathMatcher globRegex(String regex) {
    return FileSystems.getDefault().getPathMatcher("glob:" + regex);
  }
  public static PathMatcher perlRegex(String regex) {
    return FileSystems.getDefault().getPathMatcher("regex:" + regex);
  }
  
  public static List<Path> files(Path sourceDirectory) {
    return files(sourceDirectory, __ -> true);
  }
  public static List<Path> files(Path sourceDirectory, String globRegex) {
    return files(sourceDirectory, globRegex(globRegex));
  }
  public static List<Path> files(Path sourceDirectory, PathMatcher filter) {
    try {
      return list(Files.walk(sourceDirectory).filter(filter::matches));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
  
  @SafeVarargs
  public static <T> List<T> list(T... elements) {
    return List.of(elements);
  }
  @SuppressWarnings("unchecked")
  public static <T> List<T> list(Collection<? extends T> elements) {
    return list(elements.toArray((T[])new Object[0]));
  }
  @SuppressWarnings("unchecked")
  public static <T> List<T> list(Stream<? extends T> elements) {
    return list((T[])elements.toArray(Object[]::new));
  }
  
  public static Path resolve(Path location, Path child) {
    return location.resolve(child);
  }
  public static Path resolve(Path location, String child) {
    return location.resolve(child);
  }
  
  @SuppressWarnings("unchecked")  // emulate a dynamic language behavior
  public static <T> List<T> append(Object... elements) {
    return (List<T>)list(Arrays.stream(elements).flatMap(element -> {
      if (element instanceof Collection) {
        return ((Collection<?>)element).stream();
      }
      if (element instanceof Object[]) {
        return Arrays.stream((Object[])element);
      }
      return Stream.of(element);
    }));
  }
  /*
  @SafeVarargs
  public static <T> List<T> append(Collection< ? extends T> locations, T... others) {
    return list(Stream.concat(locations.stream(), Arrays.stream(others)));
  }
  public static List<Path> append(Collection<? extends Path> paths, String... others) {
    return list(Stream.concat(paths.stream(), Arrays.stream(others).map(Pro::location)));
  }
  public static List<Path> append(Collection<? extends Path> path1, Collection<? extends Path> path2) {
    return flatten(path1, path2);
  }*/
  
  @SafeVarargs
  public static <T> List<T> flatten(Collection<? extends T>... collections) {
    return list(Arrays.stream(collections).flatMap(Collection::stream));
  }
  
  public static void print(Object... elements) {
    System.out.println(Arrays.stream(elements).map(Object::toString).collect(Collectors.joining(" ")));
  }
  
  public static void run(String... pluginNames) throws UncheckedIOException {
    Map<String, Plugin> plugins = PLUGINS;
    for(String pluginName: pluginNames) {
      Plugin plugin = Optional
          .ofNullable(plugins.get(pluginName))
          .orElseThrow(() -> new IllegalArgumentException("unknown plugin " + pluginName));
      int exitCode;
      try {
        exitCode = plugin.execute(CONFIG.asConfig());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      if (exitCode != 0) {
        System.exit(exitCode);
        throw new AssertionError("should have exited with code " + exitCode);
      }
    }
  }
}
