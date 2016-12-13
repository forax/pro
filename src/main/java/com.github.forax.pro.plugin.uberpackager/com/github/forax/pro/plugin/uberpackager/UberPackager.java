package com.github.forax.pro.plugin.uberpackager;

import java.nio.file.Path;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface UberPackager {
  Path moduleArtifactSourcePath();
  void moduleArtifactSourcePath(Path destination);
  
  Path moduleUberPath();
  void moduleUberPath(Path moduleUberPath);
  
  Path moduleUberExplodedPath();
  void moduleUberExplodedPath(Path moduleUberExplodedPath);
}
