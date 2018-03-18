module com.github.forax.pro {
  requires transitive com.github.forax.pro.api;
  requires com.github.forax.pro.daemon;
  requires transitive com.github.forax.pro.helper;
  
  exports com.github.forax.pro;
  
  uses com.github.forax.pro.api.Plugin;
  uses com.github.forax.pro.daemon.Daemon;
}