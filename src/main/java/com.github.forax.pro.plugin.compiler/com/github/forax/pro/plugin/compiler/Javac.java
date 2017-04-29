package com.github.forax.pro.plugin.compiler;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

class Javac {
  private final int release;
  private final Path destination;
  private final List<Path> moduleSourcePath;
  
  private List<Path> modulePath;
  private List<Path> upgradeModulePath;
  private List<String> rootModules;
  private boolean verbose; 
  private String lint;
  private List<String> rawArguments;
  
  Javac(int release, Path destination, List<Path> moduleSourcePath) {
    this.release = release;
    this.destination = destination;
    this.moduleSourcePath = moduleSourcePath;
  }
  
  public int release() {
    return release;
  }
  public Path destination() {
    return destination;
  }
  public List<Path> moduleSourcePath() {
    return moduleSourcePath;
  }
  
  public boolean verbose() {
    return verbose;
  }
  public void verbose(boolean verbose) {
    this.verbose = verbose;
  }
  
  public Optional<String> lint() {
    return Optional.ofNullable(lint);
  }
  public void lint(String lint) {
    this.lint = Objects.requireNonNull(lint);
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
