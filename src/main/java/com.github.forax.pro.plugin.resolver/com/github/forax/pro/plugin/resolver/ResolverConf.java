package com.github.forax.pro.plugin.resolver;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface ResolverConf {
  List<String> dependencies();
  void dependencies(List<String> dependencies);
  
  List<Path> moduleSourcePath();
  void moduleSourcePath(List<Path> modulePath);
  
  List<Path> moduleTestPath();
  void moduleTestPath(List<Path> modulePath);
  
  Optional<List<Path>> modulePath();
  void modulePath(List<Path> modulePath);
  
  Path mavenLocalRepositoryPath();
  void mavenLocalRepositoryPath(Path path);
  
  List<Path> moduleDependencyPath();
  void moduleDependencyPath(List<Path> path);
}
