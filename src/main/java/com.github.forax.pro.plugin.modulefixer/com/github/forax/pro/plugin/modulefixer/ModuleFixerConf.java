package com.github.forax.pro.plugin.modulefixer;

import com.github.forax.pro.api.TypeCheckedConfig;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@TypeCheckedConfig
public interface ModuleFixerConf {
  List<Path> moduleDependencyPath();

  void moduleDependencyPath(List<Path> path);

  Path moduleDependencyFixerPath();

  void moduleDependencyFixerPath(Path path);

  boolean force();

  void force(boolean force);

  Optional<List<String>> additionalRequires();

  void additionalRequires(List<String> additionalRequires);

  Optional<List<String>> additionalUses();

  void additionalUses(List<String> additionalUses);
}
