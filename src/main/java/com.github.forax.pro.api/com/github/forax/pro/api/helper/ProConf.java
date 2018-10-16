package com.github.forax.pro.api.helper;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface ProConf {
  Path currentDir();
  void currentDir(Path path);
  
  Path pluginDir();
  void pluginDir(Path path);
  
  String loglevel();
  void loglevel(String loglevel);
  
  boolean exitOnError();
  void exitOnError(boolean exitOnError);
  
  int errorCode();
  void errorCode(int errorCode);
  
  List<String> arguments();
  void arguments(List<String> arguments);
  
  Optional<List<String>> commands();
  void commands(List<String> commands);
}
