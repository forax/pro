package com.github.forax.pro.api.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

import com.github.forax.pro.api.Plugin;

public class Plugins {
  public static List<Plugin> getAllPlugins() {
    ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class, Plugin.class.getClassLoader());
    ArrayList<Plugin> plugins = new ArrayList<>();
    loader.forEach(plugins::add);
    plugins.sort(Comparator.comparing(Plugin::name));  // have a stable order
    return plugins;
  }
}
