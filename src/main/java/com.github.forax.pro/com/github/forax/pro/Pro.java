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
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.Command;
import com.github.forax.pro.api.helper.ProConf;
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
  
  static final ThreadLocal<DefaultConfig> CONFIG = new ThreadLocal<>() {
    @Override
    protected DefaultConfig initialValue() {
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
      proConf.exitOnError(Boolean.valueOf(System.getProperty("pro.exitOnError", "true")));
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
    var config = CONFIG.get();

    var proConf = config.getOrThrow("pro", ProConf.class);
    
    List<Plugin> plugins;
    try {
      plugins = Plugins.getAllPlugins(proConf.pluginDir());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    
    var log = Log.create("pro", proConf.loglevel());
    log.info(plugins, ps -> "registered plugins " + ps.stream().map(Plugin::name).collect(Collectors.joining(", ")));

    plugins.forEach(plugin -> plugin.init(config.asChecked(plugin.name())));
    plugins.forEach(plugin -> plugin.configure(config.asChecked(plugin.name())));
    
    PLUGINS = plugins.stream().collect(toMap(Plugin::name, identity(), (_1, _2) -> { throw new AssertionError(); }, HashMap::new));
  }
  
  /**
   * Dynamically load plugins from a local directory.
   * 
   * @param dynamicPluginDir a folder containing the plugins
   */
  public static void loadPlugins(Path dynamicPluginDir) {
    var config = CONFIG.get();
    List<Plugin> plugins;
    try {
      plugins = Plugins.getDynamicPlugins(dynamicPluginDir);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    for(var plugin : plugins) {
      var pluginName = plugin.name();
      if (PLUGINS.putIfAbsent(pluginName, plugin) == null) {
        plugin.init(config.asChecked(pluginName));
        plugin.configure(config.asChecked(pluginName));
      }
    }
  }
  
  /**
   * Change the value of a configuration key.
   * 
   * @param key a configuration key, a qualified (dot separated) string
   * @param value the new value associated to the configuration key
   */
  public static void set(String key, Object value) {
    CONFIG.get().set(key, value);
  }
  /**
   * Returns the value associated with the configuration key or an empty Optional.
   * 
   * @param <T> expected type of the value
   * @param key a configuration key, a qualified (dot separated) string
   * @param type the type of the value or an interface that will proxy the value
   * @return the value associated with the configuration key or an empty Optional.
   * 
   * @see #getOrElseThrow(String, Class)
   */
  public static <T> Optional<T> get(String key, Class<T> type) {
    return CONFIG.get().get(key, type);
  }
  /**
   * Returns the value associated with the configuration key.
   * 
   * @param <T> expected type of the value
   * @param key a configuration key, a qualified (dot separated) string
   * @param type the type of the value or an interface that will proxy the value
   * @return the value associated with the configuration key.
   * @throws IllegalStateException if there is no value for the associated key.
   * 
   * @see #get(String, Class)
   */
  public static <T> T getOrElseThrow(String key, Class<T> type) {
    return get(key, type)
        .orElseThrow(() -> new IllegalStateException("unknown key " + key));
  }
  /**
   * Returns the value associated with the configuration key,
   * if there is no value and type is an interface, the value will be auto-vivified.
   * 
   * @param <T> expected type of the value
   * @param key a configuration key, a qualified (dot separated) string
   * @param type the type of the value or an interface that will proxy the value
   * @return the value associated with the configuration key.
   * 
   * @see #get(String, Class)
   */
  public static <T> T getOrUpdate(String key, Class<T> type) {
    return CONFIG.get().getOrUpdate(key, type);
  }
  
  /**
   * Create a Path representing a file system path from a string,
   * to avoid cross-platform issues, the components of the path should be separated by a slash ('/')
   * @param location a string 
   * @return a new Path
   */
  public static Path location(String location) {
    return Paths.get(location);
  }
  /**
   * Create a Path representing a file system path from all the components of a path.
   * 
   * @param first the first component of the path
   * @param more the other components of the path, as a comma separated array
   * @return a new Path
   */
  public static Path location(String first, String... more) {
    return Paths.get(first, more);
  }
  
  /**
   * Create a file system path, a list of locations from an array of string
   * @param locations an array of location
   * @return a list of locations
   */
  public static StableList<Path> path(String... locations) {
    return list(Arrays.stream(locations).map(Pro::location));
  }
  /**
   * Create a file system path, a list of locations from an array of Path
   * @param locations an array of location
   * @return a list of locations
   */
  public static StableList<Path> path(Path... locations) {
    return list(locations);
  }
  
  
  /**
   * Return a file name matcher using 'glob' regular expression.
   * The 'glob' regular expression are defined in the javadoc of
   * {@link java.nio.file.FileSystem#getPathMatcher(String)}.
   * 
   * @param regex a 'glob' regular expression
   * @return a file matcher
   */
  public static PathMatcher globRegex(String regex) {
    return FileSystems.getDefault().getPathMatcher("glob:" + regex);
  }
  /**
   * Return a file name matcher using Perl-like regular expression.
   * The Perl-like regular expression are defined in the javadoc of
   * {@link java.util.regex.Pattern}.
   * 
   * @param regex a Perl-like regular expression
   * @return a file matcher
   */
  public static PathMatcher perlRegex(String regex) {
    return FileSystems.getDefault().getPathMatcher("regex:" + regex);
  }
  
  /**
   * Returns all files and sub-directories recursively stored in the sourceDirectory.
   * @param sourceDirectory a directory
   * @return a list of files (and directories)
   * 
   * @see #files(Path, PathMatcher)
   */
  public static StableList<Path> files(Path sourceDirectory) {
    return files(sourceDirectory, __ -> true);
  }
  /**
   * Returns all files and sub-directories recursively stored in the sourceDirectory
   * that match a regular expression.
   * 
   * @param sourceDirectory a directory
   * @param globRegex a file regular expression
   * @return a list of files (and directories)
   * 
   * @see #files(Path, PathMatcher)
   */
  public static StableList<Path> files(Path sourceDirectory, String globRegex) {
    return files(sourceDirectory, globRegex(globRegex));
  }
  /**
   * Returns all files and sub-directories recursively stored in the sourceDirectory
   * that match a filter.
   * 
   * @param sourceDirectory a directory
   * @param filter a path matcher 
   * @return a list of files (and directories)
   */
  public static StableList<Path> files(Path sourceDirectory, PathMatcher filter) {
    try(var stream = Files.walk(sourceDirectory)) {
      return list(stream.filter(filter::matches));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
  
  /**
   * Creates an URI from a string.
   * 
   * @param uri a string containing an URI
   * @return a, URI
   */
  public static URI uri(String uri) {
    try {
      return new URI(uri);
    } catch(URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Creates a List from elements.
   * Each elements must be non null.
   * 
   * @param <T> type of the elements
   * @param elements an array of elements
   * @return a stable list.
   * @throws NullPointerException if one element is null.
   */
  @SafeVarargs
  public static <T> StableList<T> list(T... elements) {
    return StableList.of(elements);
  }
  /**
   * Creates a List from a collection.
   * Each elements must be non null.
   * 
   * @param <T> type of the elements
   * @param collection a collection of elements
   * @return a stable list.
   * @throws NullPointerException if one element is null.
   */
  public static <T> StableList<T> list(Collection<? extends T> collection) {
    return StableList.from(collection);
  }
  /**
   * Creates a List from elements.
   * Each elements must be non null.
   * 
   * @param <T> type of the elements
   * @param stream a stream of elements
   * @return a stable list.
   * @throws NullPointerException if one element is null.
   */
  public static <T> StableList<T> list(Stream<? extends T> stream) {
    return stream.collect(StableList.toStableList());
  }
  
  /**
   * Concatenate an array of collections into a list.
   * 
   * @param <T> common super type of the elements in the collections 
   * @param collections an array of Collection
   * @return a new list that contains all the elements of the collections 
   */
  @SafeVarargs
  public static <T> List<T> flatten(Collection<? extends T>... collections) {
    return list(Arrays.stream(collections).flatMap(Collection::stream));
  }
  
  /**
   * Prints all elements pass as arguments separated by commas
   * 
   * @param elements the elements to print
   */
  public static void print(Object... elements) {
    System.out.println(Arrays.stream(elements).map(String::valueOf).collect(Collectors.joining(" ")));
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
  
  
  /**
   * A block of code.
   *
   * @param <X> the type of checked exception thrown by the lambda.
   * 
   * @see Pro#local(String, Action)
   * @see Pro#command(Action)
   */
  @FunctionalInterface
  public interface Action<X extends Throwable> {
    /**
     * Execute the block of code and possibly throw an exception.
     * 
     * @throws X an exception thrown.
     */
    void execute() throws X;
  }
  
  /**
   * Execute an action inside a local directory with a duplicated configuration.
   * All changes done to the configuration by the action will be done on the local configuration. 
   * 
   * @param <X> the type of the propagated exceptions
   * @param localDir a local directory
   * @param action an action
   * @throws X an exception that may be thrown
   */
  public static <X extends Throwable> void local(String localDir, Action<? extends X> action) throws X { 
    var oldConfig = CONFIG.get();
    var currentDir = oldConfig.getOrThrow("pro", ProConf.class).currentDir();
    var newConfig = oldConfig.duplicate();
    newConfig.getOrUpdate("pro", ProConf.class).currentDir(currentDir.resolve(localDir));
    
    CONFIG.set(newConfig);
    try {
      action.execute();
    } finally {
      CONFIG.set(oldConfig);
    }
  }
  
  /**
   * Create a new {@link Command} that will execute the action if the command is run.
   * All changes done to the configuration by the action will be done a new fresh local configuration. 
   * 
   * @param action the action to execute
   * @return a new command.
   * 
   * @deprecated use {@link #command(String, Action)} instead.
   */
  @Deprecated
  public static Command command(Action<? extends IOException> action) {
    var line = StackWalker.getInstance().walk(s -> s.findFirst()).map(StackFrame::getLineNumber).orElse(-1);
    return command("command at line " + line, action);
  }
  
  /**
   * Create a new {@link Command} that will execute the action if the command is run.
   * All changes done to the configuration by the action will be done a new fresh local configuration. 
   * 
   * @param name name of the command
   * @param action the action to execute
   * @return a new command.
   * 
   * @see #run(Object...)
   */
  public static Command command(String name, Action<? extends IOException> action) {
    return new Command() {
      @Override
      public String name() {
        return name;
      }
      @Override
      public int execute(Config unused) throws IOException {
        var oldConfig = CONFIG.get();
        CONFIG.set(oldConfig.duplicate());
        try {
          action.execute();
        } finally {
          CONFIG.set(oldConfig);
        }
        return 0; // FIXME
      }
    };
  }
  
  /**
   * Execute all commands, {@link #command(Action) user-defined} or {@link Plugin plugins},
   * one after another in the array order with the current configuration. 
   * 
   * @param commands an array of command to be executed
   * 
   * @see #run(List)
   * @see Command#execute(Config)
   */
  public static void run(Object... commands) {
    run(List.of(commands));
  }
  
  /**
   * Execute all commands, {@link #command(Action) user-defined} or {@link Plugin plugins},
   * one after another in the list order with the current configuration. 
   * 
   * @param commands a list of command to be executed
   * 
   * @see Command#execute(Config)
   */
  public static void run(List<?> commands) {
    var config = CONFIG.get();
    var proConf = config.getOrThrow("pro", ProConf.class);
    
    var commandList = new ArrayList<Command>();
    for(Object command: commands) {
      if (command instanceof Command) {
        commandList.add((Command)command);
      } else {
        var pluginName = (command instanceof Query)? ((Query)command)._id_(): command.toString();
        var pluginOpt = Optional.ofNullable(PLUGINS.get(pluginName));
        if (!pluginOpt.isPresent()) {
          var log = Log.create("pro", proConf.loglevel());
          log.error(pluginName, name -> "unknown plugin " + name);  
          throw exit(proConf.exitOnError(), "pro", 1);  //FIXME
        }
        
        commandList.add(pluginOpt.get());
      }
    }
    
    runAll(config, commandList);
  }
  
  private static void runAll(DefaultConfig config, List<Command> commands) {
    var commandConfig = config.duplicate();
    
    var daemonOpt = ServiceLoader.load(Daemon.class, ClassLoader.getSystemClassLoader()).findFirst();
    var consumer = daemonOpt
        .filter(Daemon::isStarted)
        .<BiConsumer<List<Command>, Config>>map(daemon -> daemon::execute)
        .orElse(Pro::executeAll);
    consumer.accept(commands, commandConfig);
  }
  
  private static void executeAll(List<Command> commands, Config config) {
    var proConf = config.getOrThrow("pro", ProConf.class);
    var exitOnError = proConf.exitOnError();
    
    var errorCode = 0;
    var failedCommandName = "";
    var start = System.currentTimeMillis();
    for(var command: commands) {
      errorCode = execute(command, config);
      if (errorCode != 0) {
        failedCommandName = command.name();
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
      throw exit(exitOnError, failedCommandName, errorCode);
    }
  }
  
  private static int execute(Command command, Config config) {
    try {
      return command.execute(config);
    } catch (IOException | /*UncheckedIOException |*/ RuntimeException e) {  //FIXME revisit RuntimeException !
      e.printStackTrace();
      var logLevel = config.getOrThrow("pro", ProConf.class).loglevel();
      var log = Log.create(command.name(), logLevel);
      log.error(e);
      return 1; // FIXME
    }
  }
  
  private static Error exit(boolean exitOnError, String failedCommandName, int errorCode) {
    String errorMessage = failedCommandName + " exit with code " + errorCode;
    if (exitOnError) {
      System.err.println(errorMessage);
      System.exit(errorCode);
    }
    throw new Error(errorMessage);
  }
}
