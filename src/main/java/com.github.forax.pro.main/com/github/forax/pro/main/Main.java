package com.github.forax.pro.main;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;

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
  
  public static void main(String[] args) {
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
}
