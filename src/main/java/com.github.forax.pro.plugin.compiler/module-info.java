module com.github.forax.pro.plugin.compiler {
  requires com.github.forax.pro.api;
  requires com.github.forax.pro.helper;
  
  exports com.github.forax.pro.plugin.compiler;
  
  provides com.github.forax.pro.api.Plugin
    with com.github.forax.pro.plugin.compiler.CompilerPlugin;
}