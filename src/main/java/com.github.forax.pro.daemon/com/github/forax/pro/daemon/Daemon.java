package com.github.forax.pro.daemon;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.Plugin;
import java.util.List;

/**
 * An interface to interact with a daemon that watch directories and run a list of plugins if one of
 * file the directories change.
 */
public interface Daemon {
  /** start the daemon */
  void start();

  /**
   * Returns true if the daemon is started
   *
   * @return true if the daemon is started
   */
  boolean isStarted();

  /** stop the daemon */
  void stop();

  /**
   * Ask the daemon to run a plugin list with a configuration
   *
   * @param plugins a list of plugin to execute if a file change
   * @param config the configuration to use when running the plugins
   */
  void run(List<Plugin> plugins, Config config);
}
