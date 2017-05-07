package com.github.forax.pro.plugin.resolver;

import com.github.forax.pro.api.TypeCheckedConfig;
import java.nio.file.Path;
import java.util.List;

@TypeCheckedConfig
public interface ConventionFacade {
  public List<Path> javaModuleSourcePath();

  public List<Path> javaModuleTestPath();

  public List<Path> javaModuleDependencyPath();

  public Path javaMavenLocalRepositoryPath();
}
