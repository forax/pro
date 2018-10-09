package com.github.forax.pro.main;

import static java.lang.Runtime.version;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.forax.pro.main.runner.ConfigRunner;
import com.github.forax.pro.main.runner.PropertySequence;

public class JShellConfigRunner implements ConfigRunner {
  @Override
  public Optional<Runnable> accept(Path configFile, PropertySequence propertySeq, List<String> arguments) {
    return Optional.<Runnable>of(() -> run(configFile, propertySeq, arguments))
        .filter(__ -> configFile.toString().endsWith(".pro"));
  }
  
  private static void run(Path configFile, PropertySequence propertySeq, List<String> arguments) {
    //System.out.println("run with jshell " + configFile);
    
    var args =
      Stream.of(
        Stream.of("-R--enable-preview").filter(__ -> version().feature() >= 11),  // always enable preview features if Java 11
        Stream.of("-R-XX:+EnableValhalla").filter(__ -> System.getProperty("valhalla.enableValhalla") != null),
        Stream.of("-R-Dpro.exitOnError=false"),
        propertySeq.stream().map(entry -> "-D" + entry.getKey() + '=' + entry.getValue()),
        Stream.of(arguments).filter(a -> !a.isEmpty()).map(a -> "-R-Dpro.arguments=" + String.join(",", a)),
        Stream.of(configFile.toString())
        )
      .flatMap(s -> s)
      .toArray(String[]::new);
    
    int exitCode = JShellWrapper.run(System.in, System.out, System.err, args);
    System.exit(exitCode);
  }
}
