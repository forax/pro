package com.github.forax.pro.plugin.packager;

import static com.github.forax.pro.api.helper.OptionAction.action;
import static com.github.forax.pro.api.helper.OptionAction.actionMaybe;

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
    config.getOrUpdate(name(), PackagerConf.class);
  }
  
  @Override
  public void configure(MutableConfig config) {
    PackagerConf packager = config.getOrUpdate(name(), PackagerConf.class);
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class); 
    
    // inputs
    packager.moduleArtifactSourcePath(convention.javaModuleArtifactSourcePath());
    packager.moduleArtifactTestPath(convention.javaModuleArtifactTestPath());
    
    // outputs
    packager.moduleExplodedSourcePath(convention.javaModuleExplodedSourcePath());
    packager.moduleExplodedTestPath(convention.javaModuleExplodedTestPath());
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
    
    Map<String, Metadata> metadataMap = packager.moduleMetadata().map(MetadataParser::parse).orElse(Map.of());
    
    int errorCode = packageModules(log, jarTool, moduleExplodedSourcePath, moduleArtifactSourcePath, metadataMap, "");
    if (errorCode != 0) {
      return errorCode;
    }
    if (!moduleExplodedTestPath.stream().anyMatch(Files::exists)) {
      return 0;
    }
    return packageModules(log, jarTool, moduleExplodedTestPath, moduleArtifactTestPath, metadataMap, "test-");
  }

  private static int packageModules(Log log, ToolProvider jarTool, List<Path> moduleExplodedPath, Path moduleArtifactPath, Map<String, Metadata> metadataMap, String prefix) throws IOException {
    FileHelper.deleteAllFiles(moduleArtifactPath, false);
    Files.createDirectories(moduleArtifactPath);
    
    for(Path explodedPath: moduleExplodedPath) {
      try(DirectoryStream<Path> directoryStream = Files.newDirectoryStream(explodedPath)) {
        for(Path moduleExploded: directoryStream) {
          int exitCode = packageModule(log, jarTool, moduleExploded, moduleArtifactPath, metadataMap, prefix);
          if (exitCode != 0) {
            return exitCode;
          }
        }
      }
    }
    return 0;
  }

  private static int packageModule(Log log, ToolProvider jarTool, Path moduleExploded,  Path moduleArtifact, Map<String, Metadata> metadataMap, String prefix) {
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
    
    CmdLine cmdLine = new CmdLine().add("--create");
    cmdLine = OptionAction.gatherAll(JarOption.class, option -> option.action).apply(jar, cmdLine);
    String[] arguments = cmdLine.add(".").toArguments();
    
    log.verbose(jar, _jar -> OptionAction.toPrettyString(JarOption.class, option -> option.action).apply(_jar, "jar"));
    int exitCode = jarTool.run(System.out, System.err, arguments);
    return exitCode;
  }
}
