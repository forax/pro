package com.github.forax.pro.plugin.tester;

import static com.github.forax.pro.api.MutableConfig.derive;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.helper.Log;

public class TesterPlugin implements Plugin {

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

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
    derive(tester, TesterConf::javaHome, convention, ConventionFacade::javaHome);
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
    Log log = Log.create(name(), config.getOrThrow("pro", ProConf.class).loglevel());
    TesterConf tester = config.getOrThrow(name(), TesterConf.class);
    log.debug(tester, _tester -> "config " + _tester);

    Thread currentThread = Thread.currentThread();
    ClassLoader oldContext = currentThread.getContextClassLoader();
    try {
      int exitCodeSum = 0;
      for (Path path : directories(tester.moduleExplodedTestPath())) {
        exitCodeSum += execute(tester, path.toAbsolutePath().normalize());
      }
      return exitCodeSum;
    } finally {
      currentThread.setContextClassLoader(oldContext); // restore the context
    }
  }

  private int execute(TesterConf tester, Path testPath) {
    ModuleReference moduleReference = ModuleFinder.of(testPath).findAll().iterator().next();
    String moduleName = moduleReference.descriptor().name();
    ClassLoader testClassLoader = createTestClassLoader(tester, testPath, moduleName);
    Thread.currentThread().setContextClassLoader(testClassLoader);
    try {
      Class<?> runnerClass = testClassLoader.loadClass(TesterRunner.class.getName());
      @SuppressWarnings("unchecked")
      Callable<Integer> runner = (Callable<Integer>) runnerClass.getConstructor(Path.class).newInstance(testPath);
      Future<Integer> future = executor.submit(runner);
      return future.get(1, TimeUnit.MINUTES);
    } catch (Exception e) {
      e.printStackTrace();
      throw new AssertionError("Loading and invoking TestRunner failed", e);
    }
  }

  private ClassLoader createTestClassLoader(TesterConf tester, Path testPath, String moduleName) {
    String pluginModuleName = getClass().getModule().getName(); // com.github.forax.pro.plugin.tester
    Path pluginRoot = tester.javaHome().resolve("plugins").resolve(name());
    Path mainDependenciesPath = Paths.get("deps").toAbsolutePath().normalize();
    Path mainArtifactPath = Paths.get("target/main/artifact").toAbsolutePath().normalize();
    List<Path> paths = List.of(testPath, pluginRoot, mainDependenciesPath, mainArtifactPath);
    // System.out.println("Using ModuleFinder root path entries: " + paths);
    ModuleFinder finder = ModuleFinder.of(paths.toArray(new Path[0]));
    ModuleLayer boot = ModuleLayer.boot();
    Configuration cf = boot.configuration().resolve(finder, ModuleFinder.of(), List.of(pluginModuleName, moduleName));
    ClassLoader parentLoader = ClassLoader.getSystemClassLoader();
    ModuleLayer layer = boot.defineModulesWithOneLoader(cf, parentLoader);
    return layer.findLoader(pluginModuleName);
  }

}
