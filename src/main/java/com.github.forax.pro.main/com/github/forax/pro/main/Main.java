package com.github.forax.pro.main;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE_NEW;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.forax.pro.daemon.Daemon;
import com.github.forax.pro.main.runner.ConfigRunner;

public class Main {
  private Main() {
    throw new AssertionError();
  }
  
  enum InputFile {
    ARGUMENT(args -> (args.length == 1)? Optional.of(Paths.get(args[0])): Optional.empty()),
    DEFAULT_PRO(args -> Optional.of(Paths.get("build.pro"))),
    DEFAULT_JSON(args -> Optional.of(Paths.get("build.json")))
    ;
    
    private final Function<String[], Optional<Path>> mapper;

    private InputFile(Function<String[], Optional<Path>> mapper) {
      this.mapper = mapper;
    }
    
    static Optional<Path> find(String[] args) {
      return Arrays.stream(InputFile.values())
          .map(input -> input.mapper.apply(args))
          .map(p -> p.filter(Files::exists))
          .flatMap(Optional::stream)
          .findFirst();
    }
  }
  
  enum Command {
    SHELL(__ -> shell()),
    BUILD(Main::build),
    DAEMON(Main::daemon),
    SCAFFOLD(__ -> scaffold()), 
    HELP(__ -> help()),
    VERSION(__ -> version())
    ;
    
    final Consumer<String[]> consumer;

    private Command(Consumer<String[]> consumer) {
      this.consumer = consumer;
    }
    
    static Command command(String name) {
      return Arrays.stream(values())
          .filter(command -> command.name().toLowerCase().equals(name))
          .findFirst()
          .orElseThrow(() -> { throw new IllegalArgumentException("unknown sub command " + name); });
    }
  }
  
  static void scaffold() {
    String module = Optional.ofNullable(System.console())
        .map(console -> console.readLine("module name: (like com.acme.foo.bar) "))
        .orElse("com.acme.foo.bar");
    
    String content =
        "import static com.github.forax.pro.Pro.*;\n" + 
        "import static com.github.forax.pro.builder.Builders.*;\n" + 
        "\n" + 
        "resolver.\n" + 
        "    dependencies(list(\n" + 
        "        // JUnit 5\n" + 
        "        \"org.junit.jupiter.api=org.junit.jupiter:junit-jupiter-api:5.1.0\",\n" + 
        "        \"org.junit.platform.commons=org.junit.platform:junit-platform-commons:1.1.0\",\n" + 
        "        \"org.apiguardian.api=org.apiguardian:apiguardian-api:1.0.0\",\n" + 
        "        \"org.opentest4j=org.opentest4j:opentest4j:1.0.0\" /*,*/\n" +
        "\n" +
        "//        // JMH\n" + 
        "//        \"org.openjdk.jmh=org.openjdk.jmh:jmh-core:1.20\",\n" + 
        "//        \"org.apache.commons.math3=org.apache.commons:commons-math3:3.6.1\",\n" + 
        "//        \"net.sf.jopt-simple=net.sf.jopt-simple:jopt-simple:5.0.4\",\n" + 
        "//        \"org.openjdk.jmh.generator=org.openjdk.jmh:jmh-generator-annprocess:1.20\"\n" +
        "    ))\n" + 
        "\n" + 
        "// compiler.\n" + 
        "//     rawArguments(list(\n" + 
        "//         \"--processor-module-path\", \"deps\"   // enable JMH annotation processor\n" + 
        "//     ))\n" + 
        "\n" + 
        "docer.\n" + 
        "    rawArguments(list(\n" + 
        "        \"-quiet\",\n" + 
        "        \"-link\", \"https://docs.oracle.com/javase/9/docs/api/\"))\n" + 
        "   \n" + 
        "packager.\n" + 
        "    moduleMetadata(list(\n" + 
        "        \"" + module + "@1.0/" + module + ".Main\"\n" + 
        "    ))   \n" + 
        "    \n" + 
        "run(resolver, modulefixer, compiler, tester, docer, packager, runner /*, perfer */)\n" + 
        "\n" + 
        "/exit\n";
    
    String mainModule =
        "module " + module + "{ \n" + 
        "\n" + 
        "}\n";
    String mainClass =
        "package " + module + ";\n" + 
        "\n" + 
        "public class Main {\n" + 
        "  public static void main(String[] args) {\n" + 
        "    System.out.println(\"Hello !\");\n" + 
        "  }\n" + 
        "}\n";
    
    String testModule =
        "open module " + module + " {\n" + 
        "  requires org.junit.jupiter.api;\n" + 
        "  \n" + 
        "  // requires org.openjdk.jmh;  // JMH support\n" + 
        "  // requires org.openjdk.jmh.generator;\n" + 
        "}";
    String testClass =
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
      Files.write(Paths.get("build.pro"), content.getBytes(UTF_8), CREATE_NEW);
      System.out.println("build.pro generated");
      
      Path sourcePath = Paths.get("src", "main", "java", module);
      Files.createDirectories(sourcePath);
      Files.write(sourcePath.resolve("module-info.java"), mainModule.getBytes(UTF_8), CREATE_NEW);
      Path sourcePackage = sourcePath.resolve(module.replace('.', '/'));
      Files.createDirectories(sourcePackage);
      Files.write(sourcePackage.resolve("Main.java"), mainClass.getBytes(UTF_8), CREATE_NEW);
      System.out.println("Main.java generated");
      
      Path testPath = Paths.get("src", "test", "java", module);
      Files.createDirectories(testPath);
      Files.write(testPath.resolve("module-info.java"), testModule.getBytes(UTF_8), CREATE_NEW);
      Path testPackage = testPath.resolve(module.replace('.', '/'));
      Files.createDirectories(testPackage);
      Files.write(testPackage.resolve("HelloTests.java"), testClass.getBytes(UTF_8), CREATE_NEW);
      System.out.println("HelloTests generated");
      
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
  
  static void version() {
    System.out.println(
        "pro " + Main.class.getModule().getDescriptor().version().map(Version::toString).orElse("unknown") +
        " / jdk " + Runtime.version());
  }
  
  private static Optional<Daemon> getDaemon() {
    return ServiceLoader.load(Daemon.class, ClassLoader.getSystemClassLoader()).findFirst();
  }
  
  static void shell() {
    if (getDaemon().filter(Daemon::isStarted).isPresent()) {
      throw new IllegalStateException("shell doesn't currently support daemon mode :(");
    }
    JShellWrapper.run(System.in, System.out, System.err);
  }
  
  static void build(String[] args) {
    Path configFile = InputFile.find(args)
        .orElseThrow(() -> new IllegalArgumentException("no existing input file specified"));
    
    ServiceLoader<ConfigRunner> loader = ServiceLoader.load(ConfigRunner.class, ConfigRunner.class.getClassLoader());
    ArrayList<ConfigRunner> configRunners = new ArrayList<>();
    loader.forEach(configRunners::add);
    
    configRunners.stream()
        .flatMap(configRunner -> configRunner.accept(configFile).stream())
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("no config runner available for config file " + configFile))
        .run();
  }
  
  static void daemon(String[] args) {
    Daemon service =
        getDaemon().orElseThrow(() -> new IllegalStateException("daemon not found"));
    if (service.isStarted()) {
      throw new IllegalStateException("daemon already started");
    }
    service.start();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      if (service.isStarted()) {
        service.stop();
      }
    }));
    main(args);
  }
  
  static void help() {
    System.err.println(
        "usage: pro [subcommand] args                                                   \n" +
        "                                                                               \n" +
        "  subcommands                                                                  \n" +
        "    build [buildfile]  execute the build file                                  \n" +
        "                       use build.json or build.pro if no buildfile is specified\n" +
        "    daemon subcommand  start the subcommand in daemon mode                     \n" +
        "    shell              start the interactive shell                             \n" +
        "    scaffold           create a default build.pro                              \n" +
        "    version            print the current version                               \n" +
        "    help               this help                                               \n" +
        "                                                                               \n" +
        "  if no subcommand is specified, 'build' is used                               \n"
    );
  }
  
  public static void main(String[] args) {
    Command command;
    String[] arguments;
    if (args.length == 0) {
      command = Command.BUILD;
      arguments = args;
    } else {
      command = Command.command(args[0]);
      arguments = Arrays.stream(args).skip(1).toArray(String[]::new);
    }
    command.consumer.accept(arguments);
  }
}
