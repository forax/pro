package com.github.forax.pro.plugin.tester;

import static com.github.forax.pro.api.MutableConfig.derive;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
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
    testerConf.includeTags(List.of());
    testerConf.excludeTags(List.of());
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

  private static List<ModuleReference> modules(List<Path> paths) {
    return ModuleFinder.of(paths.toArray(new Path[0]))
        .findAll()
        .stream()
        .filter(ref -> ref.location().isPresent())
        .sorted(Comparator.comparing(ref -> ref.descriptor().name()))
        .collect(Collectors.toList());
  }

  @Override
  public int execute(Config config) throws IOException {
    var log = Log.create(name(), config.getOrThrow("pro", ProConf.class).loglevel());
    log.debug(config, conf -> "config " + config);
    var testerConf = config.getOrThrow(name(), TesterConf.class);

    var exitCodeSum = 0;
    for (var moduleRef : modules(testerConf.moduleExplodedTestPath())) {
      log.verbose(moduleRef, _moduleRef -> "Testing module " + _moduleRef.descriptor().name() + " ...");
      exitCodeSum += execute(testerConf, moduleRef);
    }
    return exitCodeSum;
  }

  private int execute(TesterConf tester, ModuleReference moduleReference) throws IOException {
    var executor = Executors.newSingleThreadExecutor();
    var moduleDescriptor = moduleReference.descriptor();
    var moduleName = moduleDescriptor.name();
    var testPath = Paths.get(moduleReference.location().get());
    var loader = createTestClassLoader(tester, testPath, moduleName);
    
    var testConfClass = load(loader, TestConf.class);
    var testConfTypes = new Class<?>[] {ModuleDescriptor.class, boolean.class, List.class, List.class};
    var testConf = create(testConfClass, testConfTypes, moduleDescriptor, tester.parallel(), tester.includeTags(), tester.excludeTags());
    var runnerClass = load(loader, TesterRunner.class);
    var runnerTypes = new Class<?>[] {testConfClass};
    var runner = (IntSupplier) create(runnerClass, runnerTypes, testConf);
     
    try {
      var future = executor.submit(runner::getAsInt);
      return future.get(tester.timeout(), TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new IOException(e);
    } catch (ExecutionException e) {
      throw rethrow(e.getCause());
    } catch (TimeoutException e) {
      e.printStackTrace(); // FIXME
      return 1;
    }
  }

  private static Class<?> load(ClassLoader loader, Class<?> type) {
    var name = type.getName();
    try {
      return loader.loadClass(name);
    } catch (ClassNotFoundException e) {
      throw new AssertionError(e);
    }
  }
  
  private static Object create(Class<?> type, Class<?>[] parameterTypes, Object... args) {
    try {
      return type.getConstructor(parameterTypes).newInstance(args);
    } catch (IllegalAccessException | NoSuchMethodException | InstantiationException e) {
      throw new AssertionError(e);
    } catch(InvocationTargetException e) {
      throw rethrow(e.getCause());
    }
  }

  private ClassLoader createTestClassLoader(
      TesterConf tester, Path testPath, String testModuleName) {
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
      throw (RuntimeException) cause;
    }
    if (cause instanceof Error) {
      throw (Error) cause;
    }
    return new UndeclaredThrowableException(cause);
  }
}
