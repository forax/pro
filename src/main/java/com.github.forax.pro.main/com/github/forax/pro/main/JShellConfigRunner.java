package com.github.forax.pro.main;

import java.nio.file.Path;
import java.util.Optional;

import com.github.forax.pro.main.runner.ConfigRunner;

public class JShellConfigRunner implements ConfigRunner {
  @Override
  public Optional<Runnable> accept(Path configFile) {
    return Optional.<Runnable>of(() -> run(configFile))
        .filter(__ -> configFile.toString().endsWith(".pro"));
  }
  
  private static void run(Path configFile) {
    //System.out.println("run with jshell " + configFile);
    
    JShellWrapper.run(System.in, System.out, System.err, "-R-Dpro.exitOnError=true", configFile.toString());
  }
}
