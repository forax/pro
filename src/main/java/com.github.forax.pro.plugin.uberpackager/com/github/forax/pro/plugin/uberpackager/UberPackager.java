package com.github.forax.pro.plugin.uberpackager;

import java.nio.file.Path;
import java.util.List;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface UberPackager {
  Path moduleArtifactSourcePath();
  void moduleArtifactSourcePath(Path destination);
  
  void moduleDependencyPath(List<Path> moduleDependencyPath);
  List<Path> moduleDependencyPath();
  
  Path moduleUberPath();
  void moduleUberPath(Path moduleUberPath);
  
  Path moduleUberExplodedPath();
  void moduleUberExplodedPath(Path moduleUberExplodedPath);
}
