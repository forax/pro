module com.github.forax.pro.aether {
  requires org.eclipse.aether.transport.wagon;
  requires org.eclipse.aether.transport.http;
  requires org.eclipse.aether.transport.file;
  requires org.eclipse.aether.connector.basic;
  requires org.eclipse.aether.impl;
  requires org.eclipse.aether.util;
  requires org.eclipse.aether.spi;
  requires org.eclipse.aether.api;
  
  requires org.apache.maven.aether.provider;
  
  exports com.github.forax.pro.aether;
}
