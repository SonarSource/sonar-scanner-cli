/*
 * SonarQube Runner - Implementation
 * Copyright (C) 2011 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.runner.impl;

import org.sonar.home.log.LogListener.Level;
import org.sonar.home.log.LogListener;

import javax.annotation.Nullable;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class Logs {
  private static LogListener listener = new PrintStreamLogListener(getDefaultFwdMap());
  private static boolean debugEnabled = false;

  private Logs() {
  }

  public static void setListener(@Nullable LogListener listener) {
    if (listener == null) {
      Logs.listener = new PrintStreamLogListener(getDefaultFwdMap());
    } else {
      Logs.listener = listener;
    }
  }

  public static LogListener getListener() {
    return Logs.listener;
  }

  public static void setDebugEnabled(boolean debugEnabled) {
    Logs.debugEnabled = debugEnabled;
  }

  public static boolean isDebugEnabled() {
    return debugEnabled;
  }

  public static void debug(String message) {
    if (isDebugEnabled()) {
      log(message, Level.DEBUG);
    }
  }

  public static void info(String message) {
    log(message, Level.INFO);
  }

  public static void warn(String message) {
    log(message, Level.WARN);
  }

  public static void error(String message) {
    log(message, Level.ERROR);
  }

  public static void error(String message, Throwable t) {
    log(message, Level.ERROR);
    if (t != null) {
      StringWriter sw = new StringWriter();

      t.printStackTrace(new PrintWriter(sw));
      String[] lines = sw.toString().split(System.getProperty("line.separator"));
      for (String l : lines) {
        log(l, Level.ERROR);
      }
    }
  }

  private static void log(String msg, Level level) {
    listener.log(msg, level);
  }

  /**
   * This is recreated every time to be sure we use the current {@link System#err} and {@link System#out}.
   */
  private static Map<Level, PrintStream> getDefaultFwdMap() {
    Map<Level, PrintStream> map = new EnumMap<>(Level.class);

    map.put(Level.ERROR, System.err);
    map.put(Level.WARN, System.out);
    map.put(Level.INFO, System.out);
    map.put(Level.DEBUG, System.out);
    map.put(Level.TRACE, System.out);
    return Collections.unmodifiableMap(map);
  }

  private static class PrintStreamLogListener implements LogListener {
    Map<Level, PrintStream> forwardMap;

    PrintStreamLogListener(Map<Level, PrintStream> forwardMap) {
      this.forwardMap = new EnumMap<>(forwardMap);
    }

    @Override
    public void log(String msg, Level level) {
      PrintStream ps = forwardMap.get(level);
      if (ps != null) {
        ps.append(level.toString() + ": " + msg + System.lineSeparator());
      }
    }
  }
}
