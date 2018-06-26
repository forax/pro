package com.github.forax.pro.plugin.tester;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectModule;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.IntSupplier;

import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

public class TesterRunner implements IntSupplier {
  private final ClassLoader classLoader;
  private final TestConf testConf;

  public TesterRunner(TestConf testConf) {
    this.classLoader = getClass().getClassLoader();
    this.testConf = testConf;
  }

  @Override
  public int getAsInt() {
    var oldContext = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(classLoader);
    try {
      return launchJUnitPlatform(testConf);
    } finally {
      Thread.currentThread().setContextClassLoader(oldContext);
    }
  }

  private static int launchJUnitPlatform(TestConf testConf) {
    var moduleName = testConf.moduleName();
    var moduleNameAndVersion = testConf.moduleNameAndVersion();
    var parallel = testConf.parallel();

    var builder = LauncherDiscoveryRequestBuilder.request();
    builder.selectors(selectModule(moduleName));
    builder.configurationParameter("junit.jupiter.execution.parallel.enabled", Boolean.toString(parallel));

    var launcher = LauncherFactory.create();
    var launcherDiscoveryRequest = builder.build();
    var summaryGeneratingListener = new SummaryGeneratingListener();
    var startTimeMillis = System.currentTimeMillis();
    launcher.execute(launcherDiscoveryRequest, summaryGeneratingListener);
    var duration = System.currentTimeMillis() - startTimeMillis;
    var summary = summaryGeneratingListener.getSummary();
    int failures = (int) summary.getTestsFailedCount();
    if (failures == 0) {
      var succeeded = summary.getTestsSucceededCount();
      System.out.printf("[tester] Successfully tested %s: %d tests in %d ms%n", moduleNameAndVersion, succeeded, duration);
    } else {
      var stringWriter = new StringWriter();
      summary.printTo(new PrintWriter(stringWriter));
      summary.printFailuresTo(new PrintWriter(stringWriter));
      System.out.println(stringWriter);
    }
    return failures;
  }
}
