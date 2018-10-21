package com.github.forax.pro.bootstrap.genbuilder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
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
    var config = new DefaultConfig();
    plugin.init(config);

    return ((Query)config.getOrThrow(plugin.name(), Object.class))._type_();
  }

  private static Stream<Object> builderClass(Stream<Plugin> plugins) {
    return Stream.of(
        "package com.github.forax.pro.builder;\n",
        "\n",
        "import com.github.forax.pro.BuilderSupport;\n",
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
    var className = builderName(name);
    return Stream.of(
        "  public static final ", className, " ", name, " =\n",
        "    BuilderSupport.createBuilderProxy(\"", name, "\", ", className ,".class);\n",
        "  \n",
        "  @SuppressWarnings(\"exports\")\n",
        "  @com.github.forax.pro.api.TypeCheckedConfig\n",
        "  public interface ", className, " {\n",
            stream(confType.getDeclaredMethods())
              .sorted(Comparator.comparing(Method::getName).thenComparing(Method::getParameterCount))
              .flatMap(m -> {  
                switch(m.getParameterCount()) {
                case 0:
                  return getterTemplate(m);
                case 1:
                  if (m.getParameterTypes()[0] == List.class) {
                    return Stream.of(setterTemplate(className, m, true), setterTemplate(className, m, false)).flatMap(t -> t);
                  }
                  return setterTemplate(className, m, false);
                default:
                  return Stream.empty();
                }
              }),
        "  }\n",
        "  \n"
        );
  }

  private static Stream<Object> getterTemplate(Method method) {
    var name = method.getName();
    var returnTypeName = method.getGenericReturnType().getTypeName();
    return Stream.concat(
             Stream.of(
               "    ", "@Deprecated", "\n").filter(__ -> method.isAnnotationPresent(Deprecated.class)),
             Stream.of(
               "    ", returnTypeName, " ", name, "();\n"
             )
           );
  }
  
  private static Stream<Object> setterTemplate(String className, Method method, boolean varargs) {
    var name = method.getName();
    var parameterType = method.getGenericParameterTypes()[0];
    var parameterTypeName = varargs? ((ParameterizedType)parameterType).getActualTypeArguments()[0].getTypeName() + "...": parameterType.getTypeName();
    return Stream.concat(
             Stream.of(
               "    ", "@Deprecated", "\n").filter(__ -> method.isAnnotationPresent(Deprecated.class)),
             Stream.of(
               "    ", className, " ", name, "(", parameterTypeName, " ", name, ");\n"
             )
           );
  }

  private static Stream<String> toStream(Object o) {
    if (!(o instanceof Stream)) {
      return Stream.of(o.toString());
    }
    return ((Stream<?>)o).flatMap(GenBuilder::toStream);
  }

  private static void template(Path directory, Stream<Plugin> plugins) throws IOException {
    var template = builderClass(plugins);
    var text = template.flatMap(GenBuilder::toStream).collect(joining());
    Files.write(directory.resolve("Builders.java"), text.getBytes(UTF_8));
  }

  public static void generate() throws IOException {
    var pluginDir = Path.of("target/image/plugins");
    var invalidPlugins = Set.of("convention", "uberpackager");
    var directory = Path.of("src/main/java/com.github.forax.pro.builder/com/github/forax/pro/builder");

    var plugins = Plugins.getAllPlugins(pluginDir);

    System.out.println("generate builders to " + directory);
    template(directory,
        plugins.stream().filter(plugin -> !invalidPlugins.contains(plugin.name())));
  }
}
