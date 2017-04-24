module com.github.forax.pro.plugin.tester {
  requires com.github.forax.pro.api;
  requires com.github.forax.pro.helper;

  requires junit.platform.console.standalone;

  opens com.github.forax.pro.plugin.tester;
  
  provides com.github.forax.pro.api.Plugin
    with com.github.forax.pro.plugin.tester.TesterPlugin;
}
