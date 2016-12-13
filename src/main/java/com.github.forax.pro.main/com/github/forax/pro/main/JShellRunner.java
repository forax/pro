package com.github.forax.pro.main;

import java.nio.file.Path;
import java.util.Optional;

import com.github.forax.pro.helper.secret.Secret;
import com.github.forax.pro.main.runner.Runner;

public class JShellRunner implements Runner {
  @Override
  public Optional<Runnable> accept(Path configFile) {
    return Optional.<Runnable>of(() -> run(configFile))
        .filter(__ -> configFile.toString().endsWith(".pro"));
  }
  
  private static void run(Path configFile) {
    //System.out.println("run with jshell " + configFile);
    
    Secret.jShellTool_main(configFile.toString());  
  }
}
