package com.github.forax.pro.main;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.forax.pro.helper.Platform;
import com.github.forax.pro.main.runner.ConfigRunner;
import com.github.forax.pro.main.runner.PropertySequence;

public class JavaConfigRunner implements ConfigRunner {
  @Override
  public Optional<Runnable> accept(Path configFile, PropertySequence propertySeq, List<String> arguments) {
    return Optional.<Runnable>of(() -> run(configFile, propertySeq, arguments))
        .filter(__ -> configFile.toString().endsWith(".java"));
  }
  
  private static void run(Path configFile, PropertySequence propertySeq, List<String> arguments) {
    //System.out.println("run with java " + configFile);
    
    var javaHome = Path.of(System.getProperty("java.home"));
    var args =
        Stream.of(
          Stream.of(javaHome.resolve("bin").resolve(Platform.current().javaExecutableName()).toString()),
          Stream.of("-XX:+EnableValhalla").filter(__ -> System.getProperty("valhalla.enableValhalla") != null),
          //FIXME JDK 13 bug due to 8212828
          Stream.of("-Djdk.lang.Process.launchMechanism=fork").filter(__ -> Platform.current() == Platform.UNIX),
          Stream.of("-Dpro.exitOnError=true"),
          propertySeq.stream().map(entry -> "-D" + entry.getKey() + '=' + entry.getValue()),
          Stream.of(arguments).filter(a -> !a.isEmpty()).map(a -> "-Dpro.arguments=" + String.join(",", a)),
          Stream.of(configFile.toString())
          )
        .flatMap(s -> s)
        .toArray(String[]::new);
    
    //System.out.println("cmd " + String.join(" ", args));
    
    var exitCode = 1;
    try {
      var process = new ProcessBuilder(args)
          .redirectErrorStream(true)
          .start();
    
      process.getInputStream().transferTo(System.out);
      
      exitCode = process.waitFor();
    } catch (InterruptedException|IOException e) {
      System.err.println("i/o error " + e.getMessage() + "\n command " + String.join(" ", args));
    }
    System.exit(exitCode);
  }
}
