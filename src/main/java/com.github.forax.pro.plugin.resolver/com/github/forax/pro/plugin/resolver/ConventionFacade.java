package com.github.forax.pro.plugin.resolver;

import java.nio.file.Path;
import java.util.List;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface ConventionFacade {
  public List<Path> javaModuleSourcePath();
  
  public List<Path> javaModuleTestPath();
  
  public List<Path> javaModuleDependencyPath();
  
  public Path javaMavenLocalRepositoryPath();
}
