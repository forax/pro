package com.github.forax.pro.main;

import java.nio.file.Path;

import com.github.forax.pro.helper.secret.Secret;
import com.github.forax.pro.main.runner.Runner;

public class JShellRunner implements Runner {
  @Override
  public boolean accept(Path config) {
    return config.toString().endsWith(".pro");
  }
  
  @Override
  public void run(Path configFile) {
    //System.out.println("run with jshell " + configFile);
    
    Secret.jShellTool_main(configFile.toString());
  }
}
