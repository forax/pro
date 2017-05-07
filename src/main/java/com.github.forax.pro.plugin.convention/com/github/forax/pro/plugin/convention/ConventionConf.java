package com.github.forax.pro.plugin.convention;

import com.github.forax.pro.api.TypeCheckedConfig;
import java.nio.file.Path;
import java.util.List;

@TypeCheckedConfig
public interface ConventionConf {
  Path javaHome();

  void javaHome(Path path);

  List<Path> javaModuleSourcePath();

  void javaModuleSourcePath(List<Path> path);

  List<Path> javaModuleSourceResourcesPath();

  void javaModuleSourceResourcesPath(List<Path> path);

  List<Path> javaModuleExplodedSourcePath();

  void javaModuleExplodedSourcePath(List<Path> path);

  Path javaModuleArtifactSourcePath();

  void javaModuleArtifactSourcePath(Path path);

  List<Path> javaModuleTestPath();

  void javaModuleTestPath(List<Path> path);

  List<Path> javaModuleTestResourcesPath();

  void javaModuleTestResourcesPath(List<Path> path);

  List<Path> javaModuleMergedTestPath();

  void javaModuleMergedTestPath(List<Path> path);

  List<Path> javaModuleExplodedTestPath();

  void javaModuleExplodedTestPath(List<Path> path);

  Path javaModuleArtifactTestPath();

  void javaModuleArtifactTestPath(Path path);

  Path javaMavenLocalRepositoryPath();

  void javaMavenLocalRepositoryPath(Path path);

  Path javaModuleDependencyFixerPath();

  void javaModuleDependencyFixerPath(Path path);

  List<Path> javaModuleDependencyPath();

  void javaModuleDependencyPath(List<Path> path);

  Path javaModuleUberPath();

  void javaModuleUberPath(Path moduleUberPath);

  Path javaModuleUberExplodedPath();

  void javaModuleUberExplodedPath(Path moduleUberExplodedPath);

  Path javaLinkerImagePath();

  void javaLinkerImagePath(Path path);
}
