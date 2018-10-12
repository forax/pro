package com.github.forax.pro.aether.bootstrap;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.github.forax.pro.aether.Aether;

public class Main {
  public static void main(String[] args) throws IOException {
    var mavenLocalRepository = Paths.get("target/deps/maven-local");
    var aether = Aether.create(mavenLocalRepository, List.of());
    
    var artifactQuery = aether.createArtifactQuery("org.ow2.asm:asm-util:7.0-beta");
    var dependencies = aether.dependencies(artifactQuery);
    System.out.println("dependencies" + dependencies);
    
    var resolvedArtifacts = aether.download(new ArrayList<>(dependencies));
    System.out.println("downloaded " + resolvedArtifacts);
  }
}
