package com.github.forax.pro.plugin.tester;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import com.github.forax.pro.helper.ModuleHelper;

public class TesterRunner implements IntSupplier {
  private final ClassLoader classLoader;
  private final Path testPath;

  public TesterRunner(Path testPath) {
    this.classLoader = getClass().getClassLoader();
    this.testPath = testPath;
  }

  @Override
  public int getAsInt() {
    var oldContext = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(classLoader);
    try {
      var moduleReference = ModuleHelper.getOnlyModule(testPath);
      var testClasses = findTestClasses(moduleReference);
      var launcher = LauncherFactory.create();
      return launch(moduleReference, launcher, testClasses);
    } finally {
      Thread.currentThread().setContextClassLoader(oldContext);
    }
  }

  private static int launch(ModuleReference moduleReference, Launcher launcher, List<Class<?>> testClasses) {
    var builder = LauncherDiscoveryRequestBuilder.request();
    testClasses.forEach(testClass -> builder.selectors(selectClass(testClass)));

    var startTimeMillis = System.currentTimeMillis();
    var launcherDiscoveryRequest = builder.build();
    var summaryGeneratingListener = new SummaryGeneratingListener();
    launcher.execute(launcherDiscoveryRequest, summaryGeneratingListener);
    var duration = System.currentTimeMillis() - startTimeMillis;
    
    var summary = summaryGeneratingListener.getSummary();
    int failures = (int) summary.getTestsFailedCount();
    if (failures == 0) {
      var succeeded = summary.getTestsSucceededCount();
      String moduleName = moduleReference.descriptor().toNameAndVersion();
      System.out.printf("[tester] Successfully tested %s: %d tests in %d ms%n", moduleName, succeeded, duration);
    } else {
      var stringWriter = new StringWriter();
      summary.printTo(new PrintWriter(stringWriter));
      summary.printFailuresTo(new PrintWriter(stringWriter));
      System.out.println(stringWriter);
    }
    return failures;
  }

  private static List<Class<?>> findTestClasses(ModuleReference moduleReference) {
    try (var moduleReader = moduleReference.open()) {
      return moduleReader.list()
          .filter(name -> name.endsWith("Tests.class")) // TODO Make test class filter configurable
          .map(TesterRunner::loadTestClass)
          .collect(Collectors.toList());
    } catch(IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Class<?> loadTestClass(String fileName) {
    var className = fileName.substring(0, fileName.length() - ".class".length());
    className = className.replace('/','.');
    var classLoader = TesterRunner.class.getClassLoader();
    try {
      return classLoader.loadClass(className);
    } catch (ClassNotFoundException e) {
      throw new UncheckedIOException(
          new IOException("Loading failed for name: " + className + " (" + fileName + ')', e));
    }
  }
}
