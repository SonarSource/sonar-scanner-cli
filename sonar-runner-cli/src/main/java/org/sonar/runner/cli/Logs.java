/*
 * SonarQube Runner - CLI - Distribution
 * Copyright (C) 2011 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.runner.cli;

public class Logs {

  private Logs() {
  }

  private static boolean debugEnabled = false;
  private static boolean displayStackTrace = false;

  public static void setDebugEnabled(boolean debugEnabled) {
    Logs.debugEnabled = debugEnabled;
  }

  public static void setDisplayStackTrace(boolean displayStackTrace) {
    Logs.displayStackTrace = displayStackTrace;
  }

  public static boolean isDebugEnabled() {
    return debugEnabled;
  }

  public static void debug(String message) {
    if (isDebugEnabled()) {
      System.out.println("DEBUG: " + message);
    }
  }

  public static void info(String message) {
    System.out.println("INFO: " + message);
  }

  public static void warn(String message) {
    System.out.println("WARN: " + message);
  }

  public static void error(String message) {
    System.err.println("ERROR: " + message);
  }

  public static void error(String message, Throwable t) {
    System.err.println("ERROR: " + message);
    if (t != null && displayStackTrace) {
      t.printStackTrace(System.err);
    }
  }
}
