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
    ConventionConf convention = config.getOrUpdate(name(), ConventionConf.class);
    ProConf pro = config.getOrThrow("pro", ProConf.class);
    convention.javaHome(Paths.get(System.getProperty("java.home")));
    
    derive(convention, ConventionConf::javaModuleSourcePath,
           pro, c -> List.of(c.currentDir().resolve("src/main/java")));
    derive(convention, ConventionConf::javaModuleSourceResourcesPath,
           pro, c -> List.of(c.currentDir().resolve("src/main/resources")));
    derive(convention, ConventionConf::javaModuleExplodedSourcePath,
           pro, c -> List.of(c.currentDir().resolve("target/main/exploded")));
    derive(convention, ConventionConf::javaModuleArtifactSourcePath,
           pro, c -> c.currentDir().resolve("target/main/artifact"));
    derive(convention, ConventionConf::javaModuleDocSourcePath,
        pro, c -> c.currentDir().resolve("target/main/doc"));
    derive(convention, ConventionConf::javaModuleSrcArtifactSourcePath,
        pro, c -> c.currentDir().resolve("target/main/artifact-src"));
    derive(convention, ConventionConf::javaModuleDocArtifactSourcePath,
        pro, c -> c.currentDir().resolve("target/main/artifact-doc"));
    
    derive(convention, ConventionConf::javaModuleTestPath,
        pro, c -> List.of(c.currentDir().resolve("src/test/java")));
    derive(convention, ConventionConf::javaModuleTestResourcesPath,
        pro, c -> List.of(c.currentDir().resolve("src/test/resources")));
    derive(convention, ConventionConf::javaModuleMergedTestPath,
        pro, c -> List.of(c.currentDir().resolve("target/test/merged")));
    derive(convention, ConventionConf::javaModuleExplodedTestPath,
        pro, c -> List.of(c.currentDir().resolve("target/test/exploded")));
    derive(convention, ConventionConf::javaModuleArtifactTestPath,
        pro, c -> c.currentDir().resolve("target/test/artifact"));
    derive(convention, ConventionConf::javaModuleDocTestPath,
        pro, c -> c.currentDir().resolve("target/test/doc"));
    derive(convention, ConventionConf::javaModuleSrcArtifactTestPath,
        pro, c -> c.currentDir().resolve("target/test/artifact-src"));
    derive(convention, ConventionConf::javaModuleDocArtifactTestPath,
        pro, c -> c.currentDir().resolve("target/test/artifact-doc"));
    
    derive(convention, ConventionConf::javaModuleDependencyPath,
        pro, c -> List.of(c.currentDir().resolve("deps")));
    derive(convention, ConventionConf::javaMavenLocalRepositoryPath,
        pro, c -> c.currentDir().resolve("target/deps/maven-local"));
    derive(convention, ConventionConf::javaModuleDependencyFixerPath,
        pro, c -> c.currentDir().resolve("target/deps/module-fixer"));
    derive(convention, ConventionConf::javaModuleUberPath,
        pro, c -> c.currentDir().resolve("target/uber"));
    derive(convention, ConventionConf::javaModuleUberExplodedPath,
        pro, c -> c.currentDir().resolve("target/uber/exploded"));
    derive(convention, ConventionConf::javaLinkerImagePath,
        pro, c -> c.currentDir().resolve("target/image"));
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
