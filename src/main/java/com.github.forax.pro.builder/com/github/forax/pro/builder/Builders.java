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
    public ProBuilder loglevel(java.lang.String loglevel);
    public ProBuilder currentDir(java.nio.file.Path currentDir);
    public ProBuilder exitOnError(boolean exitOnError);
    public ProBuilder pluginDir(java.nio.file.Path pluginDir);
  }
  
  public static final CompilerBuilder compiler =
    Pro.getOrUpdate("compiler", CompilerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface CompilerBuilder {
    public CompilerBuilder verbose(boolean verbose);
    public CompilerBuilder files(java.util.List<java.nio.file.Path> files);
    public CompilerBuilder release(int release);
    public CompilerBuilder upgradeModulePath(java.util.List<java.nio.file.Path> upgradeModulePath);
    public CompilerBuilder rootModules(java.util.List<java.lang.String> rootModules);
    public CompilerBuilder moduleSourceResourcesPath(java.util.List<java.nio.file.Path> moduleSourceResourcesPath);
    public CompilerBuilder moduleTestResourcesPath(java.util.List<java.nio.file.Path> moduleTestResourcesPath);
    public CompilerBuilder moduleExplodedSourcePath(java.nio.file.Path moduleExplodedSourcePath);
    public CompilerBuilder moduleMergedTestPath(java.nio.file.Path moduleMergedTestPath);
    public CompilerBuilder moduleExplodedTestPath(java.nio.file.Path moduleExplodedTestPath);
    public CompilerBuilder lint(java.lang.String lint);
    public CompilerBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
    public CompilerBuilder moduleSourcePath(java.util.List<java.nio.file.Path> moduleSourcePath);
    public CompilerBuilder moduleTestPath(java.util.List<java.nio.file.Path> moduleTestPath);
    public CompilerBuilder modulePath(java.util.List<java.nio.file.Path> modulePath);
    public CompilerBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
  }
  
  public static final LinkerBuilder linker =
    Pro.getOrUpdate("linker", LinkerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface LinkerBuilder {
    public LinkerBuilder serviceNames(java.util.List<java.lang.String> serviceNames);
    public LinkerBuilder compressLevel(int compressLevel);
    public LinkerBuilder stripDebug(boolean stripDebug);
    public LinkerBuilder stripNativeCommands(boolean stripNativeCommands);
    public LinkerBuilder includeSystemJMODs(boolean includeSystemJMODs);
    public LinkerBuilder ignoreSigningInformation(boolean ignoreSigningInformation);
    public LinkerBuilder systemModulePath(java.nio.file.Path systemModulePath);
    public LinkerBuilder moduleArtifactSourcePath(java.nio.file.Path moduleArtifactSourcePath);
    public LinkerBuilder rootModules(java.util.List<java.lang.String> rootModules);
    public LinkerBuilder launchers(java.util.List<java.lang.String> launchers);
    public LinkerBuilder destination(java.nio.file.Path destination);
    public LinkerBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
    public LinkerBuilder modulePath(java.util.List<java.nio.file.Path> modulePath);
    public LinkerBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
  }
  
  public static final ModulefixerBuilder modulefixer =
    Pro.getOrUpdate("modulefixer", ModulefixerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface ModulefixerBuilder {
    public ModulefixerBuilder force(boolean force);
    public ModulefixerBuilder moduleDependencyFixerPath(java.nio.file.Path moduleDependencyFixerPath);
    public ModulefixerBuilder additionalRequires(java.util.List<java.lang.String> additionalRequires);
    public ModulefixerBuilder additionalUses(java.util.List<java.lang.String> additionalUses);
    public ModulefixerBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
  }
  
  public static final PackagerBuilder packager =
    Pro.getOrUpdate("packager", PackagerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface PackagerBuilder {
    public PackagerBuilder moduleArtifactSourcePath(java.nio.file.Path moduleArtifactSourcePath);
    public PackagerBuilder moduleExplodedSourcePath(java.util.List<java.nio.file.Path> moduleExplodedSourcePath);
    public PackagerBuilder moduleExplodedTestPath(java.util.List<java.nio.file.Path> moduleExplodedTestPath);
    public PackagerBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
    public PackagerBuilder moduleArtifactTestPath(java.nio.file.Path moduleArtifactTestPath);
    public PackagerBuilder moduleMetadata(java.util.List<java.lang.String> moduleMetadata);
  }
  
  public static final ResolverBuilder resolver =
    Pro.getOrUpdate("resolver", ResolverBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface ResolverBuilder {
    public ResolverBuilder moduleSourcePath(java.util.List<java.nio.file.Path> moduleSourcePath);
    public ResolverBuilder moduleTestPath(java.util.List<java.nio.file.Path> moduleTestPath);
    public ResolverBuilder modulePath(java.util.List<java.nio.file.Path> modulePath);
    public ResolverBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
    public ResolverBuilder remoteRepositories(java.util.List<java.net.URI> remoteRepositories);
    public ResolverBuilder mavenLocalRepositoryPath(java.nio.file.Path mavenLocalRepositoryPath);
    public ResolverBuilder dependencies(java.util.List<java.lang.String> dependencies);
  }
  
  public static final RunnerBuilder runner =
    Pro.getOrUpdate("runner", RunnerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface RunnerBuilder {
    public RunnerBuilder javaCommand(java.nio.file.Path javaCommand);
    public RunnerBuilder module(java.lang.String module);
    public RunnerBuilder upgradeModulePath(java.util.List<java.nio.file.Path> upgradeModulePath);
    public RunnerBuilder rootModules(java.util.List<java.lang.String> rootModules);
    public RunnerBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
    public RunnerBuilder modulePath(java.util.List<java.nio.file.Path> modulePath);
    public RunnerBuilder mainArguments(java.util.List<java.lang.String> mainArguments);
  }
  
  public static final TesterBuilder tester =
    Pro.getOrUpdate("tester", TesterBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface TesterBuilder {
    public TesterBuilder timeout(int timeout);
    public TesterBuilder pluginDir(java.nio.file.Path pluginDir);
    public TesterBuilder moduleExplodedTestPath(java.util.List<java.nio.file.Path> moduleExplodedTestPath);
    public TesterBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
  }
  
}
