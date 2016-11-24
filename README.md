# pro
a Java 9 compatible build tool

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
