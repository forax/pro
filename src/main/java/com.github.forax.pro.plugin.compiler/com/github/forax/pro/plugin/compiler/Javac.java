package com.github.forax.pro.plugin.compiler;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

class Javac {
  private Integer release;
  private Path destination;
  private List<Path> moduleSourcePath;
  private List<Path> sourcePath;
  private List<Path> modulePath;
  private List<Path> classPath;
  private List<Path> upgradeModulePath;
  private List<Path> processorModulePath;
  private List<Path> processorPath;
  private String module;
  private List<String> rootModules;
  private boolean verbose; 
  private String lint;
  private boolean enablePreview;
  private List<String> rawArguments;
  
  Javac() {
    // empty
  }
  
  public void destination(Path destination) {
    this.destination = Objects.requireNonNull(destination);
  }
  public Optional<Path> destination() {
    return Optional.ofNullable(destination);
  }
  
  public void release(int release) {
    this.release = release;
  }
  public Optional<Integer> release() {
    return Optional.ofNullable(release);
  }
  
  public Optional<List<Path>> moduleSourcePath() {
    return Optional.ofNullable(moduleSourcePath);
  }
  public void moduleSourcePath(List<Path> moduleSourcePath) {
    this.moduleSourcePath = Objects.requireNonNull(moduleSourcePath);
  }
  
  public Optional<List<Path>> sourcePath() {
    return Optional.ofNullable(sourcePath);
  }
  public void sourcePath(List<Path> sourcePath) {
    this.sourcePath = Objects.requireNonNull(sourcePath);
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
  
  public boolean enablePreview() {
    return enablePreview;
  }
  public void enablePreview(boolean enablePreview) {
    this.enablePreview = enablePreview;
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
  public void upgradeModulePath(List<Path> upgradeModulePath) {
    this.upgradeModulePath = Objects.requireNonNull(upgradeModulePath);
  }
  
  public Optional<List<Path>> processorModulePath() {
    return Optional.ofNullable(processorModulePath);
  }
  public void processorModulePath(List<Path> processorModulePath) {
    this.processorModulePath = Objects.requireNonNull(processorModulePath);
  }
  public Optional<List<Path>> processorPath() {
    return Optional.ofNullable(processorPath);
  }
  public void processorPath(List<Path> processorPath) {
    this.processorPath = Objects.requireNonNull(processorPath);
  }
  
  public Optional<List<Path>> modulePath() {
    return Optional.ofNullable(modulePath);
  }
  public void modulePath(List<Path> modulePath) {
    this.modulePath = Objects.requireNonNull(modulePath);
  }
  
  public Optional<List<Path>> classPath() {
    return Optional.ofNullable(classPath);
  }
  /**
   * Set the class path or remove it if set as null.
   * @param classPath a class path or null
   */
  public void classPath(List<Path> classPath) {
    this.classPath = classPath;
  }
  public Optional<String> module() {
    return Optional.ofNullable(module);
  }
  public void module(String module) {
    this.module = Objects.requireNonNull(module);
  }
  public Optional<List<String>> rootModules() {
    return Optional.ofNullable(rootModules);
  }
  public void rootModules(List<String> rootModules) {
    this.rootModules = Objects.requireNonNull(rootModules);
  }
}
