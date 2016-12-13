module com.github.forax.pro {
  requires com.github.forax.pro.api;
  requires transitive com.github.forax.pro.helper;
  
  exports com.github.forax.pro;
  
  uses com.github.forax.pro.api.Plugin;
}