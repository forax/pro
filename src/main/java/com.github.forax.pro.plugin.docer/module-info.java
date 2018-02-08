module com.github.forax.pro.plugin.docer {
  requires com.github.forax.pro.api;
  requires com.github.forax.pro.helper;
  
  opens com.github.forax.pro.plugin.docer;
  
  provides com.github.forax.pro.api.Plugin
    with com.github.forax.pro.plugin.docer.DocerPlugin;
}