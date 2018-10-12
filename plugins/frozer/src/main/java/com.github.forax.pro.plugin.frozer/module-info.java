module com.github.forax.pro.plugin.frozer {
  requires com.github.forax.pro.api;
  requires com.github.forax.pro.helper;
  
  requires org.objectweb.asm;
  requires org.objectweb.asm.commons;
  
  opens com.github.forax.pro.plugin.frozer;
  
  provides com.github.forax.pro.api.Plugin
    with com.github.forax.pro.plugin.frozer.FrozerPlugin;
}