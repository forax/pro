package com.github.forax.pro.plugin.convention;

import static com.github.forax.pro.api.MutableConfig.derive;

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
    convention.currentDir(Paths.get("."));
    convention.javaHome(Paths.get(System.getProperty("java.home")));
    
    derive(convention, ConventionConf::javaModuleSourcePath,
           convention, c -> List.of(c.currentDir().resolve("src/main/java")));
    derive(convention, ConventionConf::javaModuleSourceResourcesPath,
           convention, c -> List.of(c.currentDir().resolve("src/main/resources")));
    derive(convention, ConventionConf::javaModuleExplodedSourcePath,
           convention, c -> List.of(c.currentDir().resolve("target/main/exploded")));
    derive(convention, ConventionConf::javaModuleArtifactSourcePath,
           convention, c -> c.currentDir().resolve("target/main/artifact"));
    
    derive(convention, ConventionConf::javaModuleTestPath,
        convention, c -> List.of(c.currentDir().resolve("src/test/java")));
    derive(convention, ConventionConf::javaModuleTestResourcesPath,
        convention, c -> List.of(c.currentDir().resolve("src/test/resources")));
    derive(convention, ConventionConf::javaModuleMergedTestPath,
        convention, c -> List.of(c.currentDir().resolve("target/test/merged")));
    derive(convention, ConventionConf::javaModuleExplodedTestPath,
        convention, c -> List.of(c.currentDir().resolve("target/test/exploded")));
    derive(convention, ConventionConf::javaModuleArtifactTestPath,
        convention, c -> c.currentDir().resolve("target/test/artifact"));
    
    derive(convention, ConventionConf::javaModuleDependencyPath,
        convention, c -> List.of(c.currentDir().resolve("deps")));
    derive(convention, ConventionConf::javaMavenLocalRepositoryPath,
        convention, c -> c.currentDir().resolve("target/deps/maven-local"));
    derive(convention, ConventionConf::javaModuleUberPath,
        convention, c -> c.currentDir().resolve("target/uber"));
    derive(convention, ConventionConf::javaModuleUberExplodedPath,
        convention, c -> c.currentDir().resolve("target/uber/exploded"));
    derive(convention, ConventionConf::javaLinkerImagePath,
        convention, c -> c.currentDir().resolve("target/image"));
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
