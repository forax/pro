package com.github.forax.pro.plugin.packager;

import java.nio.file.Path;
import java.util.List;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface ConventionFacade {
  List<Path> javaModuleSourcePath();
  List<Path> javaModuleTestPath();
  List<Path> javaModuleExplodedSourcePath();
  List<Path> javaModuleExplodedTestPath();
  Path javaModuleDocSourcePath();
  Path javaModuleDocTestPath();
  
  Path javaModuleArtifactSourcePath();
  Path javaModuleArtifactTestPath();
  Path javaModuleSrcArtifactSourcePath();
  Path javaModuleDocArtifactSourcePath();
  Path javaModuleSrcArtifactTestPath();
  Path javaModuleDocArtifactTestPath();
}
