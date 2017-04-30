package com.github.forax.pro.plugin.modulefixer;

import java.nio.file.Path;
import java.util.List;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface ConventionFacade {
  public List<Path> javaModuleDependencyPath();
  Path javaModuleDependencyFixerPath();
}
