package com.github.forax.pro.plugin.tester;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectModule;

import java.io.PrintWriter;
import java.util.function.IntSupplier;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.reporting.legacy.xml.LegacyXmlReportGeneratingListener;

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
    var includeTags = testConf.includeTags();
    var excludeTags = testConf.excludeTags();

    // Create Launcher API entry point
    // https://junit.org/junit5/docs/current/user-guide/#launcher-api
    var builder = LauncherDiscoveryRequestBuilder.request();

    // Select from...
    var packages = testConf.packages();
    if (packages.isEmpty()) {
      builder.selectors(selectModule(moduleName));
    } else {
      builder.selectors(packages.stream().map(DiscoverySelectors::selectPackage).collect(toUnmodifiableList()));  
    }

    // Apply filters...
    if (!includeTags.isEmpty()) {
      builder.filters(TagFilter.includeTags(includeTags));
    }
    if (!excludeTags.isEmpty()) {
      builder.filters(TagFilter.excludeTags(excludeTags));
    }

    // Fine-tune configuration...
    builder.configurationParameter("junit.jupiter.execution.parallel.enabled", Boolean.toString(parallel));

    var launcher = LauncherFactory.create();
    var launcherDiscoveryRequest = builder.build();
    var summaryGeneratingListener = new SummaryGeneratingListener();
    var xmlReportsWritingListener = new LegacyXmlReportGeneratingListener(testConf.reportPath().resolve(moduleName), new PrintWriter(System.out));
    launcher.registerTestExecutionListeners(summaryGeneratingListener);
    launcher.registerTestExecutionListeners(xmlReportsWritingListener);
    var startTimeMillis = System.currentTimeMillis();
    launcher.execute(launcherDiscoveryRequest);
    var duration = System.currentTimeMillis() - startTimeMillis;
    var summary = summaryGeneratingListener.getSummary();
    
    // DEBUG
    //summary.printTo(new PrintWriter(System.out));
    
    var success = summary.getTestsFailedCount() == 0 && summary.getTestsAbortedCount() == 0 &&
                  summary.getContainersFailedCount() == 0 && summary.getContainersAbortedCount() == 0;
    if (success) {
      var succeeded = summary.getTestsSucceededCount();
      System.out.printf("[tester] Successfully tested %s: %d tests in %d ms%n", moduleNameAndVersion, succeeded, duration);
    } else {
      var writer = new PrintWriter(System.err);
      summary.printTo(writer);
      summary.printFailuresTo(writer);
    }
    return success? 0: 1;
  }
}
