module com.github.forax.pro.main {
  requires jdk.jshell;
  requires jdk.unsupported;
  requires org.json;
  requires com.github.forax.pro;
  requires com.github.forax.pro.helper;
  
  exports com.github.forax.pro.main.runner;
  
  uses com.github.forax.pro.main.runner.Runner;
  provides com.github.forax.pro.main.runner.Runner
    with com.github.forax.pro.main.JSONRunner,
         com.github.forax.pro.main.JShellRunner;
}