package com.github.forax.pro.plugin.packager;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

class Jar {
  private final Path input;
  private final Path output;
  private String moduleVersion;
  private String mainClass;
  private List<String> rawArguments;

  Jar(Path input, Path output) {
    this.input = input;
    this.output = output;
  }

  public Path getInput() {
    return input;
  }

  public Path getOutput() {
    return output;
  }

  public Optional<String> getModuleVersion() {
    return Optional.ofNullable(moduleVersion);
  }

  public void setModuleVersion(String moduleVersion) {
    this.moduleVersion = Objects.requireNonNull(moduleVersion);
  }

  public Optional<String> getMainClass() {
    return Optional.ofNullable(mainClass);
  }

  public void setMainClass(String mainClass) {
    this.mainClass = Objects.requireNonNull(mainClass);
  }

  public Optional<List<String>> rawArguments() {
    return Optional.ofNullable(rawArguments);
  }

  public void rawArguments(List<String> rawArguments) {
    this.rawArguments = Objects.requireNonNull(rawArguments);
  }
}
