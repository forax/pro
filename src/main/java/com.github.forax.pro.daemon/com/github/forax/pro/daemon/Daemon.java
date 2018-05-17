package com.github.forax.pro.daemon;

import java.util.List;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.Command;

/**
 * An interface to interact with a daemon that watch directories and
 * run a list of task if one of file the directories change.
 */
public interface Daemon {
  /**
   * start the daemon
   */
  void start();
  
  /**
   * Returns true if the daemon is started
   * @return true if the daemon is started
   */
  boolean isStarted();
  
  /**
   * stop the daemon
   */
  void stop();

  /**
   * Ask the daemon to run a task list with a configuration
   * @param tasks a list of task to execute if a file change
   * @param config the configuration to use when running the task
   */
  void execute(List<Command> tasks, Config config);
}