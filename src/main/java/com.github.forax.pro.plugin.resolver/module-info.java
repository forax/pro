module com.github.forax.pro.plugin.resolver {
  requires com.github.forax.pro.aether;
  requires com.github.forax.pro.api;
  requires com.github.forax.pro.helper;
  
  exports com.github.forax.pro.plugin.resolver;
  
  provides com.github.forax.pro.api.Plugin
    with com.github.forax.pro.plugin.resolver.ResolverPlugin;
}