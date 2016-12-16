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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.impl.Configs;
import com.github.forax.pro.api.impl.DefaultConfig;
import com.github.forax.pro.api.impl.Plugins;
import com.github.forax.pro.helper.Log;
import com.github.forax.pro.helper.Log.Level;
import com.github.forax.pro.helper.StableList;

public class Pro {
  private Pro() {
    throw new AssertionError();
  }
  
  private static final DefaultConfig CONFIG;
  private static final Map<String, Plugin> PLUGINS;
  static {
    Object root = Configs.root();
    DefaultConfig config = new DefaultConfig(root);
    Log.Level logLevel = Optional.ofNullable(System.getenv("PRO_LOG_LEVEL"))
      .map(Log.Level::of)
      .orElse(Log.Level.INFO);
    config.set("loglevel",   logLevel.name().toLowerCase());
    
    Log log = Log.create("pro", logLevel);
    
    List<Plugin> plugins = Plugins.getAllPlugins();
    log.info(plugins, ps -> "registered plugins " + ps.stream().map(Plugin::name).collect(Collectors.joining(", ")));
    
    plugins.forEach(plugin -> plugin.init(config.asChecked(plugin.name())));
    plugins.forEach(plugin -> plugin.configure(config.asChecked(plugin.name())));
    
    CONFIG = config;
    PLUGINS = plugins.stream().collect(toMap(Plugin::name, identity()));
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
  
  public static StableList<Path> path(String... locations) {
    return list(Arrays.stream(locations).map(Pro::location));
  }
  public static StableList<Path> path(Path... locations) {
    return list(locations);
  }
  
  public static PathMatcher globRegex(String regex) {
    return FileSystems.getDefault().getPathMatcher("glob:" + regex);
  }
  public static PathMatcher perlRegex(String regex) {
    return FileSystems.getDefault().getPathMatcher("regex:" + regex);
  }
  
  public static StableList<Path> files(Path sourceDirectory) {
    return files(sourceDirectory, __ -> true);
  }
  public static StableList<Path> files(Path sourceDirectory, String globRegex) {
    return files(sourceDirectory, globRegex(globRegex));
  }
  public static StableList<Path> files(Path sourceDirectory, PathMatcher filter) {
    try {
      return list(Files.walk(sourceDirectory).filter(filter::matches));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
  
  @SafeVarargs
  public static <T> StableList<T> list(T... elements) {
    return StableList.of(elements);
  }
  public static <T> StableList<T> list(Collection<? extends T> collection) {
    return StableList.<T>of().appendAll(collection);
  }
  public static <T> StableList<T> list(Stream<? extends T> stream) {
    return list(stream.collect(StableList.asStableList()));
  }
  
  public static Path resolve(Path location, Path child) {
    return location.resolve(child);
  }
  public static Path resolve(Path location, String child) {
    return location.resolve(child);
  }
  
  /* Not sure of this one
  @SuppressWarnings("unchecked")  // emulate a dynamic language behavior
  public static <T> StableList<T> append(Object... elements) {
    return (StableList<T>)list(Arrays.stream(elements).flatMap(element -> {
      if (element instanceof Collection) {
        return ((Collection<?>)element).stream();
      }
      if (element instanceof Object[]) {
        return Arrays.stream((Object[])element);
      }
      return Stream.of(element);
    }));
  }*/
  
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
  
  public static void run(String... pluginNames) {
    ArrayList<Plugin> plugins = new ArrayList<>();
    int exitCode = checkPluginNames(PLUGINS, pluginNames, plugins);
    
    if (exitCode == 0) {
      for(Plugin plugin: plugins) {
        try {
          exitCode = plugin.execute(CONFIG.asConfig());
        } catch (IOException | /*UncheckedIOException |*/ RuntimeException e) {  //FIXME revisit RuntimeException !
          Log log = Log.create(plugin.name(), CONFIG.getOrThrow("loglevel", String.class));
          log.error(e);
          exitCode = 1; // FIXME
        }
      }
    }
    
    if (exitCode != 0) {
      System.exit(exitCode);
      throw new AssertionError("should have exited with code " + exitCode);
    }
  }

  private static int checkPluginNames(Map<String, Plugin> allPlugins, String[] pluginNames, ArrayList<Plugin> plugins) {
    int exitCode = 0;
    for(String pluginName: pluginNames) {  
      Plugin plugin = allPlugins.get(pluginName);
      if (plugin == null) {
        Log log = Log.create("pro", CONFIG.getOrThrow("loglevel", String.class));
        log.error(pluginName, name -> "unknown plugin " + name);
        exitCode = 1;  // FIXME
      }
      plugins.add(plugin);
    }
    return exitCode;
  }
}
