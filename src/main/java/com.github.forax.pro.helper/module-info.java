module com.github.forax.pro.helper {
  requires jdk.jshell;   // should be removed when JShell will have a programmatic API
  requires jdk.compiler;
  requires org.objectweb.asm.all.debug;
  
  exports com.github.forax.pro.helper;
  
  exports com.github.forax.pro.helper.secret
    to com.github.forax.pro.main;
}