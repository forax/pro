package com.github.forax.pro.plugin.tester;

import static com.github.forax.pro.api.MutableConfig.derive;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.helper.Log;
import com.github.forax.pro.helper.ModuleHelper;

public class TesterPlugin implements Plugin {
  @Override
  public String name() {
    return "tester";
  }

  @Override
  public void init(MutableConfig config) {
    config.getOrUpdate(name(), TesterConf.class);
  }

  @Override
  public void configure(MutableConfig config) {
    TesterConf tester = config.getOrUpdate(name(), TesterConf.class);
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class);

    // inputs
    derive(tester, TesterConf::moduleExplodedTestPath, convention, ConventionFacade::javaModuleExplodedTestPath);
  }

  @Override
  public void watch(Config config, WatcherRegistry registry) {
    TesterConf testerConf = config.getOrThrow(name(), TesterConf.class);
    testerConf.moduleExplodedTestPath().forEach(registry::watch);
  }

  private static List<Path> directories(List<Path> paths) {
    return ModuleFinder.of(paths.toArray(new Path[0]))
        .findAll()
        .stream()
        .flatMap(ref -> ref.location().stream())
        .map(Paths::get)
        .collect(Collectors.toList());
  }

  @Override
  public int execute(Config config) {
    ProConf proConf = config.getOrThrow("pro", ProConf.class);
    Log log = Log.create(name(), proConf.loglevel());
    TesterConf tester = config.getOrThrow(name(), TesterConf.class);
    log.debug(tester, _tester -> "config " + _tester);
    
    Path pluginDir = proConf.pluginDir();

    int exitCodeSum = 0;
    for (Path path : directories(tester.moduleExplodedTestPath())) {
      exitCodeSum += execute(pluginDir, path.toAbsolutePath().normalize());
    }
    return exitCodeSum;
  }

  private int execute(Path pluginDir, Path testPath) {
    ExecutorService executor = Executors.newSingleThreadExecutor();

    ModuleReference moduleReference = ModuleHelper.getOnlyModule(testPath);
    String moduleName = moduleReference.descriptor().name();
    ClassLoader testClassLoader = createTestClassLoader(pluginDir, testPath, moduleName);
    try {
      Class<?> runnerClass = testClassLoader.loadClass(TesterRunner.class.getName());
      IntSupplier runner = (IntSupplier) runnerClass.getConstructor(Path.class).newInstance(testPath);
      Future<Integer> future = executor.submit(runner::getAsInt);
      return future.get(2, TimeUnit.MINUTES); // TODO Make timeout configurable.
    } catch (Exception e) {
      e.printStackTrace();
      throw new AssertionError("Loading and invoking TestRunner failed", e);
    }
  }

  private ClassLoader createTestClassLoader(Path pluginDir, Path testPath, String moduleName) {
    String pluginModuleName = getClass().getModule().getName(); // "com.github.forax.pro.plugin.tester"
    List<String> roots = List.of(pluginModuleName, moduleName);
    
    Path pluginPath = pluginDir.resolve(name()); // "[pro]/plugins/tester"
    ModuleFinder finder = ModuleFinder.of(testPath, pluginPath);
    ModuleLayer bootModuleLayer = ModuleLayer.boot();
    Configuration configuration = bootModuleLayer.configuration().resolve(finder, ModuleFinder.of(), roots);
    ClassLoader parentLoader = ClassLoader.getSystemClassLoader();
    ModuleLayer configuredLayer = bootModuleLayer.defineModulesWithOneLoader(configuration, parentLoader);
    return configuredLayer.findLoader(pluginModuleName);
  }
}
