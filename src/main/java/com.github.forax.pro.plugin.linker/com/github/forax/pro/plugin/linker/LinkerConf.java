package com.github.forax.pro.plugin.linker;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface LinkerConf {
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
  
  boolean ignoreSigningInformation();
  void ignoreSigningInformation(boolean ignoreSigningInformation);
  
  Optional<List<String>> rawArguments();
  void rawArguments(List<String> rawArguments);
  
  Optional<List<Path>> modulePath();
  void modulePath(List<Path> modulePath);
  
  Path systemModulePath();
  void systemModulePath(Path resolve);
  
  Path moduleArtifactSourcePath();
  void moduleArtifactSourcePath(Path javaModuleArtifactSourcePath);
  
  List<Path> moduleDependencyPath();
  void moduleDependencyPath(List<Path> path);
  
  Optional<List<String>> rootModules();
  void rootModules(List<String> rootModules);
  
  Optional<List<String>> serviceNames();
  void serviceNames(List<String> serviceNames);
  
  Optional<List<String>>  launchers();
  void launchers(List<String> launchers);
}
