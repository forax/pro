package com.github.forax.pro.plugin.tester;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectModule;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

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
    ClassLoader oldContext = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(classLoader);
    try {
      ModuleReference moduleReference = ModuleHelper.getOnlyModule(testPath);
      Launcher launcher = LauncherFactory.create();
      return launch(moduleReference, launcher);
    } finally {
      Thread.currentThread().setContextClassLoader(oldContext);
    }
  }

  private static int launch(ModuleReference moduleReference, Launcher launcher) {
    LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request();
    builder.selectors(selectModule(moduleReference.descriptor().name()));

    long startTimeMillis = System.currentTimeMillis();
    LauncherDiscoveryRequest launcherDiscoveryRequest = builder.build();
    SummaryGeneratingListener summaryGeneratingListener = new SummaryGeneratingListener();
    launcher.execute(launcherDiscoveryRequest, summaryGeneratingListener);
    long duration = System.currentTimeMillis() - startTimeMillis;
    
    TestExecutionSummary summary = summaryGeneratingListener.getSummary();
    int failures = (int) summary.getTestsFailedCount();
    if (failures == 0) {
      long succeeded = summary.getTestsSucceededCount();
      String moduleName = moduleReference.descriptor().toNameAndVersion();
      System.out.printf("[tester] Successfully tested %s: %d tests in %d ms%n", moduleName, succeeded, duration);
    } else {
      StringWriter stringWriter = new StringWriter();
      summary.printTo(new PrintWriter(stringWriter));
      summary.printFailuresTo(new PrintWriter(stringWriter));
      System.out.println(stringWriter);
    }
    return failures;
  }

}
