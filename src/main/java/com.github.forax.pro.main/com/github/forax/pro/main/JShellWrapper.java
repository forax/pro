package com.github.forax.pro.main;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ServiceLoader;
import javax.tools.Tool;

class JShellWrapper {
  static int run(InputStream in, OutputStream out, OutputStream err, String... arguments) {
    ServiceLoader<Tool> loader =
        ServiceLoader.load(Tool.class, JShellConfigRunner.class.getClassLoader());
    Tool jshell =
        loader
            .stream()
            .map(provider -> provider.get())
            .filter(tool -> tool.name().equals("jshell"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("can not find jshell"));
    return jshell.run(in, out, err, arguments);
  }
}
