package com.github.forax.pro.daemon;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;

import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;

public interface DaemonService {
  interface PluginFacade {
    void watch(Plugin plugin, WatcherRegistry registry);
    int execute(Plugin plugin);
    
    static PluginFacade of(BiConsumer<Plugin, WatcherRegistry> watch, ToIntFunction<Plugin> execute) {
      return new PluginFacade() {
        @Override
        public void watch(Plugin plugin, WatcherRegistry registry) {
          watch.accept(plugin, registry);
        }
        @Override
        public int execute(Plugin plugin) {
          return execute.applyAsInt(plugin);
        }
      };
    }
  }
  
  void start();
  boolean isStarted();
  void stop();

  void run(List<Plugin> plugins, PluginFacade pluginFacade);
}