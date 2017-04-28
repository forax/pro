module com.github.forax.pro.plugin.formatter {
  requires com.github.forax.pro.api;
  requires com.github.forax.pro.helper;

  provides com.github.forax.pro.api.Plugin
    with com.github.forax.pro.plugin.formatter.FormatterPlugin;
}
