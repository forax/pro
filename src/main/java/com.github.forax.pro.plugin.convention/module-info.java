module com.github.forax.pro.plugin.convention {
  requires com.github.forax.pro.api;
  
  opens com.github.forax.pro.plugin.convention;
  
  provides com.github.forax.pro.api.Plugin
    with com.github.forax.pro.plugin.convention.ConventionPlugin;
}