package com.github.forax.pro.daemon;

import java.util.List;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.Plugin;

public interface DaemonService {
  void start();
  boolean isStarted();
  void stop();

  void run(List<Plugin> plugins, Config config);
}