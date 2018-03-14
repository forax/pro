package com.github.forax.pro.plugin.perfer;

import java.nio.file.Path;
import java.util.List;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface PerferConf {
  Path moduleArtifactTestPath();
  void moduleArtifactTestPath(Path moduleArtifactTestPath);
  
  List<Path> moduleDependencyPath();
  void moduleDependencyPath(List<Path> moduleDependencyPath);
  
  Path javaCommand();
  void javaCommand(Path javaCommand);
}
