package com.github.forax.pro.plugin.runner;

import java.nio.file.Path;
import java.util.List;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface ConventionFacade {
  Path javaHome();
  List<Path> javaModuleDependencyPath();
  Path javaModuleArtifactSourcePath();
  List<Path> javaModuleExplodedSourcePath();
}
