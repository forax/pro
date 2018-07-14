package com.github.forax.pro.plugin.perfer;

import static com.github.forax.pro.api.MutableConfig.derive;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.openjdk.jmh.runner.BenchmarkList;
import org.openjdk.jmh.runner.BenchmarkListEntry;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.helper.Log;
import com.github.forax.pro.helper.ModuleHelper;
import com.github.forax.pro.helper.Platform;
import com.github.forax.pro.helper.util.StableList;

public class PerferPlugin implements Plugin {
  @Override
  public String name() {
    return "perfer";
  }

  @Override
  public void init(MutableConfig config) {
    config.getOrUpdate(name(), PerferConf.class);
  }
  
  @Override
  public void configure(MutableConfig config) {
    var perferConf = config.getOrUpdate(name(), PerferConf.class);
    var convention = config.getOrThrow("convention", ConventionFacade.class); 
    
    derive(perferConf, PerferConf::moduleArtifactTestPath, convention, ConventionFacade::javaModuleArtifactTestPath);
    derive(perferConf, PerferConf::moduleDependencyPath, convention, ConventionFacade::javaModuleDependencyPath);
    
    derive(perferConf, PerferConf::javaCommand, convention,
        c -> c.javaHome().resolve("bin").resolve(Platform.current().javaExecutableName()));
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
    var perferConf = config.getOrThrow(name(), PerferConf.class);
    perferConf.moduleArtifactTestPath().forEach(registry::watch);
    perferConf.moduleDependencyPath().forEach(registry::watch);
  }
  
  @Override
  public int execute(Config config) throws IOException {
    var log = Log.create(name(), config.getOrThrow("pro", ProConf.class).loglevel());
    var perfer = config.getOrThrow(name(), PerferConf.class);
    log.debug(config, conf -> "config " + perfer);
    
    var modulePath = StableList.<Path>of()
        .append(perfer.moduleArtifactTestPath())
        .appendAll(perfer.moduleDependencyPath());
    var javaCommand = perfer.javaCommand();
    
    var finder = ModuleFinder.of(perfer.moduleArtifactTestPath());
    for(var module :finder.findAll()) {
      int exitCode = module.location().map(Paths::get).map(testPath -> execute(log, testPath, modulePath, javaCommand)).orElse(0);
      if (exitCode != 0) {
        return exitCode;
      }
    }
    return 0;
  }
  
  private static int execute(Log log, Path testPath, List<Path> modulePath, Path javaCommand) {
    var moduleReference = ModuleHelper.getOnlyModule(testPath);
    var moduleName = moduleReference.descriptor().name();
    log.debug(moduleName, name -> "found test module " + name);
    
    try(var reader = moduleReference.open()) {
      var inputOpt = reader.open("META-INF/BenchmarkList");
      if (!inputOpt.isPresent()) {
        return 0;
      }
      
      var list = BenchmarkList.readBenchmarkList(inputOpt.get());
      var classNames = list.stream().map(BenchmarkListEntry::getUserClassQName).collect(toSet());
      log.debug(classNames, names -> "benchmarks " + classNames);
      
      for(var className: classNames) {
        var exitCode = executeClass(moduleName, className, modulePath, javaCommand);
        if (exitCode != 0) {
          return exitCode;
        }
      }
      return 0;
      
    } catch (IOException e) {
      log.error(e);
      return 1;
    }
  }
  
  private static int executeClass(String moduleName, String className, List<Path> modulePath, Path javaCommand) throws IOException {
    StableList<String> arguments = Stream.of(
        Stream.of("-XX:+EnableValhalla").filter(__ -> System.getProperty("valhalla.enableValhalla") != null),
        Stream.of("--module-path", modulePath.stream().map(Path::toString).collect(joining(":"))),
        Stream.of("-m", moduleName + '/' + className)
        )
      .flatMap(s -> s)
      .collect(StableList.toStableList());
    
    var process = new ProcessBuilder(StableList.of(javaCommand.toString()).appendAll(arguments))
                .redirectErrorStream(true).start();

    process.getInputStream().transferTo(System.out);

    try {
      return process.waitFor();
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }
}
