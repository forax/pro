package com.github.forax.pro.plugin.frozer;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface FrozerConf {
  Optional<String> rootModule();
  void rootModule(String rootModule);
  
  List<Path> modulePath();
  void modulePath(List<Path> modulePath);
  
  Path moduleFrozenArtifactSourcePath();
  void moduleFrozenArtifactSourcePath(Path moduleFrozenArtifactSourcePath);
}
