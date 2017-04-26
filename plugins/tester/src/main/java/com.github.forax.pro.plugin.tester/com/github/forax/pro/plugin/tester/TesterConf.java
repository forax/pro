package com.github.forax.pro.plugin.tester;

import java.nio.file.Path;
import java.util.List;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface TesterConf {
  List<Path> moduleExplodedTestPath();
  void moduleExplodedTestPath(List<Path> moduleExplodedTestPath);
}
