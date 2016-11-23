module com.github.forax.pro.api {
  requires java.base;
  
  exports com.github.forax.pro.api;
  exports com.github.forax.pro.api.helper;
  
  exports com.github.forax.pro.api.impl
    to com.github.forax.pro;
  
  uses com.github.forax.pro.api.Plugin;
}