package com.github.forax.pro.main.runner;

import java.nio.file.Path;
import java.util.Optional;

public interface Runner {
  public Optional<Runnable> accept(Path configFile);
}
