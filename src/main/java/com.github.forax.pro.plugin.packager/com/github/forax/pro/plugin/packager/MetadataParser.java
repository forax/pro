package com.github.forax.pro.plugin.packager;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;

class MetadataParser {
  interface Metadata {
    String moduleName();
    Optional<String> version();
    Optional<String> mainClass();
    
    static Metadata parse(String data) {
      String module, mainClass;
      var slashIndex = data.lastIndexOf('/');
      if (slashIndex != -1) {
        mainClass = data.substring(slashIndex + 1);
        module = data.substring(0, slashIndex);
      } else {
        module = data;
        mainClass = null;
      }
      String moduleName, version;
      var dashIndex = module.lastIndexOf('@');
      if (dashIndex != -1) {
        version = module.substring(dashIndex + 1);
        moduleName = module.substring(0, dashIndex);
      } else {
        version = null;
        moduleName = module;
      }
      return new Metadata() {
        @Override
        public String moduleName() {
          return moduleName;
        }
        @Override
        public Optional<String> mainClass() {
          return Optional.ofNullable(mainClass);
        }
        @Override
        public Optional<String> version() {
          return Optional.ofNullable(version);
        }
      };
    }
  }

  static Map<String, Metadata> parse(List<String> moduleMetadata) {
    return moduleMetadata.stream()
      .map(Metadata::parse)
      .collect(toMap(Metadata::moduleName, identity()));
  }
}
