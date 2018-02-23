package com.github.forax.pro.plugin.packager;

import static com.github.forax.pro.api.helper.OptionAction.action;
import static com.github.forax.pro.api.helper.OptionAction.actionMaybe;
import static com.github.forax.pro.api.helper.OptionAction.rawValues;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.spi.ToolProvider;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;
import com.github.forax.pro.api.helper.CmdLine;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.api.helper.OptionAction;
import com.github.forax.pro.helper.FileHelper;
import com.github.forax.pro.helper.Log;
import com.github.forax.pro.plugin.packager.MetadataParser.Metadata;

public class PackagerPlugin implements Plugin {
  @Override
  public String name() {
    return "packager";
  }

  @Override
  public void init(MutableConfig config) {
    PackagerConf packager = config.getOrUpdate(name(), PackagerConf.class);
    packager.generateSourceTestBale(false);
  }
  
  @Override
  public void configure(MutableConfig config) {
    PackagerConf packager = config.getOrUpdate(name(), PackagerConf.class);
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class); 
    
    // inputs
    MutableConfig.derive(packager, PackagerConf::moduleSourcePath, convention, ConventionFacade::javaModuleSourcePath);
    MutableConfig.derive(packager, PackagerConf::moduleTestPath, convention, ConventionFacade::javaModuleTestPath);
    MutableConfig.derive(packager, PackagerConf::moduleExplodedSourcePath, convention, ConventionFacade::javaModuleExplodedSourcePath);
    MutableConfig.derive(packager, PackagerConf::moduleExplodedTestPath, convention, ConventionFacade::javaModuleExplodedTestPath);
    MutableConfig.derive(packager, PackagerConf::moduleDocSourcePath, convention, ConventionFacade::javaModuleDocSourcePath);
    MutableConfig.derive(packager, PackagerConf::moduleDocTestPath, convention, ConventionFacade::javaModuleDocTestPath);
    
