package com.github.forax.pro.plugin.linker;

import com.github.forax.pro.api.TypeCheckedConfig;
import java.nio.file.Path;
import java.util.List;

@TypeCheckedConfig
public interface ConventionFacade {
  public Path javaHome();

  public Path javaLinkerImagePath();

  public List<Path> javaModuleDependencyPath();

  public Path javaModuleArtifactSourcePath();
}
