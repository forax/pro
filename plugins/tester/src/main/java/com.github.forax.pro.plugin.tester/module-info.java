module com.github.forax.pro.plugin.tester {
  requires com.github.forax.pro.api;
  requires com.github.forax.pro.helper;

  // java.lang.IllegalAccessException:
  //   class com.github.forax.pro.plugin.tester.TesterPlugin (in module com.github.forax.pro.plugin.tester)
  // cannot access
  //   class com.github.forax.pro.plugin.tester.TesterRunner (in module com.github.forax.pro.plugin.tester)
  // because
  //   module com.github.forax.pro.plugin.tester
  // does not export
  //   com.github.forax.pro.plugin.tester
  // to
  //   module com.github.forax.pro.plugin.tester
  exports com.github.forax.pro.plugin.tester;

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
