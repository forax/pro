package com.github.forax.pro.plugin.resolver;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import com.github.forax.pro.aether.Aether;
import com.github.forax.pro.aether.ArtifactQuery;
import com.github.forax.pro.helper.util.Strategy;

class DependencyParser {
  interface ModuleNameMap {
    Optional<String> getArtifactCoords(String moduleName);
    
    static ModuleNameMap create(ResolverConf resolverConf) {
      URL moduleNameListURL;
      try {
        moduleNameListURL = resolverConf.moduleNameList().toURL();
      } catch (MalformedURLException e) {
        throw new UncheckedIOException(new IOException(e));
      }
      Map<String, String> moduleNameMap;
      try(var input = moduleNameListURL.openStream();
          var reader = new InputStreamReader(input, UTF_8);
          var buffered = new BufferedReader(reader);
          var lines = buffered.lines()) {
        moduleNameMap = lines
            .map(line -> line.split("="))
            .collect(toMap(tokens -> tokens[0], tokens -> tokens[1]));
      } catch(IOException e) {
        throw new UncheckedIOException(e);
      }
      return moduleName -> Optional.ofNullable(moduleNameMap.get(moduleName));
    }
  }
  
  static void parseDependencies(List<String> dependencies, Aether aether, Strategy<String, ModuleNameAndArtifactsCoords> dependencyParsingStrategy, BiConsumer<String, ArtifactQuery> listener) {
    for(var dependency: dependencies) {
      var result = dependencyParsingStrategy.get(dependency).orElseThrow(() -> new IllegalStateException("invalid dependency format " + dependency));
      var module = result.module;
      var artifactsCoords = result.artifactsCoords; 
      
      for(var artifactCoords: artifactsCoords) {
        var artifactQuery = aether.createArtifactQuery(artifactCoords);
        listener.accept(module, artifactQuery);
      }
    }
  }
  
  static class ModuleNameAndArtifactsCoords {
    final String module;
    final List<String> artifactsCoords;
    
    ModuleNameAndArtifactsCoords(String module, List<String> artifactsCoords) {
      this.module = module;
      this.artifactsCoords = artifactsCoords;
    }
  }
    
  static Optional<ModuleNameAndArtifactsCoords> parseMavenArtifactCoords(String dependency) {
    var index = dependency.indexOf('=');
    if (index == -1) {
      return Optional.empty();
    }
    var module = dependency.substring(0, index);
    var artifactNames = dependency.substring(index + 1);
    
    var artifactsCoords = artifactNames.split(",");
    if (artifactsCoords.length == 0) {
      throw new IllegalStateException("invalid dependency format " + dependency + ", empty Maven coords");
    }
    return Optional.of(new ModuleNameAndArtifactsCoords(module, List.of(artifactsCoords)));
  }
  
  static Optional<ModuleNameAndArtifactsCoords> parseVersionAndUseModuleNameMap(String dependency, ModuleNameMap moduleNameMap) {
    var index = dependency.indexOf(':');
    if (index == -1) {
      return Optional.empty();
    }
    var module = dependency.substring(0, index);
    var version = dependency.substring(index + 1);
    Optional<String> artifactCoordsOpt = moduleNameMap.getArtifactCoords(module);
    if (!artifactCoordsOpt.isPresent()) {
      throw new IllegalStateException("no Maven artifact declared for module " + module + " in module-maven.properties");
    }
    return Optional.of(new ModuleNameAndArtifactsCoords(module, List.of(artifactCoordsOpt.get() + ':' + version)));
  }
}
