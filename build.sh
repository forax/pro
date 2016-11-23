#!/bin/bash
export JAVA_HOME=/usr/jdk/jdk-9-jigsaw
export java=$JAVA_HOME/bin/java
export javac=$JAVA_HOME/bin/javac

rm -fr bootstrap
mkdir bootstrap

$javac --module-source-path src/main/java \
       -d bootstrap/modules/ \
       --module-path deps \
       $(find src/main/java/ -name "*.java")

$java --module-path bootstrap/modules \
      --module com.github.forax.pro.bootstrap/com.github.forax.pro.bootstrap.Bootstrap
      
      
