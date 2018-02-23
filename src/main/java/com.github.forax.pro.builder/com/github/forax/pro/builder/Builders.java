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
    ProBuilder currentDir(java.nio.file.Path currentDir);
    ProBuilder exitOnError(boolean exitOnError);
    ProBuilder pluginDir(java.nio.file.Path pluginDir);
  }
  
  public static final CompilerBuilder compiler =
    Pro.getOrUpdate("compiler", CompilerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface CompilerBuilder {
    CompilerBuilder verbose(boolean verbose);
    CompilerBuilder files(java.util.List<java.nio.file.Path> files);
    CompilerBuilder release(int release);
    CompilerBuilder upgradeModulePath(java.util.List<java.nio.file.Path> upgradeModulePath);
    CompilerBuilder moduleSourceResourcesPath(java.util.List<java.nio.file.Path> moduleSourceResourcesPath);
    CompilerBuilder moduleTestResourcesPath(java.util.List<java.nio.file.Path> moduleTestResourcesPath);
    CompilerBuilder moduleExplodedSourcePath(java.nio.file.Path moduleExplodedSourcePath);
    CompilerBuilder moduleExplodedTestPath(java.nio.file.Path moduleExplodedTestPath);
    CompilerBuilder lint(java.lang.String lint);
    CompilerBuilder moduleSourcePath(java.util.List<java.nio.file.Path> moduleSourcePath);
    CompilerBuilder moduleTestPath(java.util.List<java.nio.file.Path> moduleTestPath);
    CompilerBuilder modulePath(java.util.List<java.nio.file.Path> modulePath);
    CompilerBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
    CompilerBuilder moduleMergedTestPath(java.nio.file.Path moduleMergedTestPath);
    CompilerBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
    CompilerBuilder rootModules(java.util.List<java.lang.String> rootModules);
  }
  
  public static final DocerBuilder docer =
    Pro.getOrUpdate("docer", DocerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface DocerBuilder {
    DocerBuilder files(java.util.List<java.nio.file.Path> files);
    DocerBuilder upgradeModulePath(java.util.List<java.nio.file.Path> upgradeModulePath);
    DocerBuilder moduleSourcePath(java.util.List<java.nio.file.Path> moduleSourcePath);
    DocerBuilder modulePath(java.util.List<java.nio.file.Path> modulePath);
    DocerBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
    DocerBuilder generateTestDoc(boolean generateTestDoc);
    DocerBuilder quiet(boolean quiet);
    DocerBuilder html5(boolean html5);
    DocerBuilder moduleMergedTestPath(java.util.List<java.nio.file.Path> moduleMergedTestPath);
    DocerBuilder moduleDocSourcePath(java.nio.file.Path moduleDocSourcePath);
    DocerBuilder moduleDocTestPath(java.nio.file.Path moduleDocTestPath);
    DocerBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
    DocerBuilder rootModules(java.util.List<java.lang.String> rootModules);
  }
  
  public static final LinkerBuilder linker =
    Pro.getOrUpdate("linker", LinkerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface LinkerBuilder {
    LinkerBuilder serviceNames(java.util.List<java.lang.String> serviceNames);
    LinkerBuilder compressLevel(int compressLevel);
    LinkerBuilder stripDebug(boolean stripDebug);
    LinkerBuilder stripNativeCommands(boolean stripNativeCommands);
    LinkerBuilder includeSystemJMODs(boolean includeSystemJMODs);
    LinkerBuilder ignoreSigningInformation(boolean ignoreSigningInformation);
    LinkerBuilder systemModulePath(java.nio.file.Path systemModulePath);
    LinkerBuilder moduleArtifactSourcePath(java.nio.file.Path moduleArtifactSourcePath);
    LinkerBuilder launchers(java.util.List<java.lang.String> launchers);
    LinkerBuilder destination(java.nio.file.Path destination);
    LinkerBuilder modulePath(java.util.List<java.nio.file.Path> modulePath);
    LinkerBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
    LinkerBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
    LinkerBuilder rootModules(java.util.List<java.lang.String> rootModules);
  }
  
  public static final ModulefixerBuilder modulefixer =
    Pro.getOrUpdate("modulefixer", ModulefixerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface ModulefixerBuilder {
    ModulefixerBuilder force(boolean force);
    ModulefixerBuilder moduleDependencyFixerPath(java.nio.file.Path moduleDependencyFixerPath);
    ModulefixerBuilder additionalRequires(java.util.List<java.lang.String> additionalRequires);
    ModulefixerBuilder additionalUses(java.util.List<java.lang.String> additionalUses);
    ModulefixerBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
  }
  
  public static final PackagerBuilder packager =
    Pro.getOrUpdate("packager", PackagerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface PackagerBuilder {
    PackagerBuilder modules(java.util.List<java.lang.String> modules);
    PackagerBuilder moduleArtifactSourcePath(java.nio.file.Path moduleArtifactSourcePath);
    PackagerBuilder moduleExplodedSourcePath(java.util.List<java.nio.file.Path> moduleExplodedSourcePath);
    PackagerBuilder moduleExplodedTestPath(java.util.List<java.nio.file.Path> moduleExplodedTestPath);
    PackagerBuilder moduleSourcePath(java.util.List<java.nio.file.Path> moduleSourcePath);
    PackagerBuilder moduleTestPath(java.util.List<java.nio.file.Path> moduleTestPath);
    PackagerBuilder moduleDocSourcePath(java.nio.file.Path moduleDocSourcePath);
    PackagerBuilder moduleDocTestPath(java.nio.file.Path moduleDocTestPath);
    PackagerBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
    PackagerBuilder generateSourceTestBale(boolean generateSourceTestBale);
    PackagerBuilder moduleArtifactTestPath(java.nio.file.Path moduleArtifactTestPath);
    PackagerBuilder moduleDocArtifactSourcePath(java.nio.file.Path moduleDocArtifactSourcePath);
    PackagerBuilder moduleSrcArtifactSourcePath(java.nio.file.Path moduleSrcArtifactSourcePath);
    PackagerBuilder moduleDocArtifactTestPath(java.nio.file.Path moduleDocArtifactTestPath);
    PackagerBuilder moduleSrcArtifactTestPath(java.nio.file.Path moduleSrcArtifactTestPath);
    @Deprecated
    PackagerBuilder moduleMetadata(java.util.List<java.lang.String> moduleMetadata);
  }
  
  public static final ResolverBuilder resolver =
    Pro.getOrUpdate("resolver", ResolverBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface ResolverBuilder {
    ResolverBuilder moduleSourcePath(java.util.List<java.nio.file.Path> moduleSourcePath);
    ResolverBuilder moduleTestPath(java.util.List<java.nio.file.Path> moduleTestPath);
    ResolverBuilder modulePath(java.util.List<java.nio.file.Path> modulePath);
    ResolverBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
    ResolverBuilder remoteRepositories(java.util.List<java.net.URI> remoteRepositories);
    ResolverBuilder mavenLocalRepositoryPath(java.nio.file.Path mavenLocalRepositoryPath);
    ResolverBuilder dependencies(java.util.List<java.lang.String> dependencies);
  }
  
  public static final RunnerBuilder runner =
    Pro.getOrUpdate("runner", RunnerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface RunnerBuilder {
    RunnerBuilder module(java.lang.String module);
    RunnerBuilder upgradeModulePath(java.util.List<java.nio.file.Path> upgradeModulePath);
    RunnerBuilder modulePath(java.util.List<java.nio.file.Path> modulePath);
    RunnerBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
    RunnerBuilder rootModules(java.util.List<java.lang.String> rootModules);
    RunnerBuilder javaCommand(java.nio.file.Path javaCommand);
    RunnerBuilder mainArguments(java.util.List<java.lang.String> mainArguments);
  }
  
  public static final TesterBuilder tester =
    Pro.getOrUpdate("tester", TesterBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface TesterBuilder {
    TesterBuilder timeout(int timeout);
    TesterBuilder pluginDir(java.nio.file.Path pluginDir);
    TesterBuilder moduleExplodedTestPath(java.util.List<java.nio.file.Path> moduleExplodedTestPath);
    TesterBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
  }
  
}
