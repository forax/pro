package com.github.forax.pro.plugin.runner;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface RunnerConf {
  Path javaCommand();
  void javaCommand(Path javaCommand);
  
  List<Path> modulePath();
  void modulePath(List<Path> modulePath);
  
  Optional<String> module();
  void module(String moduleName);
}
