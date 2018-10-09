package com.github.forax.pro.plugin.runner;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

class Java {
  private final Path javaCommand;
  private final List<Path> modulePath;
  private final String moduleName;
  
  private boolean enablePreview;
  private List<Path> upgradeModulePath;
  private List<String> rootModules; 
  private List<String> rawArguments;
  private List<String> mainArguments;
  
  public Java(Path javaCommand, List<Path> modulePath, String moduleName) {
    this.javaCommand = Objects.requireNonNull(javaCommand);
    this.modulePath = Objects.requireNonNull(modulePath);
    this.moduleName = Objects.requireNonNull(moduleName);
  }
  
  public Path getJavaCommand() {
    return javaCommand;
  }
  public List<Path> modulePath() {
    return modulePath;
  }
  public String moduleName() {
    return moduleName;
  }
  
  public boolean enablePreview() {
    return enablePreview;
  }
  public void enablePreview(boolean enablePreview) {
    this.enablePreview = enablePreview;
  }
  public Optional<List<Path>> upgradeModulePath() {
    return Optional.ofNullable(upgradeModulePath);
  }
  public void upgradeModulePath(List<Path> upgradeModulePath) {
    this.upgradeModulePath = Objects.requireNonNull(upgradeModulePath);
  }
  
  public Optional<List<String>> rootModules() {
    return Optional.ofNullable(rootModules);
  }
  public void rootModules(List<String> rootModules) {
    this.rootModules = Objects.requireNonNull(rootModules);
  }
  
  public Optional<List<String>> rawArguments() {
    return Optional.ofNullable(rawArguments);
  }
  public void rawArguments(List<String> rawArguments) {
    this.rawArguments = Objects.requireNonNull(rawArguments);
  }
  
  public Optional<List<String>> mainArguments() {
    return Optional.ofNullable(mainArguments);
  }
  public void mainArguments(List<String> mainArguments) {
    this.mainArguments = Objects.requireNonNull(mainArguments);
  }
}
