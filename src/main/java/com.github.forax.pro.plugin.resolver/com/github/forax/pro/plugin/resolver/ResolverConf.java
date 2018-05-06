package com.github.forax.pro.plugin.resolver;

import java.net.URI;
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
  
  Optional<List<URI>> remoteRepositories();
  void remoteRepositories(List<URI> remoteRepositories);
  
  List<Path> moduleDependencyPath();
  void moduleDependencyPath(List<Path> path);
  
  boolean checkForUpdate();
  void checkForUpdate(boolean checkForUpdate);
}
