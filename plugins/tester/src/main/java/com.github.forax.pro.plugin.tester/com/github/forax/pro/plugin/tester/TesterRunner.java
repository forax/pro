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
  private final TesterFixture fixture;

  public TesterRunner(TesterFixture fixture) {
    this.classLoader = getClass().getClassLoader();
    this.fixture = fixture;
  }

  @Override
  public int getAsInt() {
    var oldContext = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(classLoader);
    try {
      return launchJUnitPlatform();
    } finally {
      Thread.currentThread().setContextClassLoader(oldContext);
    }
  }

  private int launchJUnitPlatform() {
    var builder = LauncherDiscoveryRequestBuilder.request();
    builder.selectors(selectModule(fixture.moduleDescriptor.name()));
    builder.configurationParameter("junit.jupiter.execution.parallel.enabled", "" + fixture.parallel);

    var startTimeMillis = System.currentTimeMillis();
    var launcherDiscoveryRequest = builder.build();
    var summaryGeneratingListener = new SummaryGeneratingListener();
    LauncherFactory.create().execute(launcherDiscoveryRequest, summaryGeneratingListener);
    var duration = System.currentTimeMillis() - startTimeMillis;
    var summary = summaryGeneratingListener.getSummary();
    int failures = (int) summary.getTestsFailedCount();
    if (failures == 0) {
      var succeeded = summary.getTestsSucceededCount();
      String moduleName = fixture.moduleDescriptor.toNameAndVersion();
      System.out.printf("[tester] Successfully tested %s: %d tests in %d ms%n", moduleName, succeeded, duration);
    } else {
      var stringWriter = new StringWriter();
      summary.printTo(new PrintWriter(stringWriter));
      summary.printFailuresTo(new PrintWriter(stringWriter));
      System.out.println(stringWriter);
    }
    return failures;
  }
}
