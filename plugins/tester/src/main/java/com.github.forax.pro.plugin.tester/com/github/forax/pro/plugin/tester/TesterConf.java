package com.github.forax.pro.plugin.tester;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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
  Path moduleReportTestPath();
  void moduleReportTestPath(Path moduleReportTestPath);
  
  // tester
  int timeout();
  void timeout(int seconds);
  boolean parallel();
  void parallel(boolean parallel);
  Optional<List<String>> includeTags();
  void includeTags(List<String> tags);
  Optional<List<String>> excludeTags();
  void excludeTags(List<String> tags);
  Optional<List<String>> packages();
  void packages(List<String> tags);
}
