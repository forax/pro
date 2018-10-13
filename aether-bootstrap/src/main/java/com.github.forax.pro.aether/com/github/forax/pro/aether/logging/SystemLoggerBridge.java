package com.github.forax.pro.aether.logging;
/*
import java.lang.System.Logger.Level;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

public class SystemLoggerBridge implements Logger {
  private final String name;
  private final System.Logger systemLogger;
  
  SystemLoggerBridge(String name, System.Logger systemLogger) {
    this.name = Objects.requireNonNull(name);
    this.systemLogger = systemLogger;
  }

  @Override
  public String getName() {
    return name;
  }
  
  @Override
  public void debug(String message) {
    log(Level.DEBUG, null, message, null);
  }
  @Override
  public void debug(String message, Object arg1) {
    log(Level.DEBUG, null, message, null, arg1);
  }
  @Override
  public void debug(String message, Object... args) {
    log(Level.DEBUG, null, message, null, args);
  }
  @Override
  public void debug(String message, Throwable throwable) {
    log(Level.DEBUG, null, message, throwable);
  }
  @Override
  public void debug(Marker marker, String message) {
    log(Level.DEBUG, marker, message, null);
  }
  @Override
  public void debug(String message, Object arg1, Object arg2) {
    log(Level.DEBUG, null, message, null, arg1, arg2);
  }
  @Override
  public void debug(Marker marker, String message, Object arg1) {
    log(Level.DEBUG, null, message, null, arg1);
  }
  @Override
  public void debug(Marker marker, String message, Object... args) {
    log(Level.DEBUG, marker, message, null, args);
  }
  @Override
  public void debug(Marker marker, String message, Throwable throwable) {
    log(Level.DEBUG, marker, message, throwable);
  }
  @Override
  public void debug(Marker marker, String message, Object arg1, Object arg2) {
    log(Level.DEBUG, marker, message, null, arg1, arg2);
  }

  @Override
  public void error(String message) {
    log(Level.ERROR, null, message, null);
  }
  @Override
  public void error(String message, Object arg1) {
    log(Level.ERROR, null, message, null, arg1);
  }
  @Override
  public void error(String message, Object... args) {
    log(Level.ERROR, null, message, null, args);
  }
  @Override
  public void error(String message, Throwable throwable) {
    log(Level.ERROR, null, message, throwable);
  }
  @Override
  public void error(Marker marker, String message) {
    log(Level.ERROR, marker, message, null);
  }
  @Override
  public void error(String message, Object arg1, Object arg2) {
    log(Level.ERROR, null, message, null, arg1, arg2);
  }
  @Override
  public void error(Marker marker, String message, Object arg1) {
    log(Level.ERROR, null, message, null, arg1);
  }
  @Override
  public void error(Marker marker, String message, Object... args) {
    log(Level.ERROR, marker, message, null, args);
  }
  @Override
  public void error(Marker marker, String message, Throwable throwable) {
    log(Level.ERROR, marker, message, throwable);
  }
  @Override
  public void error(Marker marker, String message, Object arg1, Object arg2) {
    log(Level.ERROR, marker, message, null, arg1, arg2);
  }

 
  @Override
  public void info(String message) {
    log(Level.INFO, null, message, null);
  }
  @Override
  public void info(String message, Object arg1) {
    log(Level.INFO, null, message, null, arg1);
  }
  @Override
  public void info(String message, Object... args) {
    log(Level.INFO, null, message, null, args);
  }
  @Override
  public void info(String message, Throwable throwable) {
    log(Level.INFO, null, message, throwable);
  }
  @Override
  public void info(Marker marker, String message) {
    log(Level.INFO, marker, message, null);
  }
  @Override
  public void info(String message, Object arg1, Object arg2) {
    log(Level.INFO, null, message, null, arg1, arg2);
  }
  @Override
  public void info(Marker marker, String message, Object arg1) {
    log(Level.INFO, null, message, null, arg1);
  }
  @Override
  public void info(Marker marker, String message, Object... args) {
    log(Level.INFO, marker, message, null, args);
  }
  @Override
  public void info(Marker marker, String message, Throwable throwable) {
    log(Level.INFO, marker, message, throwable);
  }
  @Override
  public void info(Marker marker, String message, Object arg1, Object arg2) {
    log(Level.INFO, marker, message, null, arg1, arg2);
  }

  

  @Override
  public void trace(String message) {
    log(Level.TRACE, null, message, null);
  }
  @Override
  public void trace(String message, Object arg1) {
    log(Level.TRACE, null, message, null, arg1);
  }
  @Override
  public void trace(String message, Object... args) {
    log(Level.TRACE, null, message, null, args);
  }
  @Override
  public void trace(String message, Throwable throwable) {
    log(Level.TRACE, null, message, throwable);
  }
  @Override
  public void trace(Marker marker, String message) {
    log(Level.TRACE, marker, message, null);
  }
  @Override
  public void trace(String message, Object arg1, Object arg2) {
    log(Level.TRACE, null, message, null, arg1, arg2);
  }
  @Override
  public void trace(Marker marker, String message, Object arg1) {
    log(Level.TRACE, null, message, null, arg1);
  }
  @Override
  public void trace(Marker marker, String message, Object... args) {
    log(Level.TRACE, marker, message, null, args);
  }
  @Override
  public void trace(Marker marker, String message, Throwable throwable) {
    log(Level.TRACE, marker, message, throwable);
  }
  @Override
  public void trace(Marker marker, String message, Object arg1, Object arg2) {
    log(Level.TRACE, marker, message, null, arg1, arg2);
  }

  
  @Override
  public void warn(String message) {
    log(Level.WARNING, null, message, null);
  }
  @Override
  public void warn(String message, Object arg1) {
    log(Level.WARNING, null, message, null, arg1);
  }
  @Override
  public void warn(String message, Object... args) {
    log(Level.WARNING, null, message, null, args);
  }
  @Override
  public void warn(String message, Throwable throwable) {
    log(Level.WARNING, null, message, throwable);
  }
  @Override
  public void warn(Marker marker, String message) {
    log(Level.WARNING, marker, message, null);
  }
  @Override
  public void warn(String message, Object arg1, Object arg2) {
    log(Level.WARNING, null, message, null, arg1, arg2);
  }
  @Override
  public void warn(Marker marker, String message, Object arg1) {
    log(Level.WARNING, null, message, null, arg1);
  }
  @Override
  public void warn(Marker marker, String message, Object... args) {
    log(Level.WARNING, marker, message, null, args);
  }
  @Override
  public void warn(Marker marker, String message, Throwable throwable) {
    log(Level.WARNING, marker, message, throwable);
  }
  @Override
  public void warn(Marker marker, String message, Object arg1, Object arg2) {
    log(Level.WARNING, marker, message, null, arg1, arg2);
  }
  
  
  @Override
  public boolean isDebugEnabled() {
    return isEnabled(Level.DEBUG, null);
  }
  @Override
  public boolean isDebugEnabled(Marker marker) {
    return isEnabled(Level.DEBUG, marker);
  }
  @Override
  public boolean isErrorEnabled() {
    return isEnabled(Level.ERROR, null);
  }
  @Override
  public boolean isErrorEnabled(Marker marker) {
    return isEnabled(Level.ERROR, marker);
  }
  @Override
  public boolean isInfoEnabled() {
    return isEnabled(Level.INFO, null);
  }
  @Override
  public boolean isInfoEnabled(Marker marker) {
    return isEnabled(Level.INFO, marker);
  }
  @Override
  public boolean isTraceEnabled() {
    return isEnabled(Level.TRACE, null);
  }
  @Override
  public boolean isTraceEnabled(Marker marker) {
    return isEnabled(Level.TRACE, marker);
  }
  @Override
  public boolean isWarnEnabled() {
    return isEnabled(Level.WARNING, null);
  }
  @Override
  public boolean isWarnEnabled(Marker marker) {
    return isEnabled(Level.WARNING, marker);
  }
  
  
  private boolean isEnabled(Level level, @SuppressWarnings("unused") Marker marker) {
    return systemLogger.isLoggable(level);
  }
  
  private void log(Level level, @SuppressWarnings("unused") Marker marker, String message, Throwable throwable, Object... args) {
    String text = MessageFormatter.arrayFormat(message, args).getMessage();
    systemLogger.log(level, text, throwable);
  }
}*/
