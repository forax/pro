package com.github.forax.pro.main;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE_NEW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.github.forax.pro.main.Main.InputException;

class Scaffold {
  private static String inferCurrentDirecoryName() {
    try {
      return Path.of(".").toRealPath().getFileName().toString();
    } catch (@SuppressWarnings("unused") IOException e) {
      return "pro-generated-name";
    }
  }
  
  static void scaffold() {
    var module = Optional.ofNullable(System.console())
        .map(console -> console.readLine("module name: (like com.acme.foo.bar) "))
        .filter(name -> !name.isEmpty())
        .orElse("com.acme.foo.bar");
    
    var junitPlatformVersion = "1.6.0";
    var junitJupiterVersion = "5.6.0";
    var opentest4jVersion = "1.2.0";
    var apiGuardianVersion = "1.1.0";
    
    var jmhVersion = "1.22";
    var commonMath3Version = "3.6.1";
    var joptSimpleVersion = "5.0.4";
    var content =
        "import static com.github.forax.pro.Pro.*;\n" + 
        "import static com.github.forax.pro.builder.Builders.*;\n" + 
        "\n" + 
        "resolver.\n" + 
        "    checkForUpdate(true).\n" +
        "    dependencies(\n" + 
        "        // JUnit 5\n" + 
        "        \"org.junit.jupiter.api=org.junit.jupiter:junit-jupiter-api:" + junitJupiterVersion + "\",\n" + 
        "        \"org.junit.platform.commons=org.junit.platform:junit-platform-commons:" + junitPlatformVersion + "\",\n" + 
        "        \"org.apiguardian.api=org.apiguardian:apiguardian-api:" + apiGuardianVersion + "\",\n" + 
        "        \"org.opentest4j=org.opentest4j:opentest4j:" + opentest4jVersion + "\" /*,*/\n" +
        "\n" +
        "//        // JMH\n" + 
        "//        \"org.openjdk.jmh=org.openjdk.jmh:jmh-core:" + jmhVersion + "\",\n" + 
        "//        \"org.apache.commons.math3=org.apache.commons:commons-math3:" + commonMath3Version + "\",\n" + 
        "//        \"net.sf.jopt-simple=net.sf.jopt-simple:jopt-simple:" + joptSimpleVersion + "\",\n" + 
        "//        \"org.openjdk.jmh.generator=org.openjdk.jmh:jmh-generator-annprocess:" + jmhVersion + "\"\n" +
        "    )\n" + 
        "\n" + 
        "compiler.\n" + 
        "    sourceRelease(11)/*.*/\n" +
        "//     rawArguments(\n" + 
        "//         \"--processor-module-path\", \"deps\"   // enable JMH annotation processor\n" + 
        "//     )\n" + 
        "\n" + 
        "docer.\n" + 
        "    quiet(true).\n" + 
        "    link(uri(\"https://docs.oracle.com/en/java/javase/11/docs/api/\"))\n" + 
        "   \n" + 
        "packager.\n" + 
        "    modules(\n" + 
        "        \"" + module + "@1.0/" + module + ".Main\"\n" + 
        "    )   \n" + 
        "    \n" + 
        "run(resolver, modulefixer, compiler, tester, docer, packager, runner /*, perfer */)\n" + 
        "\n" + 
        "/exit errorCode()\n";
    
    var eclipseClassPath =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
        "<classpath>\n" + 
        "  <classpathentry excluding=\"module-info.java\" kind=\"src\" output=\"eclipse-output/main\" path=\"src/main/java/" + module + "\"/>\n" + 
        "  <classpathentry excluding=\"module-info.java\" kind=\"src\" output=\"eclipse-output/test\" path=\"src/test/java/" + module + "\">\n" + 
        "    <attributes>\n" + 
        "      <attribute name=\"test\" value=\"true\"/>\n" + 
        "    </attributes>\n" + 
        "  </classpathentry>\n" + 
        "  <classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\">\n" + 
        "    <attributes>\n" + 
        "      <attribute name=\"module\" value=\"true\"/>\n" + 
        "    </attributes>\n" + 
        "  </classpathentry>\n" + 
        "  <classpathentry kind=\"con\" path=\"org.eclipse.jdt.junit.JUNIT_CONTAINER/5\">\n" + 
        "    <attributes>\n" + 
        "      <attribute name=\"module\" value=\"true\"/>\n" + 
        "    </attributes>\n" + 
        "  </classpathentry>\n" + 
        "  <classpathentry kind=\"output\" path=\"bin\"/>\n" + 
        "</classpath>";
    var eclipseProject =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
        "<projectDescription>\n" + 
        "  <name>" + inferCurrentDirecoryName() + "</name>\n" + 
        "  <comment></comment>\n" + 
        "  <projects>\n" + 
        "  </projects>\n" + 
        "  <buildSpec>\n" + 
        "    <buildCommand>\n" + 
        "      <name>org.eclipse.jdt.core.javabuilder</name>\n" + 
        "      <arguments>\n" + 
        "      </arguments>\n" + 
        "    </buildCommand>\n" + 
        "  </buildSpec>\n" + 
        "  <natures>\n" + 
        "    <nature>org.eclipse.jdt.core.javanature</nature>\n" + 
        "  </natures>\n" + 
        "</projectDescription>";
    
    var mainModule =
        "module " + module + "{ \n" + 
        "\n" + 
        "}\n";
    var mainClass =
        "package " + module + ";\n" + 
        "\n" + 
        "public class Main {\n" + 
        "  public static void main(String[] args) {\n" + 
        "    System.out.println(\"Hello Pro !\");\n" + 
        "  }\n" + 
        "}\n";
    
    var testModule =
        "open module " + module + " {\n" + 
        "  requires org.junit.jupiter.api;\n" + 
        "  \n" + 
        "  // requires org.openjdk.jmh;  // JMH support\n" + 
        "  // requires org.openjdk.jmh.generator;\n" + 
        "}";
    var testClass =
        "package " + module + ";\n" + 
        "\n" + 
        "import static org.junit.jupiter.api.Assertions.*;\n" + 
        "\n" + 
        "import org.junit.jupiter.api.Test;\n" +
        "\n" + 
        "@SuppressWarnings(\"static-method\")\n" + 
        "class HelloTests {\n" + 
        "  // pro requires the test class to finish with 'Tests'\n" + 
        "  \n" + 
        "  @Test\n" + 
        "  void test() {\n" + 
        "    System.out.println(\"Hello test !\");\n" +
        "  }\n" + 
        "}\n";
    
    try {
      Files.write(Path.of("build.pro"), content.getBytes(UTF_8), CREATE_NEW);
      System.out.println("build.pro generated");
      
      Files.write(Path.of(".classpath"), eclipseClassPath.getBytes(UTF_8), CREATE_NEW);
      Files.write(Path.of(".project"), eclipseProject.getBytes(UTF_8), CREATE_NEW);
      System.out.println(".classpath and .project (Eclipse) generated");
      
      var sourcePath = Path.of("src", "main", "java", module);
      Files.createDirectories(sourcePath);
      Files.write(sourcePath.resolve("module-info.java"), mainModule.getBytes(UTF_8), CREATE_NEW);
      var sourcePackage = sourcePath.resolve(module.replace('.', '/'));
      Files.createDirectories(sourcePackage);
      Files.write(sourcePackage.resolve("Main.java"), mainClass.getBytes(UTF_8), CREATE_NEW);
      System.out.println("Main.java generated");
      
      var testPath = Path.of("src", "test", "java", module);
      Files.createDirectories(testPath);
      Files.write(testPath.resolve("module-info.java"), testModule.getBytes(UTF_8), CREATE_NEW);
      var testPackage = testPath.resolve(module.replace('.', '/'));
      Files.createDirectories(testPackage);
      Files.write(testPackage.resolve("HelloTests.java"), testClass.getBytes(UTF_8), CREATE_NEW);
      System.out.println("HelloTests generated");
      
    } catch (IOException e) {
      throw new InputException(e);
    }
  }
}
