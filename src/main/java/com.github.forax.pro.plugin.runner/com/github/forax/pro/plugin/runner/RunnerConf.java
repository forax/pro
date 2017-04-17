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
  
  Optional<List<String>> rootModules();
  void rootModules(List<String> rootModules);
  
  Optional<List<String>> rawArguments();
  void rawArguments(List<String> rawArguments);
  
  Optional<List<String>> mainArguments();
  void mainArguments(List<String> mainArguments);
}
