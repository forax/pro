package com.github.forax.pro.plugin.packager;

import com.github.forax.pro.api.TypeCheckedConfig;
import java.nio.file.Path;
import java.util.List;

@TypeCheckedConfig
public interface ConventionFacade {
  public List<Path> javaModuleExplodedSourcePath();

  public List<Path> javaModuleExplodedTestPath();

  public Path javaModuleArtifactSourcePath();

  public Path javaModuleArtifactTestPath();
}
