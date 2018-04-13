package com.github.forax.pro.plugin.convention;

import static com.github.forax.pro.api.MutableConfig.derive;

import java.nio.file.Paths;
import java.util.List;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;
import com.github.forax.pro.api.helper.ProConf;

public class ConventionPlugin implements Plugin {
  @Override
  public String name() {
    return "convention";
  }

  @Override
  public void init(MutableConfig config) {
    var convention = config.getOrUpdate(name(), ConventionConf.class);
    var proConf = config.getOrThrow("pro", ProConf.class);
    convention.javaHome(Paths.get(System.getProperty("java.home")));
    
    derive(convention, ConventionConf::javaModuleSourcePath,
           proConf, c -> List.of(c.currentDir().resolve("src/main/java")));
    derive(convention, ConventionConf::javaModuleSourceResourcesPath,
           proConf, c -> List.of(c.currentDir().resolve("src/main/resources")));
    derive(convention, ConventionConf::javaModuleExplodedSourcePath,
           proConf, c -> List.of(c.currentDir().resolve("target/main/exploded")));
    derive(convention, ConventionConf::javaModuleArtifactSourcePath,
           proConf, c -> c.currentDir().resolve("target/main/artifact"));
    derive(convention, ConventionConf::javaModuleDocSourcePath,
        proConf, c -> c.currentDir().resolve("target/main/doc"));
    derive(convention, ConventionConf::javaModuleSrcArtifactSourcePath,
        proConf, c -> c.currentDir().resolve("target/main/artifact-src"));
    derive(convention, ConventionConf::javaModuleDocArtifactSourcePath,
        proConf, c -> c.currentDir().resolve("target/main/artifact-doc"));
    
    derive(convention, ConventionConf::javaModuleTestPath,
        proConf, c -> List.of(c.currentDir().resolve("src/test/java")));
    derive(convention, ConventionConf::javaModuleTestResourcesPath,
        proConf, c -> List.of(c.currentDir().resolve("src/test/resources")));
    derive(convention, ConventionConf::javaModuleMergedTestPath,
        proConf, c -> List.of(c.currentDir().resolve("target/test/merged")));
    derive(convention, ConventionConf::javaModuleExplodedTestPath,
        proConf, c -> List.of(c.currentDir().resolve("target/test/exploded")));
    derive(convention, ConventionConf::javaModuleArtifactTestPath,
        proConf, c -> c.currentDir().resolve("target/test/artifact"));
    derive(convention, ConventionConf::javaModuleDocTestPath,
        proConf, c -> c.currentDir().resolve("target/test/doc"));
    derive(convention, ConventionConf::javaModuleSrcArtifactTestPath,
        proConf, c -> c.currentDir().resolve("target/test/artifact-src"));
    derive(convention, ConventionConf::javaModuleDocArtifactTestPath,
        proConf, c -> c.currentDir().resolve("target/test/artifact-doc"));
    
    derive(convention, ConventionConf::javaModuleDependencyPath,
        proConf, c -> List.of(c.currentDir().resolve("deps")));
    derive(convention, ConventionConf::javaMavenLocalRepositoryPath,
        proConf, c -> c.currentDir().resolve("target/deps/maven-local"));
    derive(convention, ConventionConf::javaModuleDependencyFixerPath,
        proConf, c -> c.currentDir().resolve("target/deps/module-fixer"));
    derive(convention, ConventionConf::javaModuleUberPath,
        proConf, c -> c.currentDir().resolve("target/uber"));
    derive(convention, ConventionConf::javaModuleUberExplodedPath,
        proConf, c -> c.currentDir().resolve("target/uber/exploded"));
    derive(convention, ConventionConf::javaLinkerImagePath,
        proConf, c -> c.currentDir().resolve("target/image"));
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
