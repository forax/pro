package com.github.forax.pro.plugin.docer;


import java.nio.file.Path;
import java.util.List;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface ConventionFacade {
  List<Path> javaModuleSourcePath();
  Path javaModuleDocSourcePath();
  
  List<Path> javaModuleTestPath();
  Path javaModuleDocTestPath();
  
  List<Path> javaModuleDependencyPath();
}
