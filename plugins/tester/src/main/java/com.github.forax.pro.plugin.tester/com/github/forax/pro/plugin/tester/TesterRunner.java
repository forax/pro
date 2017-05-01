package com.github.forax.pro.plugin.tester;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public class TesterRunner implements Callable<Integer> {

    private final Path testPath;

    public TesterRunner(Path testPath) {
        this.testPath = testPath;
    }

    @Override
    public Integer call() throws Exception {
        ModuleReference moduleReference = ModuleFinder.of(testPath).findAll().iterator().next();
        System.out.println("Test run of module " + moduleReference.descriptor().name() + " starts...");
        List<Class<?>> testClasses = findTestClasses(moduleReference);
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        Launcher launcher = LauncherFactory.create();
        return launch(launcher, testClasses);
    }

    private int launch(Launcher launcher, List<Class<?>> testClasses) {
        LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request();
        for (Class<?> testClass : testClasses) {
            builder.selectors(selectClass(testClass));
        }
        LauncherDiscoveryRequest launcherDiscoveryRequest = builder.build();
        SummaryGeneratingListener summaryGeneratingListener = new SummaryGeneratingListener();
        launcher.execute(launcherDiscoveryRequest, summaryGeneratingListener);
        StringWriter stringWriter = new StringWriter();
        summaryGeneratingListener.getSummary().printTo(new PrintWriter(stringWriter));
        summaryGeneratingListener.getSummary().printFailuresTo(new PrintWriter(stringWriter));
        System.out.println(stringWriter);
        return (int) summaryGeneratingListener.getSummary().getTestsFailedCount();
    }

    private List<Class<?>> findTestClasses(ModuleReference moduleReference) {
        List<String> entries;
        try (ModuleReader moduleReader = moduleReference.open()) {
            entries = moduleReader.list()
                    .filter(name -> name.endsWith("Tests.class"))
                    .collect(Collectors.toList());
        }
        catch (IOException exception) {
            throw new AssertionError("module reader failure", exception);
        }
        List<Class<?>> testClasses = new ArrayList<>();
        for (String entry : entries) {
            String name = entry.substring(0, entry.length() - ".class".length());
            name = name.replace('/','.');
            try {
                // Thread.currentThread().getContextClassLoader()
                Class<?> testClass = getClass().getClassLoader().loadClass(name);
                testClasses.add(testClass);
            } catch (Exception exception) {
                exception.printStackTrace();
                throw new AssertionError("Loading failed for name: " + name + " (entry=" + entry + ")", exception);
            }
        }
        return testClasses;
    }

}
