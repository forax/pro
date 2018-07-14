package com.github.forax.pro.main;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.json.JSONTokener;

import com.github.forax.pro.Pro;
import com.github.forax.pro.main.runner.ConfigRunner;

public class JSONConfigRunner implements ConfigRunner {
  @Override
  public Optional<Runnable> accept(Path config, String[] arguments) {
    return Optional.<Runnable>of(() -> run(config, arguments))
             .filter(__ -> config.toString().endsWith(".json"));
  }
  
  private static void decode(String prefix, Map<?, ?> map, ArrayList<Object> pluginNames) {
    map.forEach((key, value) -> {
      String property = prefix.isEmpty()? key.toString(): prefix + '.' + key;
      if (value instanceof Map<?,?>) {
        Map<?, ?> valueMap = (Map<?,?>)value;
        decode(property, valueMap, null);
        return;
      }
      Object object;
      if (value instanceof List<?>) {
        List<?> valueList = (List<?>)value;
        if (pluginNames != null && key.equals("run")) {
          pluginNames.addAll(valueList);
          return;
        }
        object = valueList;
      } else {
        object = value;
      }
      Pro.set(property, object);
    });
  }
  
 
  private static void run(Path configFile, String... arguments) {
    //System.out.println("run with json " + configFile);
    
    var pluginNames = new ArrayList<>();
    try {
      try(var reader = Files.newBufferedReader(configFile)) {
        var tokener = new JSONTokener(reader);
        var object = new JSONObject(tokener);
        decode("", object.toMap(), pluginNames);
      }
    } catch(IOException e) {
      throw new UncheckedIOException(e);
    }
    
    Pro.set("pro.exitOnError", true);
    Pro.set("pro.arguments", String.join(",", arguments));
    
    //System.out.println("run " + String.join(" -> ", pluginNames));
    try {
      Pro.run(pluginNames);
    } catch(@SuppressWarnings("unused") Exception | Error e) {
      System.exit(1);
    }
  }
}
