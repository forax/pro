package com.github.forax.pro.plugin.resolver;

import static com.github.forax.pro.api.MutableConfig.derive;

import java.io.IOException;
import java.lang.module.ModuleFinder;
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
    var resolverConf = config.getOrUpdate(name(), ResolverConf.class);
    var convention = config.getOrThrow("convention", ConventionFacade.class);
    
    // inputs
    derive(resolverConf, ResolverConf::moduleSourcePath, convention, ConventionFacade::javaModuleSourcePath);
    derive(resolverConf, ResolverConf::moduleTestPath, convention, ConventionFacade::javaModuleTestPath);
    
    // outputs
    derive(resolverConf, ResolverConf::moduleDependencyPath, convention, ConventionFacade::javaModuleDependencyPath);
    derive(resolverConf, ResolverConf::mavenLocalRepositoryPath, convention, ConventionFacade::javaMavenLocalRepositoryPath);
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
    var resolverConf = config.getOrThrow(name(), ResolverConf.class);
    resolverConf.moduleSourcePath().forEach(registry::watch);
    resolverConf.moduleTestPath().forEach(registry::watch);
  }
  
  static Optional<List<Path>> modulePathOrDependencyPath(Optional<List<Path>> modulePath, List<Path> moduleDependencyPath, List<Path> additionnalPath) {
    return modulePath
             .or(() -> Optional.of(
                 StableList.from(moduleDependencyPath).appendAll(additionnalPath)))
             .map(FileHelper::pathFromFilesThatExist)
             .filter(list -> !list.isEmpty());
  }
  
  
  private static boolean resolveModuleDependencies(ModuleFinder moduleFinder, ModuleFinder dependencyFinder, LinkedHashSet<String> unresolvedModules) {
    var rootSourceNames = moduleFinder.findAll().stream()
            .map(ref -> ref.descriptor().name())
            .collect(Collectors.toList());
    var allFinder = ModuleFinder.compose(moduleFinder, dependencyFinder, ModuleHelper.systemModulesFinder());
    
    return ModuleHelper.resolveOnlyRequires(allFinder, rootSourceNames,
        (moduleName, __) -> unresolvedModules.add(moduleName));
  }
  
  @Override
  public int execute(Config config) throws IOException {
    var log = Log.create(name(), config.getOrThrow("pro", ProConf.class).loglevel());
    log.debug(config, conf -> "config " + config);
    
    var resolverConf = config.getOrThrow(name(), ResolverConf.class);
    
    // create finder for dependencies (or module path if specified)
    var modulePathOpt = modulePathOrDependencyPath(resolverConf.modulePath(),
        resolverConf.moduleDependencyPath(), List.of());
    
    var dependencyFinder = ModuleFinder.compose(
        modulePathOpt
            .stream()
            .flatMap(List::stream)
            .map(ModuleFinder::of)
            .toArray(ModuleFinder[]::new));
    
    
    // find unresolved modules in dependencies (for source and test)
    var unresolvedModules = new LinkedHashSet<String>();
    
    // source module names
    var moduleSourceFinder = ModuleHelper.sourceModuleFinders(resolverConf.moduleSourcePath());
    var sourceResolved = resolveModuleDependencies(moduleSourceFinder, dependencyFinder, unresolvedModules);
    
    // test module names
    var moduleTestPath = FileHelper.pathFromFilesThatExist(resolverConf.moduleTestPath());
    if (!moduleTestPath.isEmpty()) {
      ModuleFinder moduleTestFinder = ModuleHelper.sourceModuleFinders(resolverConf.moduleTestPath());
      sourceResolved &= resolveModuleDependencies(moduleTestFinder, dependencyFinder, unresolvedModules);
    }
    
    // everything is resolved, nothing to do
    if (sourceResolved) {
      return 0;
    }
    
    log.debug(unresolvedModules, unresolved -> "unresolvedModules " + unresolved);
    
    var remoteRepositories = resolverConf.remoteRepositories().orElse(List.of());
    log.debug(remoteRepositories, remotes -> "remoteRepositories " + remotes);
    
    var aether = Aether.create(resolverConf.mavenLocalRepositoryPath(), remoteRepositories);
    
    // mapping between module name and Maven artifact name
    var dependencies = resolverConf.dependencies();
    var moduleToArtifactMap = new LinkedHashMap<String, ArtifactQuery>();
    var artifactKeyToModuleMap = new LinkedHashMap<String, String>();
    for(var dependency: dependencies) {
      var index = dependency.indexOf('=');
      if (index == -1) {
        throw new IllegalStateException("invalid dependency format " + dependency);
      }
      var module = dependency.substring(0, index);
      var artifactCoords = dependency.substring(index + 1);
      
      var artifactQuery = aether.createArtifactQuery(artifactCoords);
      moduleToArtifactMap.put(module, artifactQuery);
      artifactKeyToModuleMap.put(artifactQuery.getArtifactKey(), module);
    }
    
    verifyDeclaration("modules", unresolvedModules, moduleToArtifactMap.keySet());
    
    var unresolvedRootArtifacts = unresolvedModules.stream()
        .map(moduleToArtifactMap::get)
        .collect(Collectors.toList());
    log.debug(unresolvedRootArtifacts, unresolvedRoots -> "unresolved root artifacts " + unresolvedRoots);
    
    var unresolvedArtifacts = new LinkedHashSet<ArtifactInfo>();
    for(var unresolvedRootArtifact: unresolvedRootArtifacts) {
      unresolvedArtifacts.addAll(aether.dependencies(unresolvedRootArtifact));  
    }
    log.debug(unresolvedArtifacts, unresolvedArtifactList -> "unresolved artifacts " + unresolvedArtifactList);
    
    verifyDeclaration("artifacts",
        unresolvedArtifacts.stream().map(ArtifactInfo::getArtifactKey).collect(Collectors.toSet()),
        artifactKeyToModuleMap.keySet());
    
    var resolvedArtifacts = aether.download(new ArrayList<>(unresolvedArtifacts));
    
    log.info(resolvedArtifacts, resolvedArtifactList -> "resolved artifacts " + resolvedArtifactList);
    
    var moduleDependencyPath = resolverConf.moduleDependencyPath().get(0);
    Files.createDirectories(moduleDependencyPath);
    
    var undeclaredArtifactIds = new ArrayList<ArtifactDescriptor>();
    for(var resolvedArtifact: resolvedArtifacts) {
      var moduleName = artifactKeyToModuleMap.get(resolvedArtifact.getArtifactKey().toString());
      if (moduleName == null) {
        undeclaredArtifactIds.add(resolvedArtifact);
      } else {
        log.info(null, __ -> moduleName + " (" + resolvedArtifact + ") downloaded from repositories");
        checkArtifactModuleName(log, moduleName, resolvedArtifact);
        Files.copy(resolvedArtifact.getPath(), moduleDependencyPath.resolve(moduleName + ".jar"));
      }
    }
    if (!undeclaredArtifactIds.isEmpty()) {
      throw new IllegalStateException("no dependency declared for Maven artifacts " + undeclaredArtifactIds);
    }
    
    return 0;
  }

  private static void verifyDeclaration(String kind, Set<String> unresolvedModules, Set<String> dependencySet) {
    var undeclaredModules = new ArrayList<String>();
    for(var module: unresolvedModules) {
      if (!dependencySet.contains(module)) {
        undeclaredModules.add(module);
      }
    }
    if (!undeclaredModules.isEmpty()) {
      throw new IllegalStateException("no dependency declared for unresolved " + kind + " " + undeclaredModules);
    }
  }
  
  private static void checkArtifactModuleName(Log log, String moduleName, ArtifactDescriptor resolvedArtifact) {
    var finder = ModuleFinder.of(resolvedArtifact.getPath());
    var referenceOpt = finder.findAll().stream().findFirst();
    if (!referenceOpt.isPresent()) {
      log.info(null, __ -> "WARNING! artifact " + resolvedArtifact + " is not a valide jar");
      return;
    }
    var descriptor = referenceOpt.get().descriptor();
    if (descriptor.isAutomatic()) {
      return;
    }
    var artifactModuleName = descriptor.name();
    if (!artifactModuleName.equals(moduleName)) {
      log.info(null, __ -> "WARNING! artifact module name " + artifactModuleName + " (" + resolvedArtifact + ") declared in the module-info is different from declared module name " + moduleName);
    }
  }
}
