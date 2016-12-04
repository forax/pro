module com.github.forax.pro.aether {
  // use automatic modules for now, so everything is flat
  requires aether.api;
  requires aether.connector.basic;
  requires aether.impl;
  requires aether.spi;
  requires aether.transport.classpath;
  requires aether.transport.file;
  requires aether.transport.http;
  requires aether.transport.wagon;
  requires aether.util;
  requires commons.lang;   // should be commons.lang3
  requires commons.logging.api;
  requires httpclient;
  requires httpcore;
  requires maven.aether.provider;
  requires maven.artifactfat; // merge between maven.artifact and maven.repository.metadata because of split packages
  //requires maven.artifact;  
  //requires maven.repository.metadata;
  requires maven.builder.support;
  requires maven.modelfat;    // merge between maven.model and maven.model.builder because of split packages
  //requires maven.model;
  //requires maven.model.builder;
  requires plexus.interpolation;
  requires plexus.utils;
  
  requires com.github.forax.pro.aether.fakeguava;
  
  exports com.github.forax.pro.aether;
}