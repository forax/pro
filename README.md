# pro
a Java 9 compatible build tool

> No need to be a maven to configure a build tool 


# rational
With the introduction of modules in Java 9, creating modules/jars is easier and
new applications will tend to have many more smaller modules that it was previously the case.
The build model of Maven is not well suited to describe this new world.


# principles

  - use convention over configuration
  - programmatic API first
  - stateless plugins 
  - separate configuration time where configuration is mutable and build time where configuration is immutable
  - external dependencies are in plain sight


# architectures
  


# build instructions
To compile and build pro, run 
```
build.sh
```
pro will bootstrap itself.
The bootstrap process only works with the latest [jdk9-jigsaw](https://jdk9.java.net/jigsaw/) build,
you may have to change the value of the variable JAVA_HOME at the starts of the script build.sh.

Once built, you have an image of the tool in target/pro,
this image embeds its own small JDK so no need to install anything Java related to be able to build your application.
Obviously, you will need a JDK to run your application. 
