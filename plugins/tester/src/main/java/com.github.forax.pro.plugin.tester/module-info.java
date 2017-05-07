module com.github.forax.pro.plugin.tester {
  requires com.github.forax.pro.api;
  requires com.github.forax.pro.helper;

  opens com.github.forax.pro.plugin.tester;

  // JUnit Platform
  requires org.junit.platform.engine;
  requires org.junit.platform.launcher;

  // JUnit Jupiter
  requires org.junit.jupiter.engine;
  
  provides com.github.forax.pro.api.Plugin
    with com.github.forax.pro.plugin.tester.TesterPlugin;
}
