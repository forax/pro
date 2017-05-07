package com.github.forax.pro.plugin.packager;

import com.github.forax.pro.api.TypeCheckedConfig;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@TypeCheckedConfig
public interface PackagerConf {
  List<Path> moduleExplodedSourcePath();

  void moduleExplodedSourcePath(List<Path> moduleExplodedPath);

  Path moduleArtifactSourcePath();

  void moduleArtifactSourcePath(Path destination);

  List<Path> moduleExplodedTestPath();

  void moduleExplodedTestPath(List<Path> moduleExplodedPath);

  Path moduleArtifactTestPath();

  void moduleArtifactTestPath(Path destination);

  Optional<List<String>> moduleMetadata();

  void moduleMetadata(List<String> metadata);

  Optional<List<String>> rawArguments();

  void rawArguments(List<String> rawArguments);
}
