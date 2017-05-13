module com.github.forax.pro.api {
  requires java.base;
  
  requires java.se;   // temporary fix until the VM is able to complement module at runtime
  
  exports com.github.forax.pro.api;
  exports com.github.forax.pro.api.helper;
  
  exports com.github.forax.pro.api.impl
    to com.github.forax.pro;
  
  uses com.github.forax.pro.api.Plugin;
}