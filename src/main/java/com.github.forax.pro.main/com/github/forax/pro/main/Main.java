package com.github.forax.pro.main;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.forax.pro.daemon.Daemon;
import com.github.forax.pro.main.runner.ConfigRunner;

public class Main {
  private Main() {
    throw new AssertionError();
  }
  
  enum InputFile {
    ARGUMENT(args -> (args.length == 1)? Optional.of(Paths.get(args[0])): Optional.empty()),
    DEFAULT_JSON(args -> Optional.of(Paths.get("build.json"))),
    DEFAULT_PRO(args -> Optional.of(Paths.get("build.pro")))
    ;
    
    private final Function<String[], Optional<Path>> mapper;

    private InputFile(Function<String[], Optional<Path>> mapper) {
      this.mapper = mapper;
    }
    
    static Optional<Path> find(String[] args) {
      return Arrays.stream(InputFile.values())
          .map(input -> input.mapper.apply(args))
          .map(p -> p.filter(Files::exists))
          .flatMap(Optional::stream)
          .findFirst();
    }
  }
  
  enum Command {
    SHELL(__ -> shell()),
    BUILD(Main::build),
    DAEMON(Main::daemon),
    SCAFFOLD(__ -> scaffold()), 
    HELP(__ -> help())
    ;
    
    final Consumer<String[]> consumer;

    private Command(Consumer<String[]> consumer) {
      this.consumer = consumer;
    }
    
    static Command command(String name) {
      return Arrays.stream(values())
          .filter(command -> command.name().toLowerCase().equals(name))
          .findFirst()
          .orElseThrow(() -> { throw new IllegalArgumentException("unknown sub command " + name); });
    }
  }
  
  static void scaffold() {
    List<String> lines = List.of(
        "{                                                                             ",
        "  resolver: {                                                                 ",
        "    dependencies: [                                                           ",          
        "//    \"org.junit=junit:junit:4.12\"                                          ",
        "    ]                                                                         ",
        "  },                                                                          ",
        "  packager: {                                                                 ",
        "    moduleMetadata: [                                                         ",
        "//    \"my.module@1.0/com.foo.my.module.Main\"                                ",
        "    ]                                                                         ",
        "  },                                                                          ",
        "//run: [\"resolver\", \"modulefixer\", \"compiler\", \"packager\", \"runner\"]",
        "  run: [\"compiler\", \"packager\"]                                           ",
        "}                                                                             "
      );
    
    try {
      Files.write(Paths.get("build.json"),
          (Iterable<String>)lines.stream().filter(l -> !l.startsWith("//"))::iterator,
          StandardOpenOption.CREATE_NEW);
      System.out.println(String.join("\n", lines));
      
      Files.createDirectories(Paths.get("src", "main", "java"));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
  
  private static Optional<Daemon> getDaemon() {
    return ServiceLoader.load(Daemon.class, ClassLoader.getSystemClassLoader()).findFirst();
  }
  
  static void shell() {
    if (getDaemon().filter(Daemon::isStarted).isPresent()) {
      throw new IllegalStateException("shell doesn't currently support daemon mode :(");
    }
    JShellWrapper.run(System.in, System.out, System.err);
  }
  
  static void build(String[] args) {
    Path configFile = InputFile.find(args)
        .orElseThrow(() -> new IllegalArgumentException("no existing input file specified"));
    
    ServiceLoader<ConfigRunner> loader = ServiceLoader.load(ConfigRunner.class, ConfigRunner.class.getClassLoader());
    ArrayList<ConfigRunner> configRunners = new ArrayList<>();
    loader.forEach(configRunners::add);
    
    configRunners.stream()
        .flatMap(configRunner -> configRunner.accept(configFile).stream())
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("no config runner available for config file " + configFile))
        .run();
  }
  
  static void daemon(String[] args) {
    Daemon service =
        getDaemon().orElseThrow(() -> new IllegalStateException("daemon not found"));
    if (service.isStarted()) {
      throw new IllegalStateException("daemon already started");
    }
    service.start();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      if (service.isStarted()) {
        service.stop();
      }
    }));
    main(args);
  }
  
  static void help() {
    System.err.println(
        "usage: pro [subcommand] args                                                   \n" +
        "                                                                               \n" +
        "  subcommands                                                                  \n" +
        "    build [buildfile]  execute the build file                                  \n" +
        "                       use build.json or build.pro if no buildfile is specified\n" +
        "    daemon subcommand  start the subcommand in daemon mode                     \n" +
        "    shell              start the interactive shell                             \n" +
        "    scaffold           create a default build.json                             \n" +
        "    help               this help                                               \n" +
        "                                                                               \n" +
        "  if no subcommand is specified, 'build' is used                               \n"
    );
  }
  
  public static void main(String[] args) {
    Command command;
    String[] arguments;
    if (args.length == 0) {
      command = Command.BUILD;
      arguments = args;
    } else {
      command = Command.command(args[0]);
      arguments = Arrays.stream(args).skip(1).toArray(String[]::new);
    }
    command.consumer.accept(arguments);
  }
}
