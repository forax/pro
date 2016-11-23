package com.github.forax.pro.plugin.linker;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

class Jlink {
  private final Linker linker;
  private final List<String> rootModules;
  private final List<Path> modulePath;

  Jlink(Linker linker, List<String> rootModules, List<Path> modulePath) {
    this.linker = Objects.requireNonNull(linker);
    this.rootModules = Objects.requireNonNull(rootModules);
    this.modulePath = Objects.requireNonNull(modulePath);
  }
  
  public List<String> rootModules() {
    return rootModules;
  }
  public List<Path> modulePath() {
    return modulePath;
  }
  
  public Path destination() {
    return linker.destination();
  }
  public int compressLevel() {
    return linker.compressLevel();
  }
  public boolean stripDebug() {
    return linker.stripDebug();
  }
  public boolean stripNativeCommands() {
    return linker.stripNativeCommands();
  }
}
