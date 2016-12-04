module com.github.forax.pro.plugin.modulefixer {
  requires com.github.forax.pro.api;
  requires com.github.forax.pro.helper;
  requires org.objectweb.asm.all.debug;
  
  exports com.github.forax.pro.plugin.modulefixer;
  
  provides com.github.forax.pro.api.Plugin
    with com.github.forax.pro.plugin.modulefixer.ModuleFixerPlugin;
}