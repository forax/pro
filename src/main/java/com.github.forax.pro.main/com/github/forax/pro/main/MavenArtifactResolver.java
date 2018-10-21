package com.github.forax.pro.main;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;
import static java.util.Map.entry;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.forax.pro.aether.Aether;
import com.github.forax.pro.aether.ArtifactDescriptor;

class MavenArtifactResolver {
  public static void resolve(String mavenId) throws IOException {
    var mavenLocalRepository = Path.of("target/deps/maven-local");
    
    var aether = Aether.create(mavenLocalRepository, List.of());
    
    var artifactQuery = aether.createArtifactQuery(mavenId);
    var dependencies = aether.dependencies(artifactQuery);
    var resolvedArtifacts = aether.download(new ArrayList<>(dependencies));
    
    var map = resolvedArtifacts.stream()
      .flatMap(artifact -> findArtifactModuleName(artifact).map(name -> entry(artifact, name)).stream())
      .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
    
    resolvedArtifacts.sort(nullsLast(comparing(map::get)));
    
    for(var resolvedArtifact: resolvedArtifacts) {
      var artifactId = resolvedArtifact.getGroupId() + ':' + resolvedArtifact.getArtifactId() + ':' + resolvedArtifact.getVersion();
      System.out.println(
          Optional.ofNullable(map.get(resolvedArtifact))
            .map(name -> name + '=' + artifactId)
            .orElse(artifactId + " is not JPMS compatible"));
    }
  }
  
  private static Optional<String> findArtifactModuleName(ArtifactDescriptor resolvedArtifact) {
    var finder = ModuleFinder.of(resolvedArtifact.getPath());
    var referenceOpt = finder.findAll().stream().findFirst();
    return referenceOpt.map(ref -> ref.descriptor().name());
  }
}
