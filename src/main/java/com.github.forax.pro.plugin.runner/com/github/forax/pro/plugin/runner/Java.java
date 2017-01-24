package com.github.forax.pro.plugin.runner;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

class Java {
  private final Path javaCommand;
  private final List<Path> modulePath;
  private final String module;
  private List<String> rootModules; 
  
  public Java(Path javaCommand, List<Path> modulePath, String module) {
    this.javaCommand = Objects.requireNonNull(javaCommand);
    this.modulePath = Objects.requireNonNull(modulePath);
    this.module = Objects.requireNonNull(module);
  }
  
  public Path getJavaCommand() {
    return javaCommand;
  }
  public List<Path> modulePath() {
    return modulePath;
  }
  public String module() {
    return module;
  }
  
  public Optional<List<String>> rootModules() {
    return Optional.ofNullable(rootModules);
  }
  public void rootModules(List<String> rootModules) {
    this.rootModules = Objects.requireNonNull(rootModules);
  }
}
