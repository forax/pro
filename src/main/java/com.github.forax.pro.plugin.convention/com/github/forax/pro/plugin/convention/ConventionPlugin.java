package com.github.forax.pro.plugin.convention;

import java.nio.file.Paths;
import java.util.List;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;

public class ConventionPlugin implements Plugin {
  @Override
  public String name() {
    return "convention";
  }

  @Override
  public void init(MutableConfig config) {
    ConventionConf convention = config.getOrUpdate(name(), ConventionConf.class);
    convention.javaHome(Paths.get(System.getProperty("java.home")));
    
    convention.javaModuleSourcePath(List.of(Paths.get("src/main/java")));
    convention.javaModuleExplodedSourcePath(List.of(Paths.get("target/main/exploded")));
    convention.javaModuleArtifactSourcePath(Paths.get("target/main/artifact"));
    
    convention.javaModuleTestPath(List.of(Paths.get("src/test/java")));
    convention.javaModuleMergedTestPath(List.of(Paths.get("target/test/merged")));
    convention.javaModuleExplodedTestPath(List.of(Paths.get("target/test/exploded")));
    convention.javaModuleArtifactTestPath(Paths.get("target/test/artifact"));
    
    convention.javaModuleDependencyPath(List.of(Paths.get("deps")));
    convention.javaMavenLocalRepositoryPath(Paths.get("target/deps/maven-local"));
    convention.javaModuleUberPath(Paths.get("target/uber"));
    convention.javaModuleUberExplodedPath(Paths.get("target/uber/exploded"));
    convention.javaLinkerImagePath(Paths.get("target/image"));
  }
  
  @Override
  public void configure(MutableConfig config) {
    // empty
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
    throw new UnsupportedOperationException("this plugin can not watch any directories");
  }
  
  @Override
  public int execute(Config config) {
    throw new UnsupportedOperationException("this plugin can not be executed");
  }
}
