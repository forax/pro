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
import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.api.impl.Configs;
import com.github.forax.pro.api.impl.DefaultConfig;
import com.github.forax.pro.api.impl.Plugins;
import com.github.forax.pro.daemon.Daemon;
import com.github.forax.pro.helper.Log;
import com.github.forax.pro.helper.util.StableList;

public class Pro {
  private Pro() {
    throw new AssertionError();
  }
  
  private static final ThreadLocal<DefaultConfig> CONFIG = new ThreadLocal<>() {
    @Override
    protected DefaultConfig initialValue() {
      Object root = Configs.newRoot();
      DefaultConfig config = new DefaultConfig(root);
      ProConf proConf = config.getOrUpdate("pro", ProConf.class);
      
      Log.Level logLevel = Optional.ofNullable(System.getenv("PRO_LOG_LEVEL"))
        .map(Log.Level::of)
        .orElse(Log.Level.INFO);
      proConf.loglevel(logLevel.name().toLowerCase());
      proConf.exitOnError(false);
      return config;
    }
    @Override
    public void set(DefaultConfig value) {
      throw new UnsupportedOperationException();
    }
  };
  private static final Map<String, Plugin> PLUGINS;
  static {
    DefaultConfig config = CONFIG.get();
    
    List<Plugin> plugins = Plugins.getAllPlugins();
    Log log = Log.create("pro", config.getOrThrow("pro", ProConf.class).loglevel());
    log.info(plugins, ps -> "registered plugins " + ps.stream().map(Plugin::name).collect(Collectors.joining(", ")));
    
    plugins.forEach(plugin -> plugin.init(config.asChecked(plugin.name())));
    plugins.forEach(plugin -> plugin.configure(config.asChecked(plugin.name())));
    
    PLUGINS = plugins.stream().collect(toMap(Plugin::name, identity()));
  }
  
  @SuppressWarnings("unchecked")  // emulate dynamic behavior here
  public static <T> T $(String key) {
    return (T)get(key, Object.class);
  }
  
  public static void set(String key, Object value) {
    CONFIG.get().set(key, value);
  }
  
  public static <T> T get(String key, Class<T> type) {
    return CONFIG.get().get(key, type)
        .orElseThrow(() -> new IllegalStateException("unknown key " + key));
  }
  
  /*public static void scope(Runnable runnable) {
    CONFIG.enter(runnable);
  }*/
  
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
    return list(stream.collect(StableList.toStableList()));
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
  
  /*
  public static void exec(String... commands) {
    ProcessBuilder processBuilder = new ProcessBuilder(commands);
    processBuilder.inheritIO();
    int errorCode;
    try {
      Process process = processBuilder.start();
      errorCode = process.waitFor();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      return;  //TODO revisit ?
    }
    exitIfNonZero(errorCode);
  }*/
  

  private static int checkPluginNames(Map<String, Plugin> allPlugins, String[] pluginNames, Config config, ArrayList<Plugin> plugins) {
    int exitCode = 0;
    for(String pluginName: pluginNames) {  
      Plugin plugin = allPlugins.get(pluginName);
      if (plugin == null) {
        Log log = Log.create("pro", config.get("loglevel", String.class).orElse("debug"));
        log.error(pluginName, name -> "unknown plugin " + name);
        exitCode = 1;  // FIXME
      }
      plugins.add(plugin);
    }
    return exitCode;
  }
  
  public static void run(String... pluginNames) {
    DefaultConfig config = CONFIG.get();
    ProConf proConf = config.getOrThrow("pro", ProConf.class);
    
    ArrayList<Plugin> plugins = new ArrayList<>();
    int errorCode = checkPluginNames(PLUGINS, pluginNames, config, plugins);
    if (errorCode != 0) {
      exit(proConf.exitOnError(), errorCode);
      return;
    }
    
    Config pluginConfig = config.duplicate().asConfig();
    
    Optional<Daemon> daemonService = ServiceLoader.load(Daemon.class, ClassLoader.getSystemClassLoader()).findFirst();
    BiConsumer<List<Plugin>, Config> consumer = daemonService
        .filter(Daemon::isStarted)
        .<BiConsumer<List<Plugin>, Config>>map(daemon -> daemon::run)
        .orElse(Pro::runAll);
    consumer.accept(plugins, pluginConfig);
  }
  
  private static void runAll(List<Plugin> plugins, Config config) {
    ProConf proConf = config.getOrThrow("pro", ProConf.class);
    boolean exitOnError = proConf.exitOnError();
    
    int errorCode = 0;
    long start = System.currentTimeMillis();
    for(Plugin plugin: plugins) {
      errorCode = execute(plugin, config);
      if (errorCode != 0) {
        exit(exitOnError, errorCode);
        break;
      }
    }
    long end = System.currentTimeMillis();
    long elapsed = end - start;
    
    Log log = Log.create("pro", proConf.loglevel());
    if (errorCode == 0) {
      log.info(elapsed, time -> String.format("DONE !          elapsed time %,d ms", time));
    } else {
      log.error(elapsed, time -> String.format("FAILED !        elapsed time %,d ms", time));
    }
  }
  
  private static int execute(Plugin plugin, Config config) {
    int errorCode;
    try {
      errorCode = plugin.execute(config);
    } catch (IOException | /*UncheckedIOException |*/ RuntimeException e) {  //FIXME revisit RuntimeException !
      e.printStackTrace();
      String logLevel = config.getOrThrow("pro", ProConf.class).loglevel();
      Log log = Log.create(plugin.name(), logLevel);
      log.error(e);
      errorCode = 1; // FIXME
    }
    return errorCode;
  }
  
  private static void exit(boolean exitOnError, int errorCode) {
    if (exitOnError) {
      System.exit(errorCode);
      throw new AssertionError("should have exited with code " + errorCode);
    }
  }
}
