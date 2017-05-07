package com.github.forax.pro.helper;

/**
 * Platform that run pro.
 *
 * <p>All platforms specific code should be gathered in this class to ease porting.
 */
public enum Platform {
  WINDOWS("java.exe"),
  UNIX("java");

  private final String javaExecutableName;

  private Platform(String javaExecutableName) {
    this.javaExecutableName = javaExecutableName;
  }

  /**
   * Returns the name of the java executable for the current platform.
   *
   * @return the name of the java executable for the current platform.
   */
  public String javaExecutableName() {
    return javaExecutableName;
  }

  /**
   * Returns the current platform.
   *
   * @return the current platform.
   */
  public static Platform current() {
    boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
    return isWindows ? WINDOWS : UNIX;
  }
}
