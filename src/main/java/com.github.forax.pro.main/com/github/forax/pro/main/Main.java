package com.github.forax.pro.main;

import static com.github.forax.pro.main.runner.PropertySequence.property;
import static java.util.function.Function.identity;

import java.io.IOException;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.github.forax.pro.daemon.Daemon;
import com.github.forax.pro.main.runner.ConfigRunner;
import com.github.forax.pro.main.runner.PropertySequence;

public class Main {
  private Main() {
    throw new AssertionError();
  }
  
  static class InputException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    InputException(String message) {
      super(message);
    }
    InputException(Throwable cause) {
      super(cause);
    }
  }
  
  static class Configuration {
    final Path configFile;
    final Function<List<String>, List<String>> arguments;
    
    Configuration(Path configFile, Function<List<String>, List<String>> arguments) {
      this.configFile = configFile;
      this.arguments = arguments;
    }
  }
  
  enum InputFile {
    ARGUMENT(args -> (args.size() >= 1)? Optional.of(new Configuration(Paths.get(args.get(0)), Main::shift)): Optional.empty()),
    DEFAULT_JAVA(args -> Optional.of(new Configuration(Paths.get("build.java"), identity()))),
    DEFAULT_PRO(args -> Optional.of(new Configuration(Paths.get("build.pro"), identity()))),
    DEFAULT_JSON(args -> Optional.of(new Configuration(Paths.get("build.json"), identity())))
    ;
    
    private final Function<List<String>, Optional<Configuration>> mapper;

    private InputFile(Function<List<String>, Optional<Configuration>> mapper) {
      this.mapper = mapper;
    }
    
    static Optional<Configuration> findConfiguration(List<String> args) {
      return Arrays.stream(InputFile.values())
          .flatMap(inputFile -> inputFile.mapper.apply(args).filter(conf -> Files.exists(conf.configFile)).stream())
          .findFirst();
    }
  }
  
  enum Option {
    SHELL(Main::shell),
    BUILD(Main::build),
    COMMAND(Main::command),
    DAEMON(Main::daemon),
    RESOLVE((__, args) -> resolve(args)),
    SCAFFOLD((_1, _2) -> Scaffold.scaffold()),
    HELP((_1, _2) -> help()),
    VERSION((_1, _2) -> version())
    ;
    
    final BiConsumer<PropertySequence, List<String>> consumer;

    private Option(BiConsumer<PropertySequence, List<String>> consumer) {
      this.consumer = consumer;
    }
    
    static Option option(String name) {
      return Arrays.stream(values())
          .filter(option -> option.name().toLowerCase().equals(name))
          .findFirst()
          .orElseThrow(() -> { throw new InputException("unknown option '" + name + "'"); });
    }
  }
  
  static void version() {
    System.out.println(
        "pro " + Main.class.getModule().getDescriptor().version().map(Version::toString).orElse("unknown") +
        " / jdk " + Runtime.version());
  }
  
  private static Optional<Daemon> getDaemon() {
    return ServiceLoader.load(Daemon.class, ClassLoader.getSystemClassLoader()).findFirst();
  }
  
  static void shell(PropertySequence propertySeq, List<String> arguments) {
    if (getDaemon().filter(Daemon::isStarted).isPresent()) {
      throw new InputException("shell doesn't currently support daemon mode :(");
    }
    var isAtLeastJava11 = Runtime.version().feature() >= 11;
    var args =
        Stream.of(
          Stream.of("-R--enable-preview").filter(__ -> isAtLeastJava11),  // always enable preview features if Java 11
          Stream.of("--enable-preview").filter(__ -> isAtLeastJava11),
          Stream.of("-R-XX:+EnableValhalla").filter(__ -> System.getProperty("valhalla.enableValhalla") != null),
          Stream.of("-R-Dpro.exitOnError=false"),
          propertySeq.stream().map(entry -> "-D" + entry.getKey() + '=' + entry.getValue()),
          Stream.of(arguments).filter(a -> !a.isEmpty()).map(a -> "-R-Dpro.arguments=" + String.join(",", a))
          )
        .flatMap(s -> s)
        .toArray(String[]::new);
    JShellWrapper.run(System.in, System.out, System.err, args);
  }
  
  static void build(PropertySequence propertySeq, List<String> args) {
    var conf = InputFile.findConfiguration(args)
        .orElseThrow(() -> new InputException("no existing input file specified"));
    var configFile = conf.configFile;
    var arguments = conf.arguments.apply(args);
    
    var loader = ServiceLoader.load(ConfigRunner.class, ConfigRunner.class.getClassLoader());
    var configRunners = new ArrayList<ConfigRunner>();
    loader.forEach(configRunners::add);
    
    configRunners.stream()
        .flatMap(configRunner -> configRunner.accept(configFile, propertySeq, arguments).stream())
        .findFirst()
        .orElseThrow(() -> new InputException("no config runner available for config file " + configFile))
        .run();
  }
  
  static void command(PropertySequence propertySeq, List<String> args) {
    if (args.isEmpty()) {
      throw new InputException("no command specified, the option 'command' takes a command as argument");
    }
    execute(propertySeq.append(property("pro.commands", args.get(0))), shift(args));
  }
  
  static void daemon(PropertySequence propertySeq, List<String> args) {
    var daemon = getDaemon().orElseThrow(() -> new InputException("daemon not found"));
    if (daemon.isStarted()) {
      throw new InputException("daemon already started");
    }
    daemon.start();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      if (daemon.isStarted()) {
        daemon.stop();
      }
    }));
    execute(propertySeq, args);
  }
  
  static void resolve(List<String> args) {
    if (args.isEmpty()) {
      throw new InputException("no Maven id specified");
    }
    try {
      for(var arg: args) {
        MavenArtifactResolver.resolve(arg);
      }
    } catch (IOException e) {
      throw new InputException(e);
    }
  }
  
  static void help() {
    System.err.println(
        "usage: pro [option] args                                                           \n" +
        "                                                                                   \n" +
        "  option                                                                           \n" +
        "    build [buildfile]      execute the build file                                  \n" +
        "                           use build.json or build.pro if no buildfile is specified\n" +
        "    command acommand       ask to execute the build until that command             \n" +
        "    daemon option          start the option in daemon mode                         \n" +
        "    resolve maven:id:[0,]  display Java module name and its dependencies           \n" +
        "    shell                  start the interactive shell                             \n" +
        "    scaffold               create a default build.pro                              \n" +
        "    version                print the current version                               \n" +
        "    help                   this help                                               \n" +
        "                                                                                   \n" +
        "  if no option is specified, 'build' is used                                       \n"
    );
  }
  
  static List<String> shift(List<String> args) {
    return args.subList(1, args.size());
  }
  
  private static void execute(PropertySequence propertySeq, List<String> args) {
    Option command;
    List<String> arguments;
    if (args.isEmpty()) {
      command = Option.BUILD;
      arguments = args;
    } else {
      command = Option.option(args.get(0));
      arguments = shift(args);
    }
    command.consumer.accept(propertySeq, arguments);
  }
  
  public static void main(String[] args) {
    try {
      execute(PropertySequence.empty(), List.of(args));
    } catch(InputException e) {
      System.err.println("error: " + e.getMessage() + "\n");
      help();
      System.exit(1);
    }
  }
}
