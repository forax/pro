package com.github.forax.pro.plugin.packager;

import java.nio.file.Path;
import java.util.List;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface ConventionFacade {
  public List<Path> javaModuleExplodedSourcePath();
  public List<Path> javaModuleExplodedTestPath();
  
  public Path javaModuleArtifactSourcePath();
  public Path javaModuleArtifactTestPath();
}
