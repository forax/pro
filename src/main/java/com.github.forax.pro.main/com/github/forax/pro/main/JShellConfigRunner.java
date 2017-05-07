package com.github.forax.pro.main;

import com.github.forax.pro.main.runner.ConfigRunner;
import java.nio.file.Path;
import java.util.Optional;

public class JShellConfigRunner implements ConfigRunner {
  @Override
  public Optional<Runnable> accept(Path configFile) {
    return Optional.<Runnable>of(() -> run(configFile))
        .filter(__ -> configFile.toString().endsWith(".pro"));
  }

  private static void run(Path configFile) {
    //System.out.println("run with jshell " + configFile);

    // jshell use another process, so config is lost   FIXME
    //Pro.set("pro.exitOnError", true);

    //Secret.jShellTool_main(configFile.toString());

    JShellWrapper.run(System.in, System.out, System.err, configFile.toString());
  }
}
