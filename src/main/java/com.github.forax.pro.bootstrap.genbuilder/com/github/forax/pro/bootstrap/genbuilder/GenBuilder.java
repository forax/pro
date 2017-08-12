package com.github.forax.pro.bootstrap.genbuilder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.api.impl.Configs.Query;
import com.github.forax.pro.api.impl.DefaultConfig;
import com.github.forax.pro.api.impl.Plugins;

public class GenBuilder {
  private static String builderName(String name) {
    return Character.toUpperCase(name.charAt(0)) + name.substring(1) + "Builder";
  }
  
  private static Class<?> getConfType(Plugin plugin) {
    DefaultConfig config = new DefaultConfig();
    plugin.init(config);
    
    return ((Query)config.getOrThrow(plugin.name(), Object.class))._type_();
  }
  
  
  private static Stream<Object> builderClass(Stream<Plugin> plugins) {
    return Stream.of(
        "package com.github.forax.pro.builder;\n",
        "\n",
        "import com.github.forax.pro.Pro;\n",
        "\n",
        "// THIS CLASS IS GENERATED, DO NOT EDIT\n",
        "// see GenBuilder.java and Bootstrap.java, if you want to re-generate it\n",
        "public class Builders {\n",
          classTemplate("pro", ProConf.class),
          plugins.flatMap(plugin -> classTemplate(plugin.name(), getConfType(plugin))),
        "}\n"
        );
  }
  
  private static Stream<Object> classTemplate(String name, Class<?> confType) {
    String className = builderName(name);
    return Stream.of(
        "  public static final ", className, " ", name, " =\n",
        "    Pro.getOrUpdate(\"", name, "\", ", className ,".class);\n", 
        "  \n",
        "  @com.github.forax.pro.api.TypeCheckedConfig\n",
        "  public interface ", className, " {\n",
            stream(confType.getDeclaredMethods()).filter(m -> m.getParameterCount() != 0).flatMap(m -> methodTemplate(className, m)),
        "  }\n",
        "  \n"
        );
  }
  
  private static Stream<Object> methodTemplate(String className, Method method) {
    String name = method.getName();
    return Stream.of(
        "    public ", className, " ", name, "(", method.getGenericParameterTypes()[0].getTypeName(), " ", name, ");\n"
        );
  }
  
  private static Stream<String> toStream(Object o) {
    if (!(o instanceof Stream)) {
      return Stream.of(o.toString());
    }
    return ((Stream<?>)o).flatMap(GenBuilder::toStream);
  }
  
  private static void template(Path directory, Stream<Plugin> plugins) throws IOException {
    Stream<Object> template = builderClass(plugins);
    String text = template.flatMap(GenBuilder::toStream).collect(joining());
    Files.write(directory.resolve("Builders.java"), text.getBytes(UTF_8));
  }
  
  public static void generate() throws IOException {
    Path pluginDir = Paths.get("target/image/plugins");
    Set<String> invalidPlugins = Set.of("convention", "uberpackager");
    Path directory = Paths.get("src/main/java/com.github.forax.pro.builder/com/github/forax/pro/builder");
    
    List<Plugin> plugins = Plugins.getAllPlugins(pluginDir);
    
    System.out.println("generate bulders to " + directory);
    template(directory,
        plugins.stream().filter(plugin -> !invalidPlugins.contains(plugin.name())));
  }
  
  public static void main(String[] args) throws IOException {
    generate();
  }
}
