package com.github.forax.pro.plugin.modulefixer;

import com.github.forax.pro.api.TypeCheckedConfig;
import java.nio.file.Path;
import java.util.List;

@TypeCheckedConfig
public interface ConventionFacade {
  public List<Path> javaModuleDependencyPath();

  Path javaModuleDependencyFixerPath();
}
