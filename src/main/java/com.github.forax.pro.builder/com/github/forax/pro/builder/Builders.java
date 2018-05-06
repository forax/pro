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
    java.util.List<java.lang.String> arguments();
    ProBuilder arguments(java.util.List<java.lang.String> arguments);
    java.nio.file.Path currentDir();
    ProBuilder currentDir(java.nio.file.Path currentDir);
    boolean exitOnError();
    ProBuilder exitOnError(boolean exitOnError);
    java.lang.String loglevel();
    ProBuilder loglevel(java.lang.String loglevel);
    java.nio.file.Path pluginDir();
    ProBuilder pluginDir(java.nio.file.Path pluginDir);
  }
  
  public static final CompilerBuilder compiler =
    Pro.getOrUpdate("compiler", CompilerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface CompilerBuilder {
    java.util.Optional<java.util.List<java.nio.file.Path>> files();
    CompilerBuilder files(java.util.List<java.nio.file.Path> files);
    java.util.Optional<java.lang.String> lint();
    CompilerBuilder lint(java.lang.String lint);
    java.util.List<java.nio.file.Path> moduleDependencyPath();
    CompilerBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
    java.nio.file.Path moduleExplodedSourcePath();
    CompilerBuilder moduleExplodedSourcePath(java.nio.file.Path moduleExplodedSourcePath);
    java.nio.file.Path moduleExplodedTestPath();
    CompilerBuilder moduleExplodedTestPath(java.nio.file.Path moduleExplodedTestPath);
    java.nio.file.Path moduleMergedTestPath();
    CompilerBuilder moduleMergedTestPath(java.nio.file.Path moduleMergedTestPath);
    java.util.Optional<java.util.List<java.nio.file.Path>> modulePath();
    CompilerBuilder modulePath(java.util.List<java.nio.file.Path> modulePath);
    java.util.List<java.nio.file.Path> moduleSourcePath();
    CompilerBuilder moduleSourcePath(java.util.List<java.nio.file.Path> moduleSourcePath);
    java.util.List<java.nio.file.Path> moduleSourceResourcesPath();
    CompilerBuilder moduleSourceResourcesPath(java.util.List<java.nio.file.Path> moduleSourceResourcesPath);
    java.util.List<java.nio.file.Path> moduleTestPath();
    CompilerBuilder moduleTestPath(java.util.List<java.nio.file.Path> moduleTestPath);
    java.util.List<java.nio.file.Path> moduleTestResourcesPath();
    CompilerBuilder moduleTestResourcesPath(java.util.List<java.nio.file.Path> moduleTestResourcesPath);
    java.util.Optional<java.util.List<java.lang.String>> rawArguments();
    CompilerBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
    int release();
    CompilerBuilder release(int release);
    java.util.Optional<java.util.List<java.lang.String>> rootModules();
    CompilerBuilder rootModules(java.util.List<java.lang.String> rootModules);
    java.util.Optional<java.util.List<java.nio.file.Path>> upgradeModulePath();
    CompilerBuilder upgradeModulePath(java.util.List<java.nio.file.Path> upgradeModulePath);
    java.util.Optional<java.lang.Boolean> verbose();
    CompilerBuilder verbose(boolean verbose);
  }
  
  public static final DocerBuilder docer =
    Pro.getOrUpdate("docer", DocerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface DocerBuilder {
    java.util.Optional<java.util.List<java.nio.file.Path>> files();
    DocerBuilder files(java.util.List<java.nio.file.Path> files);
    boolean generateTestDoc();
    DocerBuilder generateTestDoc(boolean generateTestDoc);
    boolean html5();
    DocerBuilder html5(boolean html5);
    java.util.Optional<java.net.URI> link();
    DocerBuilder link(java.net.URI link);
    java.util.List<java.nio.file.Path> moduleDependencyPath();
    DocerBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
    java.nio.file.Path moduleDocSourcePath();
    DocerBuilder moduleDocSourcePath(java.nio.file.Path moduleDocSourcePath);
    java.nio.file.Path moduleDocTestPath();
    DocerBuilder moduleDocTestPath(java.nio.file.Path moduleDocTestPath);
    java.util.List<java.nio.file.Path> moduleMergedTestPath();
    DocerBuilder moduleMergedTestPath(java.util.List<java.nio.file.Path> moduleMergedTestPath);
    java.util.Optional<java.util.List<java.nio.file.Path>> modulePath();
    DocerBuilder modulePath(java.util.List<java.nio.file.Path> modulePath);
    java.util.List<java.nio.file.Path> moduleSourcePath();
    DocerBuilder moduleSourcePath(java.util.List<java.nio.file.Path> moduleSourcePath);
    boolean quiet();
    DocerBuilder quiet(boolean quiet);
    java.util.Optional<java.util.List<java.lang.String>> rawArguments();
    DocerBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
    java.util.Optional<java.util.List<java.lang.String>> rootModules();
    DocerBuilder rootModules(java.util.List<java.lang.String> rootModules);
    java.util.Optional<java.util.List<java.nio.file.Path>> upgradeModulePath();
    DocerBuilder upgradeModulePath(java.util.List<java.nio.file.Path> upgradeModulePath);
  }
  
  public static final FormatterBuilder formatter =
    Pro.getOrUpdate("formatter", FormatterBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface FormatterBuilder {
    boolean dryRun();
    FormatterBuilder dryRun(boolean dryRun);
    java.util.Optional<java.util.List<java.nio.file.Path>> files();
    FormatterBuilder files(java.util.List<java.nio.file.Path> files);
    java.util.List<java.nio.file.Path> moduleSourcePath();
    FormatterBuilder moduleSourcePath(java.util.List<java.nio.file.Path> moduleSourcePath);
    java.util.List<java.nio.file.Path> moduleTestPath();
    FormatterBuilder moduleTestPath(java.util.List<java.nio.file.Path> moduleTestPath);
    java.util.Optional<java.util.List<java.lang.String>> rawArguments();
    FormatterBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
    boolean replace();
    FormatterBuilder replace(boolean replace);
    boolean setExitIfChanged();
    FormatterBuilder setExitIfChanged(boolean setExitIfChanged);
  }
  
  public static final LinkerBuilder linker =
    Pro.getOrUpdate("linker", LinkerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface LinkerBuilder {
    int compressLevel();
    LinkerBuilder compressLevel(int compressLevel);
    java.nio.file.Path destination();
    LinkerBuilder destination(java.nio.file.Path destination);
    boolean ignoreSigningInformation();
    LinkerBuilder ignoreSigningInformation(boolean ignoreSigningInformation);
    boolean includeSystemJMODs();
    LinkerBuilder includeSystemJMODs(boolean includeSystemJMODs);
    java.util.Optional<java.util.List<java.lang.String>> launchers();
    LinkerBuilder launchers(java.util.List<java.lang.String> launchers);
    java.nio.file.Path moduleArtifactSourcePath();
    LinkerBuilder moduleArtifactSourcePath(java.nio.file.Path moduleArtifactSourcePath);
    java.util.List<java.nio.file.Path> moduleDependencyPath();
    LinkerBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
    java.util.Optional<java.util.List<java.nio.file.Path>> modulePath();
    LinkerBuilder modulePath(java.util.List<java.nio.file.Path> modulePath);
    java.util.Optional<java.util.List<java.lang.String>> rawArguments();
    LinkerBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
    java.util.Optional<java.util.List<java.lang.String>> rootModules();
    LinkerBuilder rootModules(java.util.List<java.lang.String> rootModules);
    java.util.Optional<java.util.List<java.lang.String>> serviceNames();
    LinkerBuilder serviceNames(java.util.List<java.lang.String> serviceNames);
    boolean stripDebug();
    LinkerBuilder stripDebug(boolean stripDebug);
    boolean stripNativeCommands();
    LinkerBuilder stripNativeCommands(boolean stripNativeCommands);
    java.nio.file.Path systemModulePath();
    LinkerBuilder systemModulePath(java.nio.file.Path systemModulePath);
  }
  
  public static final ModulefixerBuilder modulefixer =
    Pro.getOrUpdate("modulefixer", ModulefixerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface ModulefixerBuilder {
    java.util.Optional<java.util.List<java.lang.String>> additionalRequires();
    ModulefixerBuilder additionalRequires(java.util.List<java.lang.String> additionalRequires);
    java.util.Optional<java.util.List<java.lang.String>> additionalUses();
    ModulefixerBuilder additionalUses(java.util.List<java.lang.String> additionalUses);
    boolean force();
    ModulefixerBuilder force(boolean force);
    java.nio.file.Path moduleDependencyFixerPath();
    ModulefixerBuilder moduleDependencyFixerPath(java.nio.file.Path moduleDependencyFixerPath);
    java.util.List<java.nio.file.Path> moduleDependencyPath();
    ModulefixerBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
  }
  
  public static final PackagerBuilder packager =
    Pro.getOrUpdate("packager", PackagerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface PackagerBuilder {
    boolean generateSourceTestBale();
    PackagerBuilder generateSourceTestBale(boolean generateSourceTestBale);
    java.nio.file.Path moduleArtifactSourcePath();
    PackagerBuilder moduleArtifactSourcePath(java.nio.file.Path moduleArtifactSourcePath);
    java.nio.file.Path moduleArtifactTestPath();
    PackagerBuilder moduleArtifactTestPath(java.nio.file.Path moduleArtifactTestPath);
    java.nio.file.Path moduleDocArtifactSourcePath();
    PackagerBuilder moduleDocArtifactSourcePath(java.nio.file.Path moduleDocArtifactSourcePath);
    java.nio.file.Path moduleDocArtifactTestPath();
    PackagerBuilder moduleDocArtifactTestPath(java.nio.file.Path moduleDocArtifactTestPath);
    java.nio.file.Path moduleDocSourcePath();
    PackagerBuilder moduleDocSourcePath(java.nio.file.Path moduleDocSourcePath);
    java.nio.file.Path moduleDocTestPath();
    PackagerBuilder moduleDocTestPath(java.nio.file.Path moduleDocTestPath);
    java.util.List<java.nio.file.Path> moduleExplodedSourcePath();
    PackagerBuilder moduleExplodedSourcePath(java.util.List<java.nio.file.Path> moduleExplodedSourcePath);
    java.util.List<java.nio.file.Path> moduleExplodedTestPath();
    PackagerBuilder moduleExplodedTestPath(java.util.List<java.nio.file.Path> moduleExplodedTestPath);
    @Deprecated
    java.util.Optional<java.util.List<java.lang.String>> moduleMetadata();
    @Deprecated
    PackagerBuilder moduleMetadata(java.util.List<java.lang.String> moduleMetadata);
    java.util.List<java.nio.file.Path> moduleSourcePath();
    PackagerBuilder moduleSourcePath(java.util.List<java.nio.file.Path> moduleSourcePath);
    java.nio.file.Path moduleSrcArtifactSourcePath();
    PackagerBuilder moduleSrcArtifactSourcePath(java.nio.file.Path moduleSrcArtifactSourcePath);
    java.nio.file.Path moduleSrcArtifactTestPath();
    PackagerBuilder moduleSrcArtifactTestPath(java.nio.file.Path moduleSrcArtifactTestPath);
    java.util.List<java.nio.file.Path> moduleTestPath();
    PackagerBuilder moduleTestPath(java.util.List<java.nio.file.Path> moduleTestPath);
    java.util.Optional<java.util.List<java.lang.String>> modules();
    PackagerBuilder modules(java.util.List<java.lang.String> modules);
    java.util.Optional<java.util.List<java.lang.String>> rawArguments();
    PackagerBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
  }
  
  public static final PerferBuilder perfer =
    Pro.getOrUpdate("perfer", PerferBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface PerferBuilder {
    java.nio.file.Path javaCommand();
    PerferBuilder javaCommand(java.nio.file.Path javaCommand);
    java.nio.file.Path moduleArtifactTestPath();
    PerferBuilder moduleArtifactTestPath(java.nio.file.Path moduleArtifactTestPath);
    java.util.List<java.nio.file.Path> moduleDependencyPath();
    PerferBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
  }
  
  public static final ResolverBuilder resolver =
    Pro.getOrUpdate("resolver", ResolverBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface ResolverBuilder {
    boolean checkForUpdate();
    ResolverBuilder checkForUpdate(boolean checkForUpdate);
    java.util.List<java.lang.String> dependencies();
    ResolverBuilder dependencies(java.util.List<java.lang.String> dependencies);
    java.nio.file.Path mavenLocalRepositoryPath();
    ResolverBuilder mavenLocalRepositoryPath(java.nio.file.Path mavenLocalRepositoryPath);
    java.util.List<java.nio.file.Path> moduleDependencyPath();
    ResolverBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
    java.util.Optional<java.util.List<java.nio.file.Path>> modulePath();
    ResolverBuilder modulePath(java.util.List<java.nio.file.Path> modulePath);
    java.util.List<java.nio.file.Path> moduleSourcePath();
    ResolverBuilder moduleSourcePath(java.util.List<java.nio.file.Path> moduleSourcePath);
    java.util.List<java.nio.file.Path> moduleTestPath();
    ResolverBuilder moduleTestPath(java.util.List<java.nio.file.Path> moduleTestPath);
    java.util.Optional<java.util.List<java.net.URI>> remoteRepositories();
    ResolverBuilder remoteRepositories(java.util.List<java.net.URI> remoteRepositories);
  }
  
  public static final RunnerBuilder runner =
    Pro.getOrUpdate("runner", RunnerBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface RunnerBuilder {
    java.nio.file.Path javaCommand();
    RunnerBuilder javaCommand(java.nio.file.Path javaCommand);
    java.util.Optional<java.util.List<java.lang.String>> mainArguments();
    RunnerBuilder mainArguments(java.util.List<java.lang.String> mainArguments);
    java.util.Optional<java.lang.String> module();
    RunnerBuilder module(java.lang.String module);
    java.util.List<java.nio.file.Path> modulePath();
    RunnerBuilder modulePath(java.util.List<java.nio.file.Path> modulePath);
    java.util.Optional<java.util.List<java.lang.String>> rawArguments();
    RunnerBuilder rawArguments(java.util.List<java.lang.String> rawArguments);
    java.util.Optional<java.util.List<java.lang.String>> rootModules();
    RunnerBuilder rootModules(java.util.List<java.lang.String> rootModules);
    java.util.Optional<java.util.List<java.nio.file.Path>> upgradeModulePath();
    RunnerBuilder upgradeModulePath(java.util.List<java.nio.file.Path> upgradeModulePath);
  }
  
  public static final TesterBuilder tester =
    Pro.getOrUpdate("tester", TesterBuilder.class);
  
  @SuppressWarnings("exports")
  @com.github.forax.pro.api.TypeCheckedConfig
  public interface TesterBuilder {
    java.util.List<java.nio.file.Path> moduleDependencyPath();
    TesterBuilder moduleDependencyPath(java.util.List<java.nio.file.Path> moduleDependencyPath);
    java.util.List<java.nio.file.Path> moduleExplodedTestPath();
    TesterBuilder moduleExplodedTestPath(java.util.List<java.nio.file.Path> moduleExplodedTestPath);
    java.nio.file.Path pluginDir();
    TesterBuilder pluginDir(java.nio.file.Path pluginDir);
    int timeout();
    TesterBuilder timeout(int timeout);
  }
  
}
