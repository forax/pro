package com.github.forax.pro.plugin.uberpackager;

import java.nio.file.Path;
import java.util.List;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface ConventionFacade {
  Path javaModuleArtifactSourcePath();
  List<Path> javaModuleDependencyPath();
  
  Path javaModuleUberPath();
  Path javaModuleUberExplodedPath();
}
