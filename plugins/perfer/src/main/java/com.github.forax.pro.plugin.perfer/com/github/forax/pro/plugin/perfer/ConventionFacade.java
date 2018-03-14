package com.github.forax.pro.plugin.perfer;

import java.nio.file.Path;
import java.util.List;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface ConventionFacade {
  Path javaModuleArtifactTestPath();
  List<Path> javaModuleDependencyPath();
  
  Path javaHome();
}
