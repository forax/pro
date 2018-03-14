module com.github.forax.pro.plugin.perfer {
  requires com.github.forax.pro.api;
  requires com.github.forax.pro.helper;
  requires org.openjdk.jmh;
  
  opens com.github.forax.pro.plugin.perfer;
  
  provides com.github.forax.pro.api.Plugin
    with com.github.forax.pro.plugin.perfer.PerferPlugin;
}