package com.github.forax.pro.plugin.compiler;

import java.nio.file.Path;
import java.util.List;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface ConventionFacade {
  public List<Path> javaModuleSourcePath();
  public List<Path> javaModuleExplodedSourcePath();
  
  public List<Path> javaModuleTestPath();
  public List<Path> javaModuleMergedTestPath();
  public List<Path> javaModuleExplodedTestPath();
  
  public List<Path> javaModuleDependencyPath();
}
