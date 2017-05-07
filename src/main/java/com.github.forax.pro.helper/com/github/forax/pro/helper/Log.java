package com.github.forax.pro.helper;

import java.util.Objects;
import java.util.function.Function;

/** Yet another minimal logger class */
public class Log {
  /** Log levels. */
  @SuppressWarnings("hiding")
  public enum Level {
    DEBUG,
    VERBOSE,
    INFO,
    QUIET;
    final int level;

    private Level() {
      this.level = 1 + ordinal();
    }

    /**
     * Get level from a name (not case sensitive)
     *
     * @param name name of the levels.
     * @return the corresponding level
     */
    public static Level of(String name) {
      return Level.valueOf(name.toUpperCase());
    }
  }

  private final String name;
  private final int level;

  private Log(String name, int level) {
    this.name = Objects.requireNonNull(name);
    this.level = level;
  }

  /**
   * Returns true is this logger is allowed to display message for the level taken as parameter
   *
   * @param level the level
   * @return true is this logger is allowed to display message for the level
   */
  /*public boolean allows(Level level) {
    return this.level <= level.level;
  }*/

  private static final int DEBUG = Level.DEBUG.level;

  private static final int VERBOSE = Level.VERBOSE.level;
  private static final int INFO = Level.INFO.level;

  /**
   * Creates a logger with a name and a level.
   *
   * @param name name of the logger.
   * @param level level of the logger.
   * @return a new logger
   */
  public static Log create(String name, String logLevel) {
    return create(name, Level.of(logLevel));
  }

  /**
   * Creates a logger with a name and a level.
   *
   * @param name name of the logger.
   * @param level level of the logger.
   * @return a new logger
   */
  public static Log create(String name, Level level) {
    return new Log("[" + name + "] ", level.level);
  }

  /**
   * log an error message.
   *
   * @param value value that will be sent to the message factory.
   * @param messageFactory function that will creates the error message.
   */
  public <T> void error(T value, Function<? super T, String> messageFactory) {
    System.err.println(name + messageFactory.apply(value));
  }

  /**
   * log an exception.
   *
   * @param exception the exception to log.
   */
  public void error(Throwable exception) {
    if (level == DEBUG) {
      exception.printStackTrace();
    } else {
      System.err.println(name + exception.getMessage());
    }
  }

  /**
   * log a debug message.
   *
   * @param value value that will be sent to the message factory.
   * @param messageFactory function that will creates the error message.
   */
  public <T> void debug(T value, Function<? super T, String> messageFactory) {
    if (level == DEBUG) {
      System.out.println(name + messageFactory.apply(value));
    }
  }

  /**
   * log a verbose message.
   *
   * @param value value that will be sent to the message factory.
   * @param messageFactory function that will creates the error message.
   */
  public <T> void verbose(T value, Function<? super T, String> messageFactory) {
    if (level <= VERBOSE) {
      System.out.println(name + messageFactory.apply(value));
    }
  }

  /**
   * log an info message.
   *
   * @param value value that will be sent to the message factory.
   * @param messageFactory function that will creates the error message.
   */
  public <T> void info(T value, Function<? super T, String> messageFactory) {
    if (level <= INFO) {
      System.out.println(name + messageFactory.apply(value));
    }
  }
}
