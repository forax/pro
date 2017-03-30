#!/bin/bash

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
darwin=false
os400=false
hpux=false
case "`uname`" in
  CYGWIN*) cygwin=true;;
  Darwin*) darwin=true;;
  OS400*) os400=true;;
  HP-UX*) hpux=true;;
esac


#export JAVA_HOME=/usr/jdk/jdk-9-jigsaw-b146
# For Cygwin, ensure paths are in UNIX format before anything is touched
export JAVA_HOME=/usr/jdk/jdk-9

if $darwin; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-9.jdk/Contents/Home/"
fi

export java=$JAVA_HOME/bin/java
export javac=$JAVA_HOME/bin/javac

rm -fr bootstrap
mkdir bootstrap

$javac --module-source-path src/main/java \
       -d bootstrap/modules/ \
       --module-path deps \
       $(find src/main/java/ -name "*.java")

$java --module-path bootstrap/modules:deps \
      --module com.github.forax.pro.bootstrap/com.github.forax.pro.bootstrap.Bootstrap
      
      
