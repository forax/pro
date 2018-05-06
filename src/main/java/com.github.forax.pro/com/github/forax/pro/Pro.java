package com.github.forax.pro;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.StackWalker.StackFrame;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.Task;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.api.impl.Configs.DuplicatableConfig;
import com.github.forax.pro.api.impl.Configs.Query;
import com.github.forax.pro.api.impl.DefaultConfig;
import com.github.forax.pro.api.impl.Plugins;
import com.github.forax.pro.daemon.Daemon;
import com.github.forax.pro.helper.Log;
import com.github.forax.pro.helper.util.StableList;

public class Pro {
  private Pro() {
    throw new AssertionError();
  }
  
  static final ThreadLocal<DuplicatableConfig> CONFIG = new ThreadLocal<>() {
    @Override
    protected DuplicatableConfig initialValue() {
      var config = new DefaultConfig();
      var proConf = config.getOrUpdate("pro", ProConf.class);
      proConf.currentDir(Paths.get("."));
      proConf.pluginDir(Optional.ofNullable(System.getenv("PRO_PLUGIN_DIR"))
          .map(Paths::get)
          .orElseGet(Plugins::defaultPluginDir));
      var logLevel = Optional.ofNullable(System.getenv("PRO_LOG_LEVEL"))
        .map(Log.Level::of)
        .orElse(Log.Level.INFO);
      proConf.loglevel(logLevel.name().toLowerCase());
      proConf.exitOnError(Boolean.valueOf(System.getProperty("pro.exitOnError", "false")));
      var arguments = Optional.ofNullable(System.getProperty("pro.arguments", null))
          .map(value -> List.of(value.split(",")))
          .orElse(List.of());
      proConf.arguments(arguments);
      return config;
    }
  };
  static final HashMap<String, Plugin> PLUGINS;
  static {
    // initialization
    var config = (DefaultConfig)CONFIG.get();

    var proConf = config.getOrThrow("pro", ProConf.class);
    var log = Log.create("pro", proConf.loglevel());
    var plugins = Plugins.getAllPlugins(proConf.pluginDir());
    log.info(plugins, ps -> "registered plugins " + ps.stream().map(Plugin::name).collect(Collectors.joining(", ")));

    plugins.forEach(plugin -> plugin.init(config.asChecked(plugin.name())));
    plugins.forEach(plugin -> plugin.configure(config.asChecked(plugin.name())));
    
    PLUGINS = plugins.stream().collect(toMap(Plugin::name, identity(), (_1, _2) -> { throw new AssertionError(); }, HashMap::new));
  }
  
  public static void loadPlugins(Path dynamicPluginDir) {
    var config = CONFIG.get();
    var plugins = Plugins.getDynamicPlugins(dynamicPluginDir);
    for(var plugin : plugins) {
      var pluginName = plugin.name();
      if (PLUGINS.putIfAbsent(pluginName, plugin) == null) {
        plugin.init(config.asChecked(pluginName));
        plugin.configure(config.asChecked(pluginName));
      }
    }
  }
  
  public static void set(String key, Object value) {
    CONFIG.get().set(key, value);
  }
  
  public static <T> Optional<T> get(String key, Class<T> type) {
    return CONFIG.get().get(key, type);
  }
  
  public static <T> T getOrElseThrow(String key, Class<T> type) {
    return get(key, type)
        .orElseThrow(() -> new IllegalStateException("unknown key " + key));
  }
  
