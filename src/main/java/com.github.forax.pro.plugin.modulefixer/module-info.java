module com.github.forax.pro.plugin.modulefixer {
  requires com.github.forax.pro.api;
  requires com.github.forax.pro.helper;
  requires org.objectweb.asm;
  requires org.objectweb.asm.commons;
  requires org.objectweb.asm.tree;
  requires org.objectweb.asm.tree.analysis;
  
  opens com.github.forax.pro.plugin.modulefixer;
  
  provides com.github.forax.pro.api.Plugin
    with com.github.forax.pro.plugin.modulefixer.ModuleFixerPlugin;
}