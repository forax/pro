package com.github.forax.pro.plugin.linker;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface Linker {
  Path destination();
  void destination(Path destination);
  
  int compressLevel();
  void compressLevel(int level);
  
  boolean stripDebug();
  void stripDebug(boolean stripDebug);
  
  boolean stripNativeCommands();
  void stripNativeCommands(boolean stripNativeCommands);
  
  boolean includeSystemJMODs();
  void includeSystemJMODs(boolean includeSystemJMODs);
  
  Optional<List<Path>> modulePath();
  void modulePath(List<Path> modulePath);
  
  Path systemModulePath();
  void systemModulePath(Path resolve);
  
  Path moduleArtifactSourcePath();
  void moduleArtifactSourcePath(Path javaModuleArtifactSourcePath);
  
  public List<Path> moduleDependencyPath();
  public void moduleDependencyPath(List<Path> path);
  
  Optional<List<String>> rootModules();
  void rootModules(List<String> rootModules);
  
  Optional<List<String>> serviceNames();
  void serviceNames(List<String> serviceNames);
}
