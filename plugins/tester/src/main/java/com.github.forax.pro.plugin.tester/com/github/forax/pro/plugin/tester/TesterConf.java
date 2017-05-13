package com.github.forax.pro.plugin.tester;

import java.nio.file.Path;
import java.util.List;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface TesterConf {
  // pro
  Path pluginDir();
  void pluginDir(Path pluginDir);

  // convention
  List<Path> moduleExplodedTestPath();
  void moduleExplodedTestPath(List<Path> moduleExplodedTestPath);
  List<Path> moduleDependencyPath();
  void moduleDependencyPath(List<Path> moduleDependencyPath);

  // tester
  int timeout();
  void timeout(int seconds);
}
