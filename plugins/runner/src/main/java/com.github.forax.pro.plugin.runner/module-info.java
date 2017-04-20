module com.github.forax.pro.plugin.runner {
  requires com.github.forax.pro.api;
  requires com.github.forax.pro.helper;
  //requires org.objectweb.asm.all.debug;
  
  opens com.github.forax.pro.plugin.runner;
  
  provides com.github.forax.pro.api.Plugin
    with com.github.forax.pro.plugin.runner.RunnerPlugin;
}