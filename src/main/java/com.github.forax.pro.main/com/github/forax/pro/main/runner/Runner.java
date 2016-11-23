package com.github.forax.pro.main.runner;

import java.nio.file.Path;

public interface Runner {
  public boolean accept(Path config);
  
  public void run(Path configFile);
}
