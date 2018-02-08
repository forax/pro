package com.github.forax.pro.plugin.docer;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

class Javadoc {
  private final Path destination;
  private final List<Path> moduleSourcePath;
  
  private List<Path> modulePath;
  private List<Path> upgradeModulePath;
  private List<String> rootModules;
  private List<String> rawArguments;
  
  Javadoc(Path destination, List<Path> moduleSourcePath) {
    this.destination = destination;
    this.moduleSourcePath = moduleSourcePath;
  }
  
  public Path destination() {
    return destination;
  }
  public List<Path> moduleSourcePath() {
    return moduleSourcePath;
  }
  
  public Optional<List<String>> rawArguments() {
    return Optional.ofNullable(rawArguments);
  }
  public void rawArguments(List<String> rawArguments) {
    this.rawArguments = Objects.requireNonNull(rawArguments);
  }
  
  public Optional<List<Path>> upgradeModulePath() {
    return Optional.ofNullable(upgradeModulePath);
  }
  public void upgradeModulePath(List<Path> modulePath) {
    this.upgradeModulePath = Objects.requireNonNull(modulePath);
  }
  
  public Optional<List<Path>> modulePath() {
    return Optional.ofNullable(modulePath);
  }
  public void modulePath(List<Path> modulePath) {
    this.modulePath = Objects.requireNonNull(modulePath);
  }
  
  public Optional<List<String>> rootModules() {
    return Optional.ofNullable(rootModules);
  }
  public void rootModules(List<String> rootModules) {
    this.rootModules = Objects.requireNonNull(rootModules);
  }
}
