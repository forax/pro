package com.github.forax.pro.main;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.forax.pro.helper.Platform;
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
        Stream.of("-R-XX:+EnableValhalla").filter(__ -> System.getProperty("valhalla.enableValhalla") != null),
        Stream.of("-R-Dpro.exitOnError=false"),
        //FIXME JDK 13 bug due to 8212828
        Stream.of("-R-Djdk.lang.Process.launchMechanism=fork").filter(__ -> Platform.current() == Platform.UNIX),
        propertySeq.stream().map(entry -> "-D" + entry.getKey() + '=' + entry.getValue()),
        Stream.of(arguments).filter(a -> !a.isEmpty()).map(a -> "-R-Dpro.arguments=" + String.join(",", a)),
        Stream.of(configFile.toString())
        )
      .flatMap(s -> s)
      .toArray(String[]::new);
    
    int exitCode = JShellWrapper.run(System.in, System.out, System.err, args);
    if (exitCode != 0) {
      System.err.println("error while executing jshell " + String.join(" ", args));
    }
    System.exit(exitCode);
  }
}
