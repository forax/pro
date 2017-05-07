package com.github.forax.pro.plugin.uberpackager;

import com.github.forax.pro.api.TypeCheckedConfig;
import java.nio.file.Path;
import java.util.List;

@TypeCheckedConfig
public interface ConventionFacade {
  Path javaModuleArtifactSourcePath();

  List<Path> javaModuleDependencyPath();

  Path javaModuleUberPath();

  Path javaModuleUberExplodedPath();
}
