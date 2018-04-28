module com.github.forax.pro.main {
  requires java.compiler;
  requires org.json;
  requires com.github.forax.pro;
  requires com.github.forax.pro.aether;
  requires com.github.forax.pro.builder;
  requires com.github.forax.pro.helper;
  requires com.github.forax.pro.daemon;
  
  exports com.github.forax.pro.main.runner;
  
  uses com.github.forax.pro.daemon.Daemon;
  uses com.github.forax.pro.main.runner.ConfigRunner;
  uses javax.tools.Tool;
  
  provides com.github.forax.pro.main.runner.ConfigRunner
    with com.github.forax.pro.main.JSONConfigRunner,
         com.github.forax.pro.main.JShellConfigRunner;
}