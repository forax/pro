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
    ProBuilder pluginDir(java.nio.file.Path pluginDir);
    ProBuilder currentDir(java.nio.file.Path currentDir);
    ProBuilder exitOnError(boolean exitOnError);
  }
  
  public static final CompilerBuilder compiler =
    Pro.getOrUpdate("compiler", CompilerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface CompilerBuilder {
    CompilerBuilder upgradeModulePath(java.util.List<java.nio.file.Path> upgradeModulePath);
    CompilerBuilder files(java.util.List<java.nio.file.Path> files);
    CompilerBuilder release(int release);
    CompilerBuilder verbose(boolean verbose);
    CompilerBuilder moduleSourcePath(java.util.List<java.nio.file.Path> moduleSourcePath);
    CompilerBuilder moduleTestPath(java.util.List<java.nio.file.Path> moduleTestPath);
    CompilerBuilder modulePath(java.util.List<java.nio.file.Path> modulePath);
    CompilerBuilder rootModules(java.util.List<java.lang.String> rootModules);
    CompilerBuilder lint(java.lang.String lint);
    CompilerBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
    CompilerBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
    CompilerBuilder moduleSourceResourcesPath(java.util.List<java.nio.file.Path> moduleSourceResourcesPath);
    CompilerBuilder moduleTestResourcesPath(java.util.List<java.nio.file.Path> moduleTestResourcesPath);
    CompilerBuilder moduleMergedTestPath(java.nio.file.Path moduleMergedTestPath);
    CompilerBuilder moduleExplodedTestPath(java.nio.file.Path moduleExplodedTestPath);
    CompilerBuilder moduleExplodedSourcePath(java.nio.file.Path moduleExplodedSourcePath);
  }
  
  public static final LinkerBuilder linker =
    Pro.getOrUpdate("linker", LinkerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface LinkerBuilder {
    LinkerBuilder serviceNames(java.util.List<java.lang.String> serviceNames);
    LinkerBuilder modulePath(java.util.List<java.nio.file.Path> modulePath);
    LinkerBuilder compressLevel(int compressLevel);
    LinkerBuilder stripDebug(boolean stripDebug);
    LinkerBuilder includeSystemJMODs(boolean includeSystemJMODs);
    LinkerBuilder rootModules(java.util.List<java.lang.String> rootModules);
    LinkerBuilder launchers(java.util.List<java.lang.String> launchers);
    LinkerBuilder systemModulePath(java.nio.file.Path systemModulePath);
    LinkerBuilder destination(java.nio.file.Path destination);
    LinkerBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
    LinkerBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
    LinkerBuilder stripNativeCommands(boolean stripNativeCommands);
    LinkerBuilder ignoreSigningInformation(boolean ignoreSigningInformation);
    LinkerBuilder moduleArtifactSourcePath(java.nio.file.Path moduleArtifactSourcePath);
  }
  
  public static final ModulefixerBuilder modulefixer =
    Pro.getOrUpdate("modulefixer", ModulefixerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface ModulefixerBuilder {
    ModulefixerBuilder force(boolean force);
    ModulefixerBuilder additionalRequires(java.util.List<java.lang.String> additionalRequires);
    ModulefixerBuilder additionalUses(java.util.List<java.lang.String> additionalUses);
    ModulefixerBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
    ModulefixerBuilder moduleDependencyFixerPath(java.nio.file.Path moduleDependencyFixerPath);
  }
  
  public static final PackagerBuilder packager =
    Pro.getOrUpdate("packager", PackagerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface PackagerBuilder {
    PackagerBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
    PackagerBuilder moduleArtifactSourcePath(java.nio.file.Path moduleArtifactSourcePath);
    PackagerBuilder moduleExplodedTestPath(java.util.List<java.nio.file.Path> moduleExplodedTestPath);
    PackagerBuilder moduleExplodedSourcePath(java.util.List<java.nio.file.Path> moduleExplodedSourcePath);
    PackagerBuilder moduleArtifactTestPath(java.nio.file.Path moduleArtifactTestPath);
    PackagerBuilder moduleMetadata(java.util.List<java.lang.String> moduleMetadata);
  }
  
  public static final ResolverBuilder resolver =
    Pro.getOrUpdate("resolver", ResolverBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface ResolverBuilder {
    ResolverBuilder remoteRepositories(java.util.List<java.net.URI> remoteRepositories);
    ResolverBuilder dependencies(java.util.List<java.lang.String> dependencies);
    ResolverBuilder moduleSourcePath(java.util.List<java.nio.file.Path> moduleSourcePath);
    ResolverBuilder moduleTestPath(java.util.List<java.nio.file.Path> moduleTestPath);
    ResolverBuilder modulePath(java.util.List<java.nio.file.Path> modulePath);
    ResolverBuilder mavenLocalRepositoryPath(java.nio.file.Path mavenLocalRepositoryPath);
    ResolverBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
  }
  
  public static final RunnerBuilder runner =
    Pro.getOrUpdate("runner", RunnerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface RunnerBuilder {
    RunnerBuilder upgradeModulePath(java.util.List<java.nio.file.Path> upgradeModulePath);
    RunnerBuilder module(java.lang.String module);
    RunnerBuilder javaCommand(java.nio.file.Path javaCommand);
    RunnerBuilder modulePath(java.util.List<java.nio.file.Path> modulePath);
    RunnerBuilder rootModules(java.util.List<java.lang.String> rootModules);
    RunnerBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
    RunnerBuilder mainArguments(java.util.List<java.lang.String> mainArguments);
  }
  
  public static final TesterBuilder tester =
    Pro.getOrUpdate("tester", TesterBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface TesterBuilder {
    TesterBuilder timeout(int timeout);
    TesterBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
    TesterBuilder pluginDir(java.nio.file.Path pluginDir);
    TesterBuilder moduleExplodedTestPath(java.util.List<java.nio.file.Path> moduleExplodedTestPath);
  }
  
}
