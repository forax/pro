package com.github.forax.pro.plugin.tester;

import static com.github.forax.pro.api.MutableConfig.derive;

import java.io.IOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import com.github.forax.pro.helper.util.StableList;

public class TesterPlugin implements Plugin {
  @Override
  public String name() {
    return "tester";
  }

  @Override
  public void init(MutableConfig config) {
    TesterConf tester = config.getOrUpdate(name(), TesterConf.class);
    tester.timeout(60);
  }

  @Override
  public void configure(MutableConfig config) {
    TesterConf tester = config.getOrUpdate(name(), TesterConf.class);
    ProConf pro = config.getOrThrow("pro", ProConf.class);
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class);

    // inputs
    derive(tester, TesterConf::pluginDir, pro, ProConf::pluginDir);
    derive(tester, TesterConf::moduleExplodedTestPath, convention, ConventionFacade::javaModuleExplodedTestPath);
    derive(tester, TesterConf::moduleDependencyPath, convention, ConventionFacade::javaModuleDependencyPath);
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
        .sorted()
        .collect(Collectors.toList());
  }

  @Override
  public int execute(Config config) throws IOException {
    Log log = Log.create(name(), config.getOrThrow("pro", ProConf.class).loglevel());
    log.debug(config, conf -> "config " + config);
    TesterConf tester = config.getOrThrow(name(), TesterConf.class);

    int exitCodeSum = 0;
    for (Path path : directories(tester.moduleExplodedTestPath())) {
      log.debug(path, p -> String.format("Testing %s...", p.toFile().getName()));
      exitCodeSum += execute(tester, path.toAbsolutePath().normalize());
    }
    return exitCodeSum;
  }

  private int execute(TesterConf tester, Path testPath) throws IOException {
    ExecutorService executor = Executors.newSingleThreadExecutor();

    ModuleReference moduleReference = ModuleHelper.getOnlyModule(testPath);
    String moduleName = moduleReference.descriptor().name();
    ClassLoader testClassLoader = createTestClassLoader(tester, testPath, moduleName);

    IntSupplier runner;
    try {
      Class<?> runnerClass = testClassLoader.loadClass(TesterRunner.class.getName());
      runner = (IntSupplier) runnerClass.getConstructor(Path.class).newInstance(testPath);
      Future<Integer> future = executor.submit(runner::getAsInt);
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
    String pluginModuleName = TesterPlugin.class.getModule().getName(); // "com.github.forax.pro.plugin.tester"
    List<String> rootNames = List.of(pluginModuleName, testModuleName);

    StableList<Path> moduleFinderRoots = StableList
        .of(testPath)                                // "target/test/exploded/[MODULE_NAME]
        .append(tester.pluginDir().resolve(name()))  // "[PRO_HOME]/plugins/tester"
        .appendAll(tester.moduleExplodedTestPath())  // "target/test/exploded")
        .appendAll(tester.moduleDependencyPath());   // "deps"

    ModuleFinder finder = ModuleFinder.of(moduleFinderRoots.toArray(Path[]::new));
    ModuleLayer bootModuleLayer = ModuleLayer.boot();
    Configuration configuration = bootModuleLayer.configuration().resolve(finder, ModuleFinder.of(), rootNames);
    ClassLoader parentLoader = ClassLoader.getSystemClassLoader();
    ModuleLayer configuredLayer = bootModuleLayer.defineModulesWithOneLoader(configuration, parentLoader);
    ClassLoader classLoader = configuredLayer.findLoader(pluginModuleName);
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
