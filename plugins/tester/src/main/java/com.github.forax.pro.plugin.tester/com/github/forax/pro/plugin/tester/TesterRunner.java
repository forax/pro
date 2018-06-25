package com.github.forax.pro.plugin.tester;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectModule;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.IntSupplier;

import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import com.github.forax.pro.helper.ModuleHelper;

public class TesterRunner implements IntSupplier {
  private final ClassLoader classLoader;
  private final Map<String, Object> args;

  // Note: TestConf.class does not work as a parameter here.
  public TesterRunner(Map<String, Object> args) {
    this.classLoader = getClass().getClassLoader();
    this.args = args;
  }

  @Override
  public int getAsInt() {
    var oldContext = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(classLoader);
    try {
      var moduleReference = ModuleHelper.getOnlyModule((Path) args.get("testPath"));
      return launch(moduleReference);
    } finally {
      Thread.currentThread().setContextClassLoader(oldContext);
    }
  }

  private int launch(ModuleReference moduleReference) {
    var builder = LauncherDiscoveryRequestBuilder.request();
    builder.selectors(selectModule(moduleReference.descriptor().name()));
    builder.configurationParameter("junit.jupiter.execution.parallel.enabled", "" + args.get("parallel"));

    var startTimeMillis = System.currentTimeMillis();
    var launcherDiscoveryRequest = builder.build();
    var summaryGeneratingListener = new SummaryGeneratingListener();
    LauncherFactory.create().execute(launcherDiscoveryRequest, summaryGeneratingListener);
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
}
