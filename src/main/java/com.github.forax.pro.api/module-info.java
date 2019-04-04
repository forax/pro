module com.github.forax.pro.api {
  requires java.base;
  
  requires java.se;   // temporary fix until the VM is able to complement module at runtime
  
  requires com.github.forax.pro.helper;
  
  exports com.github.forax.pro.api;
  exports com.github.forax.pro.api.helper;
  
  exports com.github.forax.pro.api.impl
    to com.github.forax.pro,
       com.github.forax.pro.bootstrap.genbuilder;
  
  uses com.github.forax.pro.api.Plugin;
}