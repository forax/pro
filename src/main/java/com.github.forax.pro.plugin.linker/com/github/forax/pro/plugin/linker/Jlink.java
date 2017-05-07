package com.github.forax.pro.plugin.linker;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

class Jlink {
  private final LinkerConf linker;
  private final Set<String> rootModules;
  private final List<String> launchers;
  private final List<Path> modulePath;

  Jlink(LinkerConf linker, Set<String> rootModules, List<String> launchers, List<Path> modulePath) {
    this.linker = Objects.requireNonNull(linker);
    this.rootModules = Objects.requireNonNull(rootModules);
    this.launchers = Objects.requireNonNull(launchers);
    this.modulePath = Objects.requireNonNull(modulePath);
  }
  
  public Set<String> rootModules() {
    return rootModules;
  }
  public List<String> launchers() {
    return launchers;
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
  public boolean ignoreSigningInformation() {
    return linker.ignoreSigningInformation();
  }
  public Optional<List<String>> rawArguments() {
    return linker.rawArguments();
  }
}
