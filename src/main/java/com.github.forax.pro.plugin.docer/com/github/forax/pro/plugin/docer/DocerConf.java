package com.github.forax.pro.plugin.docer;


import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface DocerConf {
  boolean generateTestDoc();
  void generateTestDoc(boolean generate);
  
  boolean quiet();
  void quiet(boolean quiet);
  
  boolean html5();
  void html5(boolean enable);
  
  Optional<List<String>> rawArguments();
  void rawArguments(List<String> rawArguments);
  
  List<Path> moduleSourcePath();
  void moduleSourcePath(List<Path> modulePath);
  Path moduleDocSourcePath();
  void moduleDocSourcePath(Path docPath);
  
  List<Path> moduleMergedTestPath();
  void moduleMergedTestPath(List<Path> modulePath);
  Path moduleDocTestPath();
  void moduleDocTestPath(Path docPath);
  
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
