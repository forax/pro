package com.github.forax.pro.plugin.packager;

import static com.github.forax.pro.api.helper.OptionAction.action;
import static com.github.forax.pro.api.helper.OptionAction.actionMaybe;
import static com.github.forax.pro.api.helper.OptionAction.rawValues;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    var packagerConf = config.getOrUpdate(name(), PackagerConf.class);
    packagerConf.generateSourceTestBale(false);
  }
  
  @Override
  public void configure(MutableConfig config) {
    var packagerConf = config.getOrUpdate(name(), PackagerConf.class);
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class); 
    
    // inputs
    MutableConfig.derive(packagerConf, PackagerConf::moduleSourcePath, convention, ConventionFacade::javaModuleSourcePath);
    MutableConfig.derive(packagerConf, PackagerConf::moduleTestPath, convention, ConventionFacade::javaModuleTestPath);
    MutableConfig.derive(packagerConf, PackagerConf::moduleExplodedSourcePath, convention, ConventionFacade::javaModuleExplodedSourcePath);
    MutableConfig.derive(packagerConf, PackagerConf::moduleExplodedTestPath, convention, ConventionFacade::javaModuleExplodedTestPath);
    MutableConfig.derive(packagerConf, PackagerConf::moduleDocSourcePath, convention, ConventionFacade::javaModuleDocSourcePath);
    MutableConfig.derive(packagerConf, PackagerConf::moduleDocTestPath, convention, ConventionFacade::javaModuleDocTestPath);
    
    // outputs
    MutableConfig.derive(packagerConf, PackagerConf::moduleArtifactSourcePath, convention, ConventionFacade::javaModuleArtifactSourcePath);
    MutableConfig.derive(packagerConf, PackagerConf::moduleArtifactTestPath, convention, ConventionFacade::javaModuleArtifactTestPath);
    MutableConfig.derive(packagerConf, PackagerConf::moduleSrcArtifactSourcePath, convention, ConventionFacade::javaModuleSrcArtifactSourcePath);
    MutableConfig.derive(packagerConf, PackagerConf::moduleDocArtifactSourcePath, convention, ConventionFacade::javaModuleDocArtifactSourcePath);
    MutableConfig.derive(packagerConf, PackagerConf::moduleSrcArtifactTestPath, convention, ConventionFacade::javaModuleSrcArtifactTestPath);
    MutableConfig.derive(packagerConf, PackagerConf::moduleDocArtifactTestPath, convention, ConventionFacade::javaModuleDocArtifactTestPath);
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
    var packagerConf = config.getOrThrow(name(), PackagerConf.class);
    packagerConf.moduleExplodedSourcePath().forEach(registry::watch);
    packagerConf.moduleExplodedTestPath().forEach(registry::watch);
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
    var log = Log.create(name(), config.getOrThrow("pro", ProConf.class).loglevel());
    log.debug(config, conf -> "config " + config);
    
    var jarTool = ToolProvider.findFirst("jar").orElseThrow(() -> new IllegalStateException("can not find jar"));
    var packagerConf = config.getOrThrow(name(), PackagerConf.class);
    
    var moduleExplodedSourcePath = FileHelper.pathFromFilesThatExist(packagerConf.moduleExplodedSourcePath());
    var moduleArtifactSourcePath = packagerConf.moduleArtifactSourcePath();
    var moduleExplodedTestPath = FileHelper.pathFromFilesThatExist(packagerConf.moduleExplodedTestPath());
    var moduleArtifactTestPath = packagerConf.moduleArtifactTestPath();
    
    @SuppressWarnings("deprecation")
    var modules = packagerConf.modules().or(() -> packagerConf.moduleMetadata());
    var metadataMap = modules.map(MetadataParser::parse).orElse(Map.of());
    
    var errorCode = packageAll(moduleExplodedSourcePath, moduleArtifactSourcePath,
        (input, output) -> packageModule(log, jarTool, input, output, packagerConf, metadataMap, ""));
    if (errorCode != 0) {
      return errorCode;
    }
    
    if (Files.exists(packagerConf.moduleDocSourcePath())) {
      errorCode = packageAll(List.of(packagerConf.moduleDocSourcePath()), packagerConf.moduleDocArtifactSourcePath(),
          (input, output) -> packageSourceOrDoc(log, jarTool, input, output, packagerConf, metadataMap, "doc"));
      if (errorCode != 0) {
        return errorCode;
      }
    }
    errorCode = packageAll(FileHelper.pathFromFilesThatExist(packagerConf.moduleSourcePath()), packagerConf.moduleSrcArtifactSourcePath(),
        (input, output) -> packageSourceOrDoc(log, jarTool, input, output, packagerConf, metadataMap, "source"));
    if (errorCode != 0) {
      return errorCode;
    } 
    if (!moduleExplodedTestPath.stream().anyMatch(Files::exists)) {
      return 0;
    }
    errorCode = packageAll(moduleExplodedTestPath, moduleArtifactTestPath,
        (input, output) -> packageModule(log, jarTool, input, output, packagerConf, metadataMap, "test-"));
    if (errorCode != 0) {
      return errorCode;
    }
    if (Files.exists(packagerConf.moduleDocTestPath())) {
      errorCode = packageAll(List.of(packagerConf.moduleDocTestPath()), packagerConf.moduleDocArtifactTestPath(),
          (input, output) -> packageSourceOrDoc(log, jarTool, input, output, packagerConf, metadataMap, "test-doc"));
      if (errorCode != 0) {
        return errorCode;
      }
    }
    if (packagerConf.generateSourceTestBale()) { 
      packageAll(FileHelper.pathFromFilesThatExist(packagerConf.moduleTestPath()), packagerConf.moduleSrcArtifactTestPath(),
          (input, output) -> packageSourceOrDoc(log, jarTool, input, output, packagerConf, metadataMap, "test-source"));
    }
    return 0;
  }

  interface Action {
    int apply(Path input, Path output);
  }
  
  private static int packageAll(List<Path> inputs, Path output, Action action) throws IOException {
    FileHelper.deleteAllFiles(output, false);
    Files.createDirectories(output);
    
    for(var path: inputs) {
      try(var directoryStream = Files.newDirectoryStream(path)) {
        for(var input: directoryStream) {
          var exitCode = action.apply(input, output);
          if (exitCode != 0) {
            return exitCode;
          }
        }
      }
    }
    return 0;
  }

  private static int packageModule(Log log, ToolProvider jarTool, Path moduleExploded,  Path moduleArtifact, PackagerConf packager, Map<String, Metadata> metadataMap, String prefix) {
    var modules = ModuleFinder.of(moduleExploded).findAll();
    if (modules.size() != 1) {
      throw new IllegalStateException("more than one module packaged in the exploded module " + moduleExploded);
    }
    
    var moduleName = modules.iterator().next().descriptor().name(); 
    
    var metadataOpt = Optional.ofNullable(metadataMap.get(moduleName));
    var version = metadataOpt.flatMap(Metadata::version).orElse("1.0");
    var jar = new Jar(moduleExploded, moduleArtifact.resolve(prefix + moduleName + "-" + version + ".jar"));
    jar.setModuleVersion(version);
    metadataOpt.flatMap(Metadata::mainClass).ifPresent(jar::setMainClass);
    packager.rawArguments().ifPresent(jar::rawArguments);
    
    var cmdLine = new CmdLine().add("--create");
    cmdLine = OptionAction.gatherAll(JarOption.class, option -> option.action).apply(jar, cmdLine);
    var arguments = cmdLine.add(".").toArguments();
    
    log.verbose(jar, _jar -> OptionAction.toPrettyString(JarOption.class, option -> option.action).apply(_jar, "jar"));
    var exitCode = jarTool.run(System.out, System.err, arguments);
    return exitCode;
  }
  
  private static int packageSourceOrDoc(Log log, ToolProvider jarTool, Path input, Path outputPath, PackagerConf packager, Map<String, Metadata> metadataMap, String suffix) {
    var moduleName = input.getFileName().toString();
    
    var metadataOpt = Optional.ofNullable(metadataMap.get(moduleName));
    var version = metadataOpt.flatMap(Metadata::version).orElse("1.0");
    
    var jar = new Jar(input, outputPath.resolve(moduleName + "-" + suffix + "-" + version + ".jar"));
    packager.rawArguments().ifPresent(jar::rawArguments);
    
    var cmdLine = new CmdLine().add("--create");
    cmdLine = OptionAction.gatherAll(JarOption.class, option -> option.action).apply(jar, cmdLine);
    var arguments = cmdLine.add(".").toArguments();
    
    log.verbose(jar, _jar -> OptionAction.toPrettyString(JarOption.class, option -> option.action).apply(_jar, "jar"));
    var exitCode = jarTool.run(System.out, System.err, arguments);
    return exitCode;
  }
}
