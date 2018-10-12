package com.github.forax.pro.plugin.frozer;

import java.nio.file.Path;
import java.util.List;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface ConventionFacade {
  List<Path> javaModuleDependencyPath();
  Path javaModuleArtifactSourcePath();
  List<Path> javaModuleExplodedSourcePath();
  Path javaModuleFrozenArtifactSourcePath();
}
