module com.github.forax.pro.plugin.tester {
  requires com.github.forax.pro.api;
  requires com.github.forax.pro.helper;

  opens com.github.forax.pro.plugin.tester;

  // Open Test Alliance for the JVM
  requires opentest4j;

  // JUnit Platform
  requires junit.platform.commons;
  requires junit.platform.engine;
  requires junit.platform.launcher;

  // JUnit Jupiter
  requires junit.jupiter.api;
  requires junit.jupiter.engine;
  
  provides com.github.forax.pro.api.Plugin
    with com.github.forax.pro.plugin.tester.TesterPlugin;
}
