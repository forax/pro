package com.github.forax.pro.plugin.convention;

import java.nio.file.Path;
import java.util.List;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface Convention {
  public Path javaHome();
  public void javaHome(Path path);
  
  public List<Path> javaModuleSourcePath();
  public void javaModuleSourcePath(List<Path> path);
  public List<Path> javaModuleExplodedSourcePath();
  public void javaModuleExplodedSourcePath(List<Path> path);
  public Path javaModuleArtifactSourcePath();
  public void javaModuleArtifactSourcePath(Path path);
  
  public List<Path> javaModuleTestPath();
  public void javaModuleTestPath(List<Path> path);
  public List<Path> javaModuleMergedTestPath();
  public void javaModuleMergedTestPath(List<Path> path);
  public List<Path> javaModuleExplodedTestPath();
  public void javaModuleExplodedTestPath(List<Path> path);
  public Path javaModuleArtifactTestPath();
  public void javaModuleArtifactTestPath(Path path);
  
  public List<Path> javaModuleDependencyPath();
  public void javaModuleDependencyPath(List<Path> path);
  
  public Path javaLinkerImagePath();
  public void javaLinkerImagePath(Path path);
}
