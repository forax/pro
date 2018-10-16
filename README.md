[![Linux build status](https://api.travis-ci.org/forax/pro.svg?branch=master)](https://travis-ci.org/forax/pro) [![Windows build status](https://ci.appveyor.com/api/projects/status/fdsju4o5390vn282?svg=true)](https://ci.appveyor.com/project/forax/pro)

# pro
A Java 9 compatible build tool

> No need to be a maven to be able to use a build tool 


# rationale
With the introduction of modules in Java 9, creating modules/jars is easier and
new applications will tend to have many more, smaller modules than before.
The build model of Maven is not well suited to describe this new world.


# principles

  - **pro**grammatic API first
  - use [convention](https://github.com/forax/pro/blob/master/src/main/java/com.github.forax.pro.plugin.convention/com/github/forax/pro/plugin/convention/ConventionPlugin.java#L17) over configuration
  - stateless [plugins](https://github.com/forax/pro/blob/master/src/main/java/com.github.forax.pro.api/com/github/forax/pro/api/Plugin.java) 
  - separate configuration time where configuration is [mutable](https://github.com/forax/pro/blob/master/src/main/java/com.github.forax.pro.api/com/github/forax/pro/api/MutableConfig.java) and build time where configuration is [immutable](https://github.com/forax/pro/blob/master/src/main/java/com.github.forax.pro.api/com/github/forax/pro/api/Config.java)
  - external dependencies are in plain sight (in the `deps` folder)


# anatomy of a build.pro

pro uses a file named `build.pro` as build script, which is composed of two parts, the configuration part and the run part.
In the configuration part, you can set the properties of a specific plugin, by example, this how to set the release version of the source to Java 11 for the compiler
```
  compiler.sourceRelease(11)
```
you can chain the calls, by example to set the source release and use the preview features
```
  compiler.
    sourceRelease(11).
```
Note: pro uses jshell to parse the build.pro, this tool is line oriented so you have to put the dot '.' at the end of
      the line to ask for the parsing of the next line.
      
Then you have to call `run()` with all the command you want to execute, by example,
```
  run(compiler, packager)
```
to run the `compiler` on the sources and uses the `packager` to create a jar.

Here is a list of the main plugins
 - `resolver`  use Maven artifact coordinate to download the dependencies 
 - `modulefixer` patch the artifacts downloaded to make them fully compatible with the module-path 
 - `compiler` compile the sources and the tests
 - `tester` run the JUnit 5 tests
 - `docer` generate the javadoc
 - `runner` run the `main()` of the main class of the main module.



# getting started
  
To create a minimal project that uses pro, you can use the option `scaffold`
```
  mkdir myproject
  cd myproject
  pro scaffold 
```

`scaffold` will ask for a module name (a name in reverse DNS form like a kind of root package) and will generate a skeleton of the folders.

Then you can then run pro to build your project
```
  pro
```



# demo
There is a small demo in the github project [pro-demo](https://github.com/forax/pro-demo/).




# build instructions
To compile and build pro, run:
```
build.sh
```
pro will bootstrap itself.

To build pro you need the [jdk11](http://jdk.java.net/11/) or a more recent version,
you may have to change the value of the variable JAVA_HOME at the start of the script build.sh.

Once built, you have an image of the tool in target/pro.
This image embeds its own small JDK: no need to install anything Java-related to be able to build your application.
Obviously, you will need a JDK to run your application. 
