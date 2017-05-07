package com.github.forax.pro.plugin.uberpackager;

import com.github.forax.pro.api.TypeCheckedConfig;
import java.nio.file.Path;
import java.util.List;

@TypeCheckedConfig
public interface UberPackagerConf {
  Path moduleArtifactSourcePath();

  void moduleArtifactSourcePath(Path destination);

  void moduleDependencyPath(List<Path> moduleDependencyPath);

  List<Path> moduleDependencyPath();

  Path moduleUberPath();

  void moduleUberPath(Path moduleUberPath);

  Path moduleUberExplodedPath();

  void moduleUberExplodedPath(Path moduleUberExplodedPath);
}
