package com.github.forax.pro.api.helper;

import java.nio.file.Path;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface ProConf {
  Path currentDir();
  void currentDir(Path path);
  
  String loglevel();
  void loglevel(String loglevel);
  
  boolean exitOnError();
  void exitOnError(boolean exitOnError);
}
