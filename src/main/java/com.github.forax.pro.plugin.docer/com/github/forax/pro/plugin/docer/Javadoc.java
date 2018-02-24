package com.github.forax.pro.plugin.docer;

import java.net.URI;
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
  private boolean quiet;
  private boolean html5;
  private URI link;
  
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
  
  public boolean quiet() {
    return quiet;
  }
  public void quiet(boolean quiet) {
    this.quiet = quiet;
  }
  public boolean html5() {
    return html5;
  }
  public void html5(boolean enable) {
    this.html5 = enable;
  }
  public Optional<URI> link() {
    return Optional.ofNullable(link);
  }
  public void link(URI link) {
    this.link = Objects.requireNonNull(link);
  }
}
