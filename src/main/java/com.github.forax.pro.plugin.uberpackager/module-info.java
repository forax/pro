module com.github.forax.pro.plugin.uberpackager {
  requires com.github.forax.pro.api;
  requires com.github.forax.pro.helper;
  
  requires com.github.forax.pro.ubermain;
  
  opens com.github.forax.pro.plugin.uberpackager;
  
  provides com.github.forax.pro.api.Plugin
    with com.github.forax.pro.plugin.uberpackager.UberPackagerPlugin;
}