package com.github.forax.pro.plugin.tester;

import static com.github.forax.pro.api.MutableConfig.derive;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.helper.Log;
import com.github.forax.pro.helper.ModuleHelper;
import com.github.forax.pro.helper.util.StableList;

public class TesterPlugin implements Plugin {
  @Override
  public String name() {
    return "tester";
  }

  @Override
  public void init(MutableConfig config) {
    var testerConf = config.getOrUpdate(name(), TesterConf.class);
    testerConf.timeout(60);
    testerConf.parallel(true);
  }

  @Override
  public void configure(MutableConfig config) {
    var testerConf = config.getOrUpdate(name(), TesterConf.class);
    var proConf = config.getOrThrow("pro", ProConf.class);
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class);

    // inputs
    derive(testerConf, TesterConf::pluginDir, proConf, ProConf::pluginDir);
    derive(testerConf, TesterConf::moduleExplodedTestPath, convention, ConventionFacade::javaModuleExplodedTestPath);
    derive(testerConf, TesterConf::moduleDependencyPath, convention, ConventionFacade::javaModuleDependencyPath);
  }

  @Override
  public void watch(Config config, WatcherRegistry registry) {
    var testerConf = config.getOrThrow(name(), TesterConf.class);
    testerConf.moduleExplodedTestPath().forEach(registry::watch);
  }

  private static List<Path> directories(List<Path> paths) {
    return ModuleFinder.of(paths.toArray(new Path[0]))
        .findAll()
        .stream()
        .flatMap(ref -> ref.location().stream())
        .map(Paths::get)
        .sorted()
        .collect(Collectors.toList());
  }

  @Override
  public int execute(Config config) throws IOException {
    var log = Log.create(name(), config.getOrThrow("pro", ProConf.class).loglevel());
    log.debug(config, conf -> "config " + config);
    var testerConf = config.getOrThrow(name(), TesterConf.class);

    var exitCodeSum = 0;
    for (var path : directories(testerConf.moduleExplodedTestPath())) {
      log.debug(path, p -> String.format("Testing %s...", p.toFile().getName()));
      exitCodeSum += execute(testerConf, path.toAbsolutePath().normalize());
    }
    return exitCodeSum;
  }

  private int execute(TesterConf tester, Path testPath) throws IOException {
    var executor = Executors.newSingleThreadExecutor();

    var moduleReference = ModuleHelper.getOnlyModule(testPath);
    var moduleName = moduleReference.descriptor().name();
    var testClassLoader = createTestClassLoader(tester, testPath, moduleName);

    IntSupplier runner;
    try {
      var runnerClass = testClassLoader.loadClass(TesterRunner.class.getName());
      var args = new HashMap<String, Object>();
      args.put("testPath", testPath);
      args.put("parallel", tester.parallel());
      runner = (IntSupplier) runnerClass.getConstructor(Map.class).newInstance(args);
      var future = executor.submit(runner::getAsInt);
      return future.get(tester.timeout(), TimeUnit.SECONDS);

    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InterruptedException e) {
      throw new IOException("Loading, creating or invoking the TesterRunner failed", e);
    } catch(InvocationTargetException | ExecutionException e) {
      throw rethrow(e.getCause());
    } catch(TimeoutException e) {
      e.printStackTrace(); // FIXME
      return 1;
    }
  }

  private ClassLoader createTestClassLoader(TesterConf tester, Path testPath, String testModuleName) {
    var pluginModuleName = TesterPlugin.class.getModule().getName(); // "com.github.forax.pro.plugin.tester"
    var rootNames = List.of(pluginModuleName, testModuleName);

    var moduleFinderRoots = StableList
        .of(testPath)                                // "target/test/exploded/[MODULE_NAME]
        .append(tester.pluginDir().resolve(name()))  // "[PRO_HOME]/plugins/tester"
        .appendAll(tester.moduleExplodedTestPath())  // "target/test/exploded")
        .appendAll(tester.moduleDependencyPath());   // "deps"

    var finder = ModuleFinder.of(moduleFinderRoots.toArray(Path[]::new));
    var bootModuleLayer = ModuleLayer.boot();
    var configuration = bootModuleLayer.configuration().resolve(finder, ModuleFinder.of(), rootNames);
    var parentLoader = ClassLoader.getSystemClassLoader();
    var configuredLayer = bootModuleLayer.defineModulesWithOneLoader(configuration, parentLoader);
    var classLoader = configuredLayer.findLoader(pluginModuleName);
    classLoader.setDefaultAssertionStatus(true); // -ea
    return classLoader;
  }

  private static UndeclaredThrowableException rethrow(Throwable cause) {
    if (cause instanceof RuntimeException) {
      throw (RuntimeException)cause;
    }
    if (cause instanceof Error) {
      throw (Error)cause;
    }
    return new UndeclaredThrowableException(cause);
  }
}
