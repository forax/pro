package com.github.forax.pro.main;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.forax.pro.daemon.Daemon;
import com.github.forax.pro.helper.secret.Secret;
import com.github.forax.pro.main.runner.Runner;

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
    RUN(Main::run),
    DAEMON(Main::daemon),
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
  
  private static Optional<Daemon> getDaemonService() {
    return ServiceLoader.load(Daemon.class).findFirst();
  }
  
  static void shell() {
    Secret.jShellTool_main();
    getDaemonService()
      .filter(Daemon::isStarted)
      .ifPresent(Daemon::stop);
  }
  
  static void run(String[] args) {
    Path configFile = InputFile.find(args)
        .orElseThrow(() -> new IllegalArgumentException("no existing input file specified"));
    
    ServiceLoader<Runner> loader = ServiceLoader.load(Runner.class, Runner.class.getClassLoader());
    ArrayList<Runner> runners = new ArrayList<>();
    loader.forEach(runners::add);
    
    runners.stream()
        .flatMap(runner -> runner.accept(configFile).stream())
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("no runner available for config file " + configFile))
        .run();
  }
  
  static void daemon(String[] args) {
    Daemon service =
        getDaemonService().orElseThrow(() -> new IllegalStateException("daemon service not found"));
    if (service.isStarted()) {
      throw new IllegalStateException("daemon service already started");
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
        "    shell              start the interactive shell                             \n" +
        "    daemon subcommand  start the subcommand in daemon mode                     \n" +
        "    help               this help                                               \n" +
        "                                                                               \n" +
        "  if no subcommand is specified, 'run' is used                                 \n"
    );
  }
  
  public static void main(String[] args) {
    Command command;
    String[] arguments;
    if (args.length == 0) {
      command = Command.RUN;
      arguments = args;
    } else {
      command = Command.command(args[0]);
      arguments = Arrays.stream(args).skip(1).toArray(String[]::new);
    }
    command.consumer.accept(arguments);
  }
}
