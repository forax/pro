package com.github.forax.pro.plugin.compiler;

import com.github.forax.pro.api.TypeCheckedConfig;
import java.nio.file.Path;
import java.util.List;

@TypeCheckedConfig
public interface ConventionFacade {
  List<Path> javaModuleSourcePath();

  List<Path> javaModuleSourceResourcesPath();

  List<Path> javaModuleExplodedSourcePath();

  List<Path> javaModuleTestPath();

  List<Path> javaModuleTestResourcesPath();

  List<Path> javaModuleMergedTestPath();

  List<Path> javaModuleExplodedTestPath();

  List<Path> javaModuleDependencyPath();
}
