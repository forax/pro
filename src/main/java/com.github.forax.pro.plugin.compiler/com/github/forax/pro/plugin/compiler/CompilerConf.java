package com.github.forax.pro.plugin.compiler;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface CompilerConf {
  int release();
  void release(int release);
  
  Optional<Boolean> verbose();
  void verbose(boolean verbose);
  
  List<Path> moduleSourcePath();
  void moduleSourcePath(List<Path> modulePath);
  Path moduleExplodedSourcePath();
  void moduleExplodedSourcePath(Path destination);
  
  List<Path> moduleTestPath();
  void moduleTestPath(List<Path> modulePath);
  Path moduleMergedTestPath();
  void moduleMergedTestPath(Path modulePath);
  Path moduleExplodedTestPath();
  void moduleExplodedTestPath(Path destination);
  
  Optional<List<Path>> modulePath();
  void modulePath(List<Path> modulePath);
  
  public List<Path> moduleDependencyPath();
  public void moduleDependencyPath(List<Path> path);
  
  Optional<List<Path>> files();
  void files(List<Path> files);
}
