package com.github.forax.pro.plugin.tester;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;
import org.junit.platform.console.ConsoleLauncher;
import org.junit.platform.engine.TestEngine;

public class TesterPlugin implements Plugin {
  @Override
  public String name() {
    return "tester";
  }

  @Override
  public void init(MutableConfig config) {
  }
  
  @Override
  public void configure(MutableConfig config) {
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
  }

  @Override
  public int execute(Config config) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();

    Thread.currentThread().setContextClassLoader(TestEngine.class.getClassLoader());
    int exitCode = ConsoleLauncher.execute(
        new PrintStream(out, true, "UTF-8"),
        new PrintStream(err, true, "UTF-8"),
        "--scan-classpath",
        "--include-engine", "junit-jupiter",
        "--classpath", "target/test/exploded/com.github.forax.pro.plugin.tester"
    ).getExitCode();

    System.out.println(out);
    System.err.println(err);

    return exitCode;
  }
}
