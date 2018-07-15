package com.github.forax.pro.main;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.forax.pro.helper.Platform;
import com.github.forax.pro.main.runner.ConfigRunner;

public class JavaConfigRunner implements ConfigRunner {
  @Override
  public Optional<Runnable> accept(Path configFile, String[] arguments) {
    return Optional.<Runnable>of(() -> run(configFile, arguments))
        .filter(__ -> configFile.toString().endsWith(".java"));
  }
  
  private static void run(Path configFile, String... arguments) {
    //System.out.println("run with java " + configFile);
    
    Path javaHome = Paths.get(System.getProperty("java.home"));
    var args =
        Stream.of(
          Stream.of(javaHome.resolve("bin").resolve(Platform.current().javaExecutableName()).toString()),
          Stream.of("-Dpro.exitOnError=true"),
          Stream.of(arguments).filter(a -> a.length() != 0).map(a -> "-Dpro.arguments=" + String.join(",", a)),
          Stream.of(configFile.toString())
          )
        .flatMap(s -> s)
        .toArray(String[]::new);
    
    //System.out.println("cmd " + java.util.Arrays.toString(args));
    
    var exitCode = 1;
    try {
      var process = new ProcessBuilder(args)
          .redirectErrorStream(true)
          .start();
    
      process.getInputStream().transferTo(System.out);
      
      exitCode = process.waitFor();
    } catch (InterruptedException|IOException e) {
      System.err.println("i/o error " + e.getMessage());
    }
    System.exit(exitCode);
  }
}
