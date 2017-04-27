package com.github.forax.pro.plugin.tester;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface TesterConf {
  Optional<List<String>> overrideArguments();
  void overrideArguments(List<String> overrideArguments);

  List<Path> moduleExplodedTestPath();
  void moduleExplodedTestPath(List<Path> moduleExplodedTestPath);
}
