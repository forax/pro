package com.github.forax.pro.plugin.formatter;

import java.nio.file.Path;
import java.util.List;

import com.github.forax.pro.api.TypeCheckedConfig;
import java.util.Optional;

@TypeCheckedConfig
public interface FormatterConf {

  // Derived from convention

  List<Path> moduleSourcePath();
  void moduleSourcePath(List<Path> modulePath);

  List<Path> moduleTestPath();
  void moduleTestPath(List<Path> modulePath);

  // Formatter options

  boolean replace();
  void replace(boolean replace);
  
  boolean dryRun();
  void dryRun(boolean dryRun);
  
  boolean setExitIfChanged();
  void setExitIfChanged(boolean setExitIfChanged);
  
  Optional<List<String>> rawArguments();
  void rawArguments(List<String> rawArguments);

  Optional<List<Path>> files();
  void files(List<Path> files);
}
