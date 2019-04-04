package com.github.forax.pro.main;

import static com.github.forax.pro.helper.util.Unchecked.suppress;
import static java.nio.file.Files.newBufferedReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.json.JSONTokener;

import com.github.forax.pro.Pro;
import com.github.forax.pro.main.runner.ConfigRunner;
import com.github.forax.pro.main.runner.PropertySequence;

public class JSONConfigRunner implements ConfigRunner {
  @Override
  public Optional<Runnable> accept(Path config, PropertySequence propertySeq, List<String> arguments) {
    return Optional.of(suppress(() -> run(config, propertySeq, arguments)))
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
  
 
  private static void run(Path configFile, PropertySequence propertySeq, List<String> arguments) throws IOException {
    //System.out.println("run with json " + configFile);
    
    var pluginNames = new ArrayList<>();
    try(var reader = newBufferedReader(configFile)) {
      var tokener = new JSONTokener(reader);
      var object = new JSONObject(tokener);
      decode("", object.toMap(), pluginNames);
    }
    
    Pro.set("pro.exitOnError", true);
    Pro.set("pro.arguments", String.join(",", arguments));
    propertySeq.forEach((key, value) -> Pro.set(key, value.toString()));
    
    //System.out.println("run " + String.join(" -> ", pluginNames));
    try {
      Pro.run(pluginNames);
    } catch(@SuppressWarnings("unused") Exception | Error e) {
      System.exit(1);
    }
  }
}
