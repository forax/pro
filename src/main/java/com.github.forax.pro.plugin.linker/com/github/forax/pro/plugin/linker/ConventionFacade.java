package com.github.forax.pro.plugin.linker;

import java.nio.file.Path;
import java.util.List;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface ConventionFacade {
  public Path javaHome();
  
  public Path javaLinkerImagePath();
  
  public List<Path> javaModuleDependencyPath();
  
  public Path javaModuleArtifactSourcePath();
}
