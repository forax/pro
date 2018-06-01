package com.github.forax.pro.plugin.resolver;

import static com.github.forax.pro.api.MutableConfig.derive;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
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
import com.github.forax.pro.helper.ModuleHelper.ResolverListener;
import com.github.forax.pro.helper.util.StableList;

public class ResolverPlugin implements Plugin {
  @Override
  public String name() {
    return "resolver";
  }

  @Override
  public void init(MutableConfig config) {
    var resolverConf = config.getOrUpdate(name(), ResolverConf.class);
    resolverConf.checkForUpdate(false);
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
  
  
  private static boolean resolveModuleDependencies(ModuleFinder moduleFinder, ModuleFinder dependencyFinder, LinkedHashSet<String> foundModules, LinkedHashSet<String> unresolvedModules) {
    var rootSourceNames = moduleFinder.findAll().stream()
        .map(ref -> ref.descriptor().name())
        .collect(Collectors.toList());
    var allFinder = ModuleFinder.compose(moduleFinder, dependencyFinder, ModuleHelper.systemModulesFinder());
    return ModuleHelper.resolveOnlyRequires(allFinder, rootSourceNames,
        new ResolverListener() {
          @Override
          public void module(String moduleName) {
            foundModules.add(moduleName);
          }
          @Override
          public void dependencyNotFound(String moduleName, String dependencyChain) {
            unresolvedModules.add(moduleName);
          }
        });
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
    
    
    // find resolved and unresolved modules in dependencies (for source and test)
    var resolvedModules = new LinkedHashSet<String>();
    var unresolvedModules = new LinkedHashSet<String>();
    
    // source module names
    var moduleSourceFinder = ModuleHelper.sourceModuleFinders(resolverConf.moduleSourcePath());
    var sourceResolved = resolveModuleDependencies(moduleSourceFinder, dependencyFinder, resolvedModules, unresolvedModules);
    
    // test module names
    var moduleTestPath = FileHelper.pathFromFilesThatExist(resolverConf.moduleTestPath());
    if (!moduleTestPath.isEmpty()) {
      ModuleFinder moduleTestFinder = ModuleHelper.sourceModuleFinders(resolverConf.moduleTestPath());
      sourceResolved &= resolveModuleDependencies(moduleTestFinder, dependencyFinder, resolvedModules, unresolvedModules);
    }
    
    log.debug(unresolvedModules, unresolved -> "unresolvedModules " + unresolved);
    
    var remoteRepositories = resolverConf.remoteRepositories().orElse(List.of());
    log.debug(remoteRepositories, remotes -> "remoteRepositories " + remotes);
    
    var aether = Aether.create(resolverConf.mavenLocalRepositoryPath(), remoteRepositories);
    
    var depencenciesOpt = resolverConf.dependencies();
    
    // check if there are dependencies with new versions
    if (resolverConf.checkForUpdate() && depencenciesOpt.isPresent()) {
      try {
        parseResolverDependencies(depencenciesOpt.get(), aether, (module, artifactQuery) -> {
          if (resolvedModules.contains(module)) {  // if the module is referenced by a module-info
            var artifactKey = artifactQuery.getArtifactKey();
            ArtifactQuery queryMostRecent = aether.createArtifactQuery(artifactKey + ":[0,]");
            Set<ArtifactInfo> artifactInfos;
            try {
              artifactInfos = aether.dependencies(queryMostRecent);
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
            ArtifactInfo artifactInfo = artifactInfos.stream().filter(info -> info.getArtifactKey().equals(artifactKey)).findFirst().get();
            if (!artifactQuery.getArtifactCoords().equals(artifactInfo.getArtifactCoords())) {
              log.info(artifactInfo,
                  _artifactInfo ->  _artifactInfo.getArtifactKey() + " can be updated" +
                                    "\n     deps: " + artifactInfos.stream().map(ArtifactInfo::getArtifactCoords).collect(joining(", ")));
            }
          }
        });
      } catch( @SuppressWarnings("unused") UncheckedIOException e) {
        log.info(null, __ -> "io or network error, dependencies will not be checked for update");
      }
    }
    
    // everything is resolved, nothing to do
    if (sourceResolved) {
      return 0;
    }
    
    // does the dependencies are specified ? 
    if (!depencenciesOpt.isPresent()) {
      log.error(unresolvedModules, _depencenciesOpt -> "no dependencies specified but there are some unresolved modules " + unresolvedModules);
      return 1;
    }
    
    // create mapping between module name and Maven artifact name
    var moduleToArtifactMap = new LinkedHashMap<String, List<ArtifactQuery>>();
    var artifactKeyToModuleMap = new LinkedHashMap<String, String>();
    parseResolverDependencies(depencenciesOpt.get(), aether, (module, artifactQuery) -> {
      moduleToArtifactMap.computeIfAbsent(module, __ -> new ArrayList<>()).add(artifactQuery);
      artifactKeyToModuleMap.put(artifactQuery.getArtifactKey(), module);
    });
    
    verifyDeclaration("modules", unresolvedModules, moduleToArtifactMap.keySet());
    
    var unresolvedRootArtifacts = unresolvedModules.stream()
        .flatMap(unresolveModule -> moduleToArtifactMap.get(unresolveModule).stream())
        .collect(Collectors.toList());
    log.debug(unresolvedRootArtifacts, unresolvedRootArtifactList -> "unresolved root artifacts " + unresolvedRootArtifactList);
    
    // find all resolved artifacts
    var unresolvedArtifacts = new LinkedHashSet<ArtifactInfo>();
    for(var unresolvedRootArtifact: unresolvedRootArtifacts) {
      unresolvedArtifacts.addAll(aether.dependencies(unresolvedRootArtifact));  
    }
    
    // remove not resolvable artifacts
    resolverConf.dontResolve().map(deps -> StableList.from(deps).map(aether::createArtifactInfo)).ifPresent(unresolvedArtifacts::removeAll);
    
    log.debug(unresolvedArtifacts, unresolvedArtifactList -> "unresolved artifacts " + unresolvedArtifactList);
    
    verifyDeclaration("artifacts",
        unresolvedArtifacts.stream().map(ArtifactInfo::getArtifactKey).collect(Collectors.toSet()),
        artifactKeyToModuleMap.keySet());
    
    var resolvedArtifacts = aether.download(new ArrayList<>(unresolvedArtifacts));
    
    log.info(resolvedArtifacts, resolvedArtifactList -> "resolved artifacts " + resolvedArtifactList);
    
    var resolvedModuleMap = resolvedArtifacts.stream()
        .peek(resolvedArtifact -> log.info(resolvedArtifact, _resolvedArtifact -> _resolvedArtifact + " downloaded from repositories"))
        .collect(Collectors.groupingBy(resolvedArtifact -> artifactKeyToModuleMap.get(resolvedArtifact.getArtifactKey().toString())));
    
    var undeclaredArtifactIds = resolvedModuleMap.get(null);
    if (undeclaredArtifactIds != null) {  // resolved artifacts with no module name
      throw new IllegalStateException("no dependency declared for Maven artifacts " + undeclaredArtifactIds);
    }
    
    // copy the jars in the dependency folder, merge them if there is more than one jar for a module
    var moduleDependencyPath = resolverConf.moduleDependencyPath().get(0);
    Files.createDirectories(moduleDependencyPath);
    
    for(var entry: resolvedModuleMap.entrySet()) {
      var moduleName = entry.getKey();
      var resolvedArtifactList = entry.getValue();
      
      var jarPath = moduleDependencyPath.resolve(moduleName + ".jar");
      switch(resolvedArtifactList.size()) {
      case 1: {
        var resolvedArtifact = resolvedArtifactList.get(0);
        checkArtifactModuleName(log, moduleName, resolvedArtifact);
        Files.copy(resolvedArtifact.getPath(), jarPath);
        break;
      }
      default: 
        mergeInOneJar(resolvedArtifactList.stream().map(ArtifactDescriptor::getPath).collect(Collectors.toList()), jarPath, log);
      }
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
  
  private static void parseResolverDependencies(List<String> dependencies, Aether aether, BiConsumer<String, ArtifactQuery> listener) {
    for(var dependency: dependencies) {
      var index = dependency.indexOf('=');
      if (index == -1) {
        throw new IllegalStateException("invalid dependency format " + dependency + ", = not found");
      }
      var module = dependency.substring(0, index);
      var artifactNames = dependency.substring(index + 1);
      
      var artifactsCoords = artifactNames.split(",");
      if (artifactsCoords.length == 0) {
        throw new IllegalStateException("invalid dependency format " + dependency + ", empty Maven coords");
      }
      
      for(var artifactCoords: artifactsCoords) {
        var artifactQuery = aether.createArtifactQuery(artifactCoords);
        listener.accept(module, artifactQuery);
      }
    }
  }
  
  private static void mergeInOneJar(List<Path> artifactJars, Path jarPath, Log log) throws IOException {
    var duplicateEntryNames = new HashSet<String>();
    
    try(var fileOutput = Files.newOutputStream(jarPath, StandardOpenOption.CREATE_NEW);
        var jarOutput = new JarOutputStream(fileOutput)) { 
      for(var artifactJar: artifactJars) {
        try(var jarFile = new JarFile(artifactJar.toFile())) {
          for(var entry: (Iterable<JarEntry>)jarFile.entries()::asIterator) {
            var name = entry.getName();
            if (name.endsWith("module-info.class")) {
              throw new IOException("invalid jar " + artifactJar + ", only non modular jar can be merged");
            }
            if (duplicateEntryNames.add(name) == false) { // duplicate file
              if (!entry.isDirectory()) {
                log.info(name, _name -> jarPath + ": duplicate entry " + name + " skipped");
              }
              continue;
            }
            
            jarOutput.putNextEntry(new JarEntry(entry));
            jarFile.getInputStream(entry).transferTo(jarOutput);
            jarOutput.closeEntry();
          }
        }
      }
    }
  }
}
