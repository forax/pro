package com.github.forax.pro.plugin.resolver;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.forax.pro.aether.Aether;
import com.github.forax.pro.aether.ArtifactDescriptor;
import com.github.forax.pro.aether.ArtifactInfo;
import com.github.forax.pro.aether.ArtifactQuery;
import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.helper.FileHelper;
import com.github.forax.pro.helper.Log;
import com.github.forax.pro.helper.ModuleHelper;
import com.github.forax.pro.helper.util.StableList;

public class ResolverPlugin implements Plugin {
  @Override
  public String name() {
    return "resolver";
  }

  @Override
  public void init(MutableConfig config) {
    config.getOrUpdate(name(), ResolverConf.class);
  }
  
  @Override
  public void configure(MutableConfig config) {
    ResolverConf resolver = config.getOrUpdate(name(), ResolverConf.class);
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class);
    
    // inputs
    resolver.moduleSourcePath(convention.javaModuleSourcePath());
    resolver.moduleTestPath(convention.javaModuleTestPath());
    
    // outputs
    resolver.moduleDependencyPath(convention.javaModuleDependencyPath());
    resolver.mavenLocalRepositoryPath(convention.javaMavenLocalRepositoryPath());
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
    ResolverConf resolver = config.getOrThrow(name(), ResolverConf.class);
    resolver.moduleSourcePath().forEach(registry::watch);
    resolver.moduleTestPath().forEach(registry::watch);
  }
  
  static Optional<List<Path>> modulePathOrDependencyPath(Optional<List<Path>> modulePath, List<Path> moduleDependencyPath, List<Path> additionnalPath) {
    return modulePath
             .or(() -> Optional.of(
                 new StableList<Path>().appendAll(moduleDependencyPath).appendAll(additionnalPath)))
             .map(FileHelper::pathFromFilesThatExist)
             .filter(list -> !list.isEmpty());
  }
  
  @Override
  public int execute(Config config) throws IOException {
    Log log = Log.create(name(), config.getOrThrow("pro", ProConf.class).loglevel());
    log.debug(config, conf -> "config " + config);
    
    ResolverConf resolver = config.getOrThrow(name(), ResolverConf.class);
    
    ModuleFinder moduleSourceFinder = ModuleHelper.sourceModuleFinders(resolver.moduleSourcePath());
    
    /*
    List<Path> moduleTestPath = FileHelper.pathFromFilesThatExist(compiler.moduleTestPath());
    if (moduleTestPath.isEmpty()) {
      return 0;
    }
    
    ModuleFinder moduleTestFinder = ModuleHelper.sourceModuleFinders(compiler.moduleTestPath());
    */
    
    Optional<List<Path>> modulePath = modulePathOrDependencyPath(resolver.modulePath(),
        resolver.moduleDependencyPath(), List.of());
    
    ModuleFinder dependencyFinder = ModuleFinder.compose(
        modulePath
            .stream()
            .flatMap(List::stream)
            .map(ModuleFinder::of)
            .toArray(ModuleFinder[]::new));
    List<String> rootSourceNames = moduleSourceFinder.findAll().stream()
            .map(ref -> ref.descriptor().name())
            .collect(Collectors.toList());
    
    ModuleFinder allFinder = ModuleFinder.compose(moduleSourceFinder, dependencyFinder, ModuleFinder.ofSystem());
    //allFinder.findAll().stream().sorted(Comparator.comparing(ModuleReference::toString)).forEach(System.out::println);
    
    LinkedHashSet<String> unresolvedModules = new LinkedHashSet<>();
    boolean resolved = ModuleHelper.resolveOnlyRequires(allFinder, rootSourceNames,
        (moduleName, dependencyChain) -> unresolvedModules.add(moduleName));
    if (resolved) {
      return 0;
    }
    
    log.debug(unresolvedModules, unresolved -> "unresolvedModules " + unresolved);
    
    List<URI> remoteRepositories = resolver.remoteRepositories().orElse(List.of());
    log.debug(remoteRepositories, remotes -> "remoteRepositories " + remotes);
    
    Aether aether = Aether.create(resolver.mavenLocalRepositoryPath(), remoteRepositories);
    
    // mapping between module name and Maven artifact name
    List<String> dependencies = resolver.dependencies();
    LinkedHashMap<String, ArtifactQuery> moduleToArtifactMap = new LinkedHashMap<>();
    LinkedHashMap<String, String> artifactKeyToModuleMap = new LinkedHashMap<>();
    for(String dependency: dependencies) {
      int index = dependency.indexOf('=');
      if (index == -1) {
        throw new IllegalStateException("invalid dependency format " + dependency);
      }
      String module = dependency.substring(0, index);
      String artifactCoords = dependency.substring(index + 1);
      
      ArtifactQuery artifactQuery = aether.createArtifactQuery(artifactCoords);
      moduleToArtifactMap.put(module, artifactQuery);
      artifactKeyToModuleMap.put(artifactQuery.getArtifactKey(), module);
    }
    
    verifyDeclaration("modules", unresolvedModules, moduleToArtifactMap.keySet());
    
    List<ArtifactQuery> unresolvedRootArtifacts = unresolvedModules.stream()
        .map(moduleToArtifactMap::get)
        .collect(Collectors.toList());
    log.debug(unresolvedRootArtifacts, unresolvedRoots -> "unresolved root artifacts " + unresolvedRoots);
    
    LinkedHashSet<ArtifactInfo> unresolvedArtifacts = new LinkedHashSet<>();
    for(ArtifactQuery unresolvedRootArtifact: unresolvedRootArtifacts) {
      unresolvedArtifacts.addAll(aether.dependencies(unresolvedRootArtifact));  
    }
    log.debug(unresolvedArtifacts, unresolvedArtifactList -> "unresolved artifacts " + unresolvedArtifactList);
    
    verifyDeclaration("artifacts",
        unresolvedArtifacts.stream().map(ArtifactInfo::getArtifactKey).collect(Collectors.toSet()),
        artifactKeyToModuleMap.keySet());
    
    List<ArtifactDescriptor> resolvedArtifacts =
        aether.download(new ArrayList<>(unresolvedArtifacts));
    
    log.info(resolvedArtifacts, resolvedArtifactList -> "resolved artifacts " + resolvedArtifactList);
    
    Path moduleDependencyPath = resolver.moduleDependencyPath().get(0);
    Files.createDirectories(moduleDependencyPath);
    
    ArrayList<ArtifactDescriptor> undeclaredArtifactIds = new ArrayList<>();
    for(ArtifactDescriptor resolvedArtifact: resolvedArtifacts) {
      String moduleName = artifactKeyToModuleMap.get(resolvedArtifact.getArtifactKey());
      if (moduleName == null) {
        undeclaredArtifactIds.add(resolvedArtifact);
      } else {
        log.info(null, __ -> moduleName + " (" + resolvedArtifact + ") downloaded from repositories");
        Files.copy(resolvedArtifact.getPath(), moduleDependencyPath.resolve(moduleName + ".jar"));
      }
    }
    if (!undeclaredArtifactIds.isEmpty()) {
      throw new IllegalStateException("no dependency declared for Maven artifacts " + undeclaredArtifactIds);
    }
    
    return 0;
  }
  
  private static void verifyDeclaration(String kind, Set<String> unresolvedModules, Set<String> dependencySet) {
    ArrayList<String> undeclaredModules = new ArrayList<>();
    for(String module: unresolvedModules) {
      if (!dependencySet.contains(module)) {
        undeclaredModules.add(module);
      }
    }
    if (!undeclaredModules.isEmpty()) {
      throw new IllegalStateException("no dependency declared for unresolved " + kind + " " + undeclaredModules);
    }
  }
}
