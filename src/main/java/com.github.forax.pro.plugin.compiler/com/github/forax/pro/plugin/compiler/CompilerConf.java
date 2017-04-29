package com.github.forax.pro.plugin.compiler;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface CompilerConf {
  int release();
  void release(int release);
  
  Optional<Boolean> verbose();
  void verbose(boolean verbose);
  
  Optional<String> lint();
  void lint(String lintOptions);
  
  Optional<List<String>> rawArguments();
  void rawArguments(List<String> rawArguments);
  
  List<Path> moduleSourcePath();
  void moduleSourcePath(List<Path> modulePath);
  List<Path> moduleSourceResourcesPath();
  void moduleSourceResourcesPath(List<Path> modulePath);
  Path moduleExplodedSourcePath();
  void moduleExplodedSourcePath(Path destination);
  
  List<Path> moduleTestPath();
  void moduleTestPath(List<Path> modulePath);
  List<Path> moduleTestResourcesPath();
  void moduleTestResourcesPath(List<Path> modulePath);
  Path moduleMergedTestPath();
  void moduleMergedTestPath(Path modulePath);
  Path moduleExplodedTestPath();
  void moduleExplodedTestPath(Path destination);
  
  public Optional<List<Path>> upgradeModulePath();
  public void upgradeModulePath(List<Path> modulePath);
  
  Optional<List<Path>> modulePath();
  void modulePath(List<Path> modulePath);
  
  List<Path> moduleDependencyPath();
  void moduleDependencyPath(List<Path> path);
  
  Optional<List<String>> rootModules();
  void rootModules(List<String> rootModules);
  
  Optional<List<Path>> files();
  void files(List<Path> files);
}
