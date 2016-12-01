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
    SHELL(__ -> Secret.jShellTool_main()),
    RUN(args -> run(args)),
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
  
  static void run(String[] args) {
    Path configFile = InputFile.find(args)
        .orElseThrow(() -> new IllegalArgumentException("no existing input file specified"));
    
    ServiceLoader<Runner> loader = ServiceLoader.load(Runner.class, Runner.class.getClassLoader());
    ArrayList<Runner> runners = new ArrayList<>();
    loader.forEach(runners::add);
    
    runners.stream()
        .filter(runner -> runner.accept(configFile))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("no runner available for config file " + configFile))
        .run(configFile);
  }
  
  static void help() {
    System.err.println(
        "usage: pro [subcommand] args                                                 \n" +
        "                                                                             \n" +
        "  subcommands                                                                \n" +
        "    run [buildfile]  run the build file                                      \n" +
        "                     use build.json or build.pro if no buildfile is specified\n" +
        "    shell            start the interactive shell                             \n" +
        "    help             this help                                               \n" +
        "                                                                             \n" +
        "  if no subcommand is specified, 'run' is used                               \n"
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
