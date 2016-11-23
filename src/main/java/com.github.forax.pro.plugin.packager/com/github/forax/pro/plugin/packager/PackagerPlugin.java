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
import com.github.forax.pro.api.helper.CmdLine;
import com.github.forax.pro.api.helper.OptionAction;
import com.github.forax.pro.helper.FileHelper;
import com.github.forax.pro.plugin.packager.MetadataParser.Metadata;

public class PackagerPlugin implements Plugin {
  @Override
  public String name() {
    return "packager";
  }

  @Override
  public void init(MutableConfig config) {
    config.getOrUpdate(name(), Packager.class);
  }
  
  @Override
  public void configure(MutableConfig config) {
    Packager packager = config.getOrUpdate(name(), Packager.class);
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class); 
    packager.moduleArtifactSourcePath(convention.javaModuleArtifactSourcePath());
    packager.moduleExplodedSourcePath(convention.javaModuleExplodedSourcePath());
    packager.moduleArtifactTestPath(convention.javaModuleArtifactTestPath());
    packager.moduleExplodedTestPath(convention.javaModuleExplodedTestPath());
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
    //System.out.println("execute " + config);
    
    ToolProvider jarTool = ToolProvider.findFirst("jar")
        .orElseThrow(() -> new IllegalStateException("can not find jar"));
    Packager packager = config.getOrThrow(name(), Packager.class);
    
    List<Path> moduleExplodedSourcePath = FileHelper.pathFromFilesThatExist(packager.moduleExplodedSourcePath());
    Path moduleArtifactSourcePath = packager.moduleArtifactSourcePath();
    List<Path> moduleExplodedTestPath = FileHelper.pathFromFilesThatExist(packager.moduleExplodedTestPath());
    Path moduleArtifactTestPath = packager.moduleArtifactTestPath();
    
    Map<String, Metadata> metadataMap = packager.moduleMetadata().map(MetadataParser::parse).orElse(Map.of());
    
    int errorCode = packageModules(jarTool, moduleExplodedSourcePath, moduleArtifactSourcePath, metadataMap, "");
    if (errorCode != 0) {
      return errorCode;
    }
    return packageModules(jarTool, moduleExplodedTestPath, moduleArtifactTestPath, metadataMap, "test-");
  }

  private static int packageModules(ToolProvider jarTool, List<Path> moduleExplodedPath, Path moduleArtifactPath, Map<String, Metadata> metadataMap, String prefix) throws IOException {
    FileHelper.deleteAllFiles(moduleArtifactPath);
    Files.createDirectories(moduleArtifactPath);
    
    for(Path explodedPath: moduleExplodedPath) {
      try(DirectoryStream<Path> directoryStream = Files.newDirectoryStream(explodedPath)) {
        for(Path moduleExploded: directoryStream) {
          int exitCode = packageModule(jarTool, moduleExploded, moduleArtifactPath, metadataMap, prefix);
          if (exitCode != 0) {
            return exitCode;
          }
        }
      }
    }
    return 0;
  }

  private static int packageModule(ToolProvider jarTool, Path moduleExploded,  Path moduleArtifact, Map<String, Metadata> metadataMap, String prefix) {
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
    
    //System.out.println("jar " + String.join(" ", arguments));
    int exitCode = jarTool.run(System.out, System.err, arguments);
    return exitCode;
  }
}
