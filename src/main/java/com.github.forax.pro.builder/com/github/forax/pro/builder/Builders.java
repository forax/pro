package com.github.forax.pro.builder;

import com.github.forax.pro.Pro;

// THIS CLASS IS GENERATED, DO NOT EDIT
// see GenBuilder.java and Bootstrap.java, if you want to re-generate it
public class Builders {
  public static final ProBuilder pro =
    Pro.getOrUpdate("pro", ProBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface ProBuilder {
    ProBuilder loglevel(java.lang.String loglevel);
    java.lang.String loglevel();
    java.nio.file.Path currentDir();
    ProBuilder currentDir(java.nio.file.Path currentDir);
    boolean exitOnError();
    ProBuilder exitOnError(boolean exitOnError);
    ProBuilder pluginDir(java.nio.file.Path pluginDir);
    java.nio.file.Path pluginDir();
    java.util.List<java.lang.String> arguments();
    ProBuilder arguments(java.util.List<java.lang.String> arguments);
  }
  
  public static final CompilerBuilder compiler =
    Pro.getOrUpdate("compiler", CompilerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface CompilerBuilder {
    java.util.Optional<java.lang.Boolean> verbose();
    CompilerBuilder verbose(boolean verbose);
    java.util.Optional<java.util.List<java.nio.file.Path>> upgradeModulePath();
    CompilerBuilder upgradeModulePath(java.util.List<java.nio.file.Path> upgradeModulePath);
    CompilerBuilder moduleMergedTestPath(java.nio.file.Path moduleMergedTestPath);
    java.nio.file.Path moduleMergedTestPath();
    CompilerBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
    java.util.Optional<java.util.List<java.lang.String>> rawArguments();
    CompilerBuilder rootModules(java.util.List<java.lang.String> rootModules);
    java.util.Optional<java.util.List<java.lang.String>> rootModules();
    CompilerBuilder moduleSourcePath(java.util.List<java.nio.file.Path> moduleSourcePath);
    java.util.List<java.nio.file.Path> moduleSourcePath();
    CompilerBuilder moduleTestPath(java.util.List<java.nio.file.Path> moduleTestPath);
    java.util.List<java.nio.file.Path> moduleTestPath();
    CompilerBuilder modulePath(java.util.List<java.nio.file.Path> modulePath);
    java.util.Optional<java.util.List<java.nio.file.Path>> modulePath();
    java.util.List<java.nio.file.Path> moduleDependencyPath();
    CompilerBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
    java.util.List<java.nio.file.Path> moduleSourceResourcesPath();
    CompilerBuilder moduleSourceResourcesPath(java.util.List<java.nio.file.Path> moduleSourceResourcesPath);
    CompilerBuilder moduleTestResourcesPath(java.util.List<java.nio.file.Path> moduleTestResourcesPath);
    java.util.List<java.nio.file.Path> moduleTestResourcesPath();
    java.nio.file.Path moduleExplodedSourcePath();
    CompilerBuilder moduleExplodedSourcePath(java.nio.file.Path moduleExplodedSourcePath);
    CompilerBuilder moduleExplodedTestPath(java.nio.file.Path moduleExplodedTestPath);
    java.nio.file.Path moduleExplodedTestPath();
    java.util.Optional<java.lang.String> lint();
    CompilerBuilder lint(java.lang.String lint);
    java.util.Optional<java.util.List<java.nio.file.Path>> files();
    CompilerBuilder files(java.util.List<java.nio.file.Path> files);
    CompilerBuilder release(int release);
    int release();
  }
  
  public static final DocerBuilder docer =
    Pro.getOrUpdate("docer", DocerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface DocerBuilder {
    java.util.Optional<java.net.URI> link();
    DocerBuilder link(java.net.URI link);
    DocerBuilder upgradeModulePath(java.util.List<java.nio.file.Path> upgradeModulePath);
    java.util.Optional<java.util.List<java.nio.file.Path>> upgradeModulePath();
    boolean generateTestDoc();
    DocerBuilder generateTestDoc(boolean generateTestDoc);
    DocerBuilder quiet(boolean quiet);
    boolean quiet();
    DocerBuilder html5(boolean html5);
    boolean html5();
    DocerBuilder moduleMergedTestPath(java.util.List<java.nio.file.Path> moduleMergedTestPath);
    java.util.List<java.nio.file.Path> moduleMergedTestPath();
    java.nio.file.Path moduleDocSourcePath();
    DocerBuilder moduleDocSourcePath(java.nio.file.Path moduleDocSourcePath);
    java.nio.file.Path moduleDocTestPath();
    DocerBuilder moduleDocTestPath(java.nio.file.Path moduleDocTestPath);
    DocerBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
    java.util.Optional<java.util.List<java.lang.String>> rawArguments();
    DocerBuilder rootModules(java.util.List<java.lang.String> rootModules);
    java.util.Optional<java.util.List<java.lang.String>> rootModules();
    DocerBuilder moduleSourcePath(java.util.List<java.nio.file.Path> moduleSourcePath);
    java.util.List<java.nio.file.Path> moduleSourcePath();
    java.util.Optional<java.util.List<java.nio.file.Path>> modulePath();
    DocerBuilder modulePath(java.util.List<java.nio.file.Path> modulePath);
    java.util.List<java.nio.file.Path> moduleDependencyPath();
    DocerBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
    java.util.Optional<java.util.List<java.nio.file.Path>> files();
    DocerBuilder files(java.util.List<java.nio.file.Path> files);
  }
  
  public static final LinkerBuilder linker =
    Pro.getOrUpdate("linker", LinkerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface LinkerBuilder {
    java.util.Optional<java.util.List<java.lang.String>> serviceNames();
    LinkerBuilder serviceNames(java.util.List<java.lang.String> serviceNames);
    java.util.Optional<java.util.List<java.lang.String>> rawArguments();
    LinkerBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
    java.util.Optional<java.util.List<java.lang.String>> rootModules();
    LinkerBuilder rootModules(java.util.List<java.lang.String> rootModules);
    LinkerBuilder modulePath(java.util.List<java.nio.file.Path> modulePath);
    java.util.Optional<java.util.List<java.nio.file.Path>> modulePath();
    java.util.List<java.nio.file.Path> moduleDependencyPath();
    LinkerBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
    LinkerBuilder compressLevel(int compressLevel);
    int compressLevel();
    boolean stripDebug();
    LinkerBuilder stripDebug(boolean stripDebug);
    boolean stripNativeCommands();
    LinkerBuilder stripNativeCommands(boolean stripNativeCommands);
    boolean includeSystemJMODs();
    LinkerBuilder includeSystemJMODs(boolean includeSystemJMODs);
    boolean ignoreSigningInformation();
    LinkerBuilder ignoreSigningInformation(boolean ignoreSigningInformation);
    java.nio.file.Path systemModulePath();
    LinkerBuilder systemModulePath(java.nio.file.Path systemModulePath);
    LinkerBuilder moduleArtifactSourcePath(java.nio.file.Path moduleArtifactSourcePath);
    java.nio.file.Path moduleArtifactSourcePath();
    java.util.Optional<java.util.List<java.lang.String>> launchers();
    LinkerBuilder launchers(java.util.List<java.lang.String> launchers);
    LinkerBuilder destination(java.nio.file.Path destination);
    java.nio.file.Path destination();
  }
  
  public static final ModulefixerBuilder modulefixer =
    Pro.getOrUpdate("modulefixer", ModulefixerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface ModulefixerBuilder {
    ModulefixerBuilder force(boolean force);
    boolean force();
    java.util.List<java.nio.file.Path> moduleDependencyPath();
    ModulefixerBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
    ModulefixerBuilder moduleDependencyFixerPath(java.nio.file.Path moduleDependencyFixerPath);
    java.nio.file.Path moduleDependencyFixerPath();
    ModulefixerBuilder additionalRequires(java.util.List<java.lang.String> additionalRequires);
    java.util.Optional<java.util.List<java.lang.String>> additionalRequires();
    java.util.Optional<java.util.List<java.lang.String>> additionalUses();
    ModulefixerBuilder additionalUses(java.util.List<java.lang.String> additionalUses);
  }
  
  public static final PackagerBuilder packager =
    Pro.getOrUpdate("packager", PackagerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface PackagerBuilder {
    PackagerBuilder moduleDocSourcePath(java.nio.file.Path moduleDocSourcePath);
    java.nio.file.Path moduleDocSourcePath();
    java.nio.file.Path moduleDocTestPath();
    PackagerBuilder moduleDocTestPath(java.nio.file.Path moduleDocTestPath);
    java.util.Optional<java.util.List<java.lang.String>> rawArguments();
    PackagerBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
    PackagerBuilder moduleSourcePath(java.util.List<java.nio.file.Path> moduleSourcePath);
    java.util.List<java.nio.file.Path> moduleSourcePath();
    PackagerBuilder moduleTestPath(java.util.List<java.nio.file.Path> moduleTestPath);
    java.util.List<java.nio.file.Path> moduleTestPath();
    java.util.List<java.nio.file.Path> moduleExplodedSourcePath();
    PackagerBuilder moduleExplodedSourcePath(java.util.List<java.nio.file.Path> moduleExplodedSourcePath);
    PackagerBuilder moduleExplodedTestPath(java.util.List<java.nio.file.Path> moduleExplodedTestPath);
    java.util.List<java.nio.file.Path> moduleExplodedTestPath();
    PackagerBuilder moduleArtifactSourcePath(java.nio.file.Path moduleArtifactSourcePath);
    java.nio.file.Path moduleArtifactSourcePath();
    boolean generateSourceTestBale();
    PackagerBuilder generateSourceTestBale(boolean generateSourceTestBale);
    java.nio.file.Path moduleArtifactTestPath();
    PackagerBuilder moduleArtifactTestPath(java.nio.file.Path moduleArtifactTestPath);
    PackagerBuilder moduleDocArtifactSourcePath(java.nio.file.Path moduleDocArtifactSourcePath);
    java.nio.file.Path moduleDocArtifactSourcePath();
    PackagerBuilder moduleSrcArtifactSourcePath(java.nio.file.Path moduleSrcArtifactSourcePath);
    java.nio.file.Path moduleSrcArtifactSourcePath();
    PackagerBuilder moduleDocArtifactTestPath(java.nio.file.Path moduleDocArtifactTestPath);
    java.nio.file.Path moduleDocArtifactTestPath();
    PackagerBuilder moduleSrcArtifactTestPath(java.nio.file.Path moduleSrcArtifactTestPath);
    java.nio.file.Path moduleSrcArtifactTestPath();
    @Deprecated
    java.util.Optional<java.util.List<java.lang.String>> moduleMetadata();
    @Deprecated
    PackagerBuilder moduleMetadata(java.util.List<java.lang.String> moduleMetadata);
    java.util.Optional<java.util.List<java.lang.String>> modules();
    PackagerBuilder modules(java.util.List<java.lang.String> modules);
  }
  
  public static final PerferBuilder perfer =
    Pro.getOrUpdate("perfer", PerferBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface PerferBuilder {
    PerferBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
    java.util.List<java.nio.file.Path> moduleDependencyPath();
    java.nio.file.Path moduleArtifactTestPath();
    PerferBuilder moduleArtifactTestPath(java.nio.file.Path moduleArtifactTestPath);
    java.nio.file.Path javaCommand();
    PerferBuilder javaCommand(java.nio.file.Path javaCommand);
  }
  
  public static final ResolverBuilder resolver =
    Pro.getOrUpdate("resolver", ResolverBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface ResolverBuilder {
    java.util.List<java.nio.file.Path> moduleSourcePath();
    ResolverBuilder moduleSourcePath(java.util.List<java.nio.file.Path> moduleSourcePath);
    ResolverBuilder moduleTestPath(java.util.List<java.nio.file.Path> moduleTestPath);
    java.util.List<java.nio.file.Path> moduleTestPath();
    java.util.Optional<java.util.List<java.nio.file.Path>> modulePath();
    ResolverBuilder modulePath(java.util.List<java.nio.file.Path> modulePath);
    java.util.List<java.nio.file.Path> moduleDependencyPath();
    ResolverBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
    ResolverBuilder remoteRepositories(java.util.List<java.net.URI> remoteRepositories);
    java.util.Optional<java.util.List<java.net.URI>> remoteRepositories();
    java.nio.file.Path mavenLocalRepositoryPath();
    ResolverBuilder mavenLocalRepositoryPath(java.nio.file.Path mavenLocalRepositoryPath);
    ResolverBuilder dependencies(java.util.List<java.lang.String> dependencies);
    java.util.List<java.lang.String> dependencies();
  }
  
  public static final RunnerBuilder runner =
    Pro.getOrUpdate("runner", RunnerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface RunnerBuilder {
    RunnerBuilder upgradeModulePath(java.util.List<java.nio.file.Path> upgradeModulePath);
    java.util.Optional<java.util.List<java.nio.file.Path>> upgradeModulePath();
    RunnerBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
    java.util.Optional<java.util.List<java.lang.String>> rawArguments();
    java.util.Optional<java.util.List<java.lang.String>> rootModules();
    RunnerBuilder rootModules(java.util.List<java.lang.String> rootModules);
    RunnerBuilder modulePath(java.util.List<java.nio.file.Path> modulePath);
    java.util.List<java.nio.file.Path> modulePath();
    RunnerBuilder mainArguments(java.util.List<java.lang.String> mainArguments);
    java.util.Optional<java.util.List<java.lang.String>> mainArguments();
    RunnerBuilder javaCommand(java.nio.file.Path javaCommand);
    java.nio.file.Path javaCommand();
    java.util.Optional<java.lang.String> module();
    RunnerBuilder module(java.lang.String module);
  }
  
  public static final TesterBuilder tester =
    Pro.getOrUpdate("tester", TesterBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface TesterBuilder {
    java.nio.file.Path pluginDir();
    TesterBuilder pluginDir(java.nio.file.Path pluginDir);
    java.util.List<java.nio.file.Path> moduleDependencyPath();
    TesterBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
    TesterBuilder moduleExplodedTestPath(java.util.List<java.nio.file.Path> moduleExplodedTestPath);
    java.util.List<java.nio.file.Path> moduleExplodedTestPath();
    TesterBuilder timeout(int timeout);
    int timeout();
  }
  
}