    // outputs
    MutableConfig.derive(packager, PackagerConf::moduleArtifactSourcePath, convention, ConventionFacade::javaModuleArtifactSourcePath);
    MutableConfig.derive(packager, PackagerConf::moduleArtifactTestPath, convention, ConventionFacade::javaModuleArtifactTestPath);
    MutableConfig.derive(packager, PackagerConf::moduleSrcArtifactSourcePath, convention, ConventionFacade::javaModuleSrcArtifactSourcePath);
    MutableConfig.derive(packager, PackagerConf::moduleDocArtifactSourcePath, convention, ConventionFacade::javaModuleDocArtifactSourcePath);
    MutableConfig.derive(packager, PackagerConf::moduleSrcArtifactTestPath, convention, ConventionFacade::javaModuleSrcArtifactTestPath);
    MutableConfig.derive(packager, PackagerConf::moduleDocArtifactTestPath, convention, ConventionFacade::javaModuleDocArtifactTestPath);
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
    PackagerConf packager = config.getOrThrow(name(), PackagerConf.class);
    packager.moduleExplodedSourcePath().forEach(registry::watch);
    packager.moduleExplodedTestPath().forEach(registry::watch);
  }
  
  enum JarOption {
    FILE(action("--file", Jar::getOutput)),
    VERSION(actionMaybe("--module-version", Jar::getModuleVersion)),
    MAIN_CLASS(actionMaybe("--main-class", Jar::getMainClass)),
    RAW_ARGUMENTS(rawValues(Jar::rawArguments)),
    C(action("-C", Jar::getInput))
    ;
    
    final OptionAction<Jar> action;
    
    private JarOption(OptionAction<Jar> action) {
      this.action = action;
    }
  }
  
  @Override
  public int execute(Config config) throws IOException {
    Log log = Log.create(name(), config.getOrThrow("pro", ProConf.class).loglevel());
    log.debug(config, conf -> "config " + config);
    
    ToolProvider jarTool = ToolProvider.findFirst("jar")
        .orElseThrow(() -> new IllegalStateException("can not find jar"));
    PackagerConf packager = config.getOrThrow(name(), PackagerConf.class);
    
    List<Path> moduleExplodedSourcePath = FileHelper.pathFromFilesThatExist(packager.moduleExplodedSourcePath());
    Path moduleArtifactSourcePath = packager.moduleArtifactSourcePath();
    List<Path> moduleExplodedTestPath = FileHelper.pathFromFilesThatExist(packager.moduleExplodedTestPath());
    Path moduleArtifactTestPath = packager.moduleArtifactTestPath();
    
    @SuppressWarnings("deprecation")
    Optional<List<String>> modules = packager.modules().or(() -> packager.moduleMetadata());
    Map<String, Metadata> metadataMap = modules.map(MetadataParser::parse).orElse(Map.of());
    
    int errorCode = packageAll(moduleExplodedSourcePath, moduleArtifactSourcePath,
        (input, output) -> packageModule(log, jarTool, input, output, packager, metadataMap, ""));
    if (errorCode != 0) {
      return errorCode;
    }
    
    if (Files.exists(packager.moduleDocSourcePath())) {
      errorCode = packageAll(List.of(packager.moduleDocSourcePath()), packager.moduleDocArtifactSourcePath(),
          (input, output) -> packageSourceOrDoc(log, jarTool, input, output, packager, metadataMap, "doc"));
      if (errorCode != 0) {
        return errorCode;
      }
    }
    errorCode = packageAll(FileHelper.pathFromFilesThatExist(packager.moduleSourcePath()), packager.moduleSrcArtifactSourcePath(),
        (input, output) -> packageSourceOrDoc(log, jarTool, input, output, packager, metadataMap, "source"));
    if (errorCode != 0) {
      return errorCode;
    } 
    if (!moduleExplodedTestPath.stream().anyMatch(Files::exists)) {
      return 0;
    }
    errorCode = packageAll(moduleExplodedTestPath, moduleArtifactTestPath,
        (input, output) -> packageModule(log, jarTool, input, output, packager, metadataMap, "test-"));
    if (errorCode != 0) {
      return errorCode;
    }
    if (Files.exists(packager.moduleDocTestPath())) {
      errorCode = packageAll(List.of(packager.moduleDocTestPath()), packager.moduleDocArtifactTestPath(),
          (input, output) -> packageSourceOrDoc(log, jarTool, input, output, packager, metadataMap, "test-doc"));
      if (errorCode != 0) {
        return errorCode;
      }
    }
    if (packager.generateSourceTestBale()) { 
      packageAll(FileHelper.pathFromFilesThatExist(packager.moduleTestPath()), packager.moduleSrcArtifactTestPath(),
          (input, output) -> packageSourceOrDoc(log, jarTool, input, output, packager, metadataMap, "test-source"));
    }
    return 0;
  }

  interface Action {
    int apply(Path input, Path output);
  }
  
  private static int packageAll(List<Path> inputs, Path output, Action action) throws IOException {
    FileHelper.deleteAllFiles(output, false);
    Files.createDirectories(output);
    
    for(Path path: inputs) {
      try(DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
        for(Path input: directoryStream) {
          int exitCode = action.apply(input, output);
          if (exitCode != 0) {
            return exitCode;
          }
        }
      }
    }
    return 0;
  }

  private static int packageModule(Log log, ToolProvider jarTool, Path moduleExploded,  Path moduleArtifact, PackagerConf packager, Map<String, Metadata> metadataMap, String prefix) {
    Set<ModuleReference> set = ModuleFinder.of(moduleExploded).findAll();
    if (set.size() != 1) {
      throw new IllegalStateException("more than one module packaged in the exploded module " + moduleExploded);
    }
    
    String moduleName = set.iterator().next().descriptor().name(); 
    
    Optional<Metadata> metadata = Optional.ofNullable(metadataMap.get(moduleName));
    String version = metadata.flatMap(Metadata::version).orElse("1.0");
    Jar jar = new Jar(moduleExploded, moduleArtifact.resolve(prefix + moduleName + "-" + version + ".jar"));
    jar.setModuleVersion(version);
    metadata.flatMap(Metadata::mainClass).ifPresent(jar::setMainClass);
    packager.rawArguments().ifPresent(jar::rawArguments);
    
    CmdLine cmdLine = new CmdLine().add("--create");
    cmdLine = OptionAction.gatherAll(JarOption.class, option -> option.action).apply(jar, cmdLine);
    String[] arguments = cmdLine.add(".").toArguments();
    
    log.verbose(jar, _jar -> OptionAction.toPrettyString(JarOption.class, option -> option.action).apply(_jar, "jar"));
    int exitCode = jarTool.run(System.out, System.err, arguments);
    return exitCode;
  }
  
  private static int packageSourceOrDoc(Log log, ToolProvider jarTool, Path input, Path outputPath, PackagerConf packager, Map<String, Metadata> metadataMap, String suffix) {
    String moduleName = input.getFileName().toString();
    
    Optional<Metadata> metadata = Optional.ofNullable(metadataMap.get(moduleName));
    String version = metadata.flatMap(Metadata::version).orElse("1.0");
    
    Jar jar = new Jar(input, outputPath.resolve(moduleName + "-" + suffix + "-" + version + ".jar"));
    packager.rawArguments().ifPresent(jar::rawArguments);
    
    CmdLine cmdLine = new CmdLine().add("--create");
    cmdLine = OptionAction.gatherAll(JarOption.class, option -> option.action).apply(jar, cmdLine);
    String[] arguments = cmdLine.add(".").toArguments();
    
    log.verbose(jar, _jar -> OptionAction.toPrettyString(JarOption.class, option -> option.action).apply(_jar, "jar"));
    int exitCode = jarTool.run(System.out, System.err, arguments);
    return exitCode;
  }
}
