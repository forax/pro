package com.github.forax.pro.plugin.packager;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface PackagerConf {
  boolean generateSourceTestBale();
  void generateSourceTestBale(boolean generateBale);
  
  List<Path> moduleSourcePath();
  void moduleSourcePath(List<Path> moduleSourcePath);
  Path moduleDocSourcePath();
  void moduleDocSourcePath(Path moduleDocSourcePath);
  List<Path> moduleExplodedSourcePath();
  void moduleExplodedSourcePath(List<Path> moduleExplodedPath);
  
  Path moduleArtifactSourcePath();
  void moduleArtifactSourcePath(Path moduleArtifactSourcePath);
  Path moduleBaleSourcePath();
  void moduleBaleSourcePath(Path moduleBaleSourcePath);
  
  
  List<Path> moduleTestPath();
  void moduleTestPath(List<Path> moduleTestPath);
  Path moduleDocTestPath();
  void moduleDocTestPath(Path moduleDocTestPath);
  List<Path> moduleExplodedTestPath();
  void moduleExplodedTestPath(List<Path> moduleExplodedPath);
  
  Path moduleArtifactTestPath();
  void moduleArtifactTestPath(Path destination);
  Path moduleBaleTestPath();
  void moduleBaleTestPath(Path moduleBaleTestPath);
  
  Optional<List<String>> moduleMetadata();
  void moduleMetadata(List<String> metadata);
  
  Optional<List<String>> rawArguments();
  void rawArguments(List<String> rawArguments);
}
