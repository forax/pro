module com.github.forax.pro.plugin.linker {
  requires com.github.forax.pro.api;
  requires com.github.forax.pro.helper;
  
  opens com.github.forax.pro.plugin.linker;
  
  provides com.github.forax.pro.api.Plugin
    with com.github.forax.pro.plugin.linker.LinkerPlugin;
  
  uses java.util.spi.ToolProvider;
}