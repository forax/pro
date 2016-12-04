package com.github.forax.pro.plugin.modulefixer;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface ModuleFixer {
  List<Path> moduleDependencyPath();
  void moduleDependencyPath(List<Path> path);
  
  Optional<Path> moduleDependencyFixerPath();
  void moduleDependencyFixerPath(Path path);
  
  boolean force();
  void force(boolean force);
  
  Optional<List<String>> additionalRequires();
  void additionalRequires(List<String> additionalRequires);
}