  public static <T> T getOrUpdate(String key, Class<T> type) {
    return CONFIG.get().getOrUpdate(key, type);
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
  
  public static URI uri(String uri) {
    try {
      return new URI(uri);
    } catch(URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
  
  @SafeVarargs
  public static <T> StableList<T> list(T... elements) {
    return StableList.of(elements);
  }
  public static <T> StableList<T> list(Collection<? extends T> collection) {
    return StableList.from(collection);
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
    var processBuilder = new ProcessBuilder(commands);
    processBuilder.inheritIO();
    int errorCode;
    try {
      var process = processBuilder.start();
      errorCode = process.waitFor();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      return;  //TODO revisit ?
    }
    exitIfNonZero(errorCode);
  }*/
  

  private static Optional<Plugin> findPluginByName(Map<String, Plugin> allPlugins, String pluginName, ProConf proConf) {
    var pluginOpt = Optional.ofNullable(allPlugins.get(pluginName));
    if (!pluginOpt.isPresent()) {
      var log = Log.create("pro", proConf.loglevel());
      log.error(pluginName, name -> "unknown plugin " + name);  
    }
    return pluginOpt;
  }
  
  public static void local(String localDir, Runnable action) { 
    var oldConfig = CONFIG.get();
    var currentDir = oldConfig.getOrThrow("pro", ProConf.class).currentDir();
    try {
      var newConfig = oldConfig.duplicate();
      newConfig.getOrUpdate("pro", ProConf.class).currentDir(currentDir.resolve(localDir));
      CONFIG.set(newConfig);
      action.run();
    } finally {
      CONFIG.set(oldConfig);
    }
  }
  
  public static Task task(Runnable action) {
    var line = StackWalker.getInstance().walk(s -> s.findFirst()).map(StackFrame::getLineNumber).orElse(-1);
    return new Task() {
      @Override
      public String name() {
        return "task at " + line;
      }
      @Override
      public int execute(Config config) throws IOException {
        var oldConfig = CONFIG.get();
        try {
          CONFIG.set(DefaultConfig.asNonMutable(config));
          action.run();
        } finally {
          CONFIG.set(oldConfig);
        }
        return 0; // FIXME
      }
    };
  }
  
  public static void run(Object... tasks) {
    var config = CONFIG.get();
    var proConf = config.getOrThrow("pro", ProConf.class);
    
    var taskList = new ArrayList<Task>();
    for(Object task: tasks) {
      if (task instanceof Task) {
        taskList.add((Task)task);
      } else {
        var pluginName = (task instanceof Query)? ((Query)task)._id_(): task.toString();
        var pluginOpt = findPluginByName(PLUGINS, pluginName, proConf);
        if (!pluginOpt.isPresent()) {
          // plugin not found
          exit(proConf.exitOnError(), 1);  //FIXME
          return;
        }
        taskList.add(pluginOpt.get());
      }
    }
    
    runAll(config, taskList);
  }
  
  public static void run(String... pluginNames) {
    run(List.of(pluginNames));
  }
  
  public static void run(List<String> pluginNames) {
    var config = CONFIG.get();
    var proConf = config.getOrThrow("pro", ProConf.class);
    
    var plugins = new ArrayList<Task>();
    for(var pluginName: pluginNames) {
      var pluginOpt = findPluginByName(PLUGINS, pluginName, proConf);  
      if (!pluginOpt.isPresent()) {
        // plugin not found
        exit(proConf.exitOnError(), 1);  //FIXME
        return;
      }
      plugins.add(pluginOpt.get());
    }
    
    runAll(config, plugins);
  }
  
  private static void runAll(DuplicatableConfig config, List<Task> tasks) {
    var taskConfig = config.duplicate().asConfig();
    
    var daemonOpt = ServiceLoader.load(Daemon.class, ClassLoader.getSystemClassLoader()).findFirst();
    var consumer = daemonOpt
        .filter(Daemon::isStarted)
        .<BiConsumer<List<Task>, Config>>map(daemon -> daemon::execute)
        .orElse(Pro::executeAll);
    consumer.accept(tasks, taskConfig);
  }
  
  private static void executeAll(List<Task> tasks, Config config) {
    var proConf = config.getOrThrow("pro", ProConf.class);
    var exitOnError = proConf.exitOnError();
    
    var errorCode = 0;
    var start = System.currentTimeMillis();
    for(var task: tasks) {
      errorCode = execute(task, config);
      if (errorCode != 0) {
        break;
      }
    }
    var end = System.currentTimeMillis();
    var elapsed = end - start;
    
    var log = Log.create("pro", proConf.loglevel());
    if (errorCode == 0) {
      log.info(elapsed, time -> String.format("DONE !          elapsed time %,d ms", time));
    } else {
      log.error(elapsed, time -> String.format("FAILED !        elapsed time %,d ms", time));
    }
    
    if (errorCode != 0) {
      exit(exitOnError, errorCode);
    }
  }
  
  private static int execute(Task task, Config config) {
    try {
      return task.execute(config);
    } catch (IOException | /*UncheckedIOException |*/ RuntimeException e) {  //FIXME revisit RuntimeException !
      e.printStackTrace();
      var logLevel = config.getOrThrow("pro", ProConf.class).loglevel();
      var log = Log.create(task.name(), logLevel);
      log.error(e);
      return 1; // FIXME
    }
  }
  
  private static void exit(boolean exitOnError, int errorCode) {
    if (exitOnError) {
      System.exit(errorCode);
      throw new AssertionError("should have exited with code " + errorCode);
    }
  }
}
