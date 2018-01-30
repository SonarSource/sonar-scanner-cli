/*
 * SonarQube Scanner
 * Copyright (C) 2011-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.scanner.cli;

import java.io.PrintStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Logs {
  private DateTimeFormatter timeFormatter;
  private boolean showTimestamp = false;
  private PrintStream stdOut;
  private PrintStream stdErr;
  private LogLevel logLevel;

  public Logs(PrintStream stdOut, PrintStream stdErr) {
    this(stdOut, stdErr, LogLevel.INFO);
  }

  public Logs(PrintStream stdOut, PrintStream stdErr, LogLevel logLevel) {
    this.stdOut = stdOut;
    this.stdErr = stdErr;
    this.logLevel = logLevel;
    this.timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
  }

  public void setShowTimestamp(boolean showTimestamp) {
    this.showTimestamp = showTimestamp;
  }

  public void setLogLevel(LogLevel logLevel) {
    this.logLevel = logLevel;
  }

  public void debug(String message) {
    if (logLevel.priority <= LogLevel.DEBUG.priority) {
      LocalTime currentTime = LocalTime.now();
      String timestamp = currentTime.format(timeFormatter);
      stdOut.println(timestamp + " " + LogLevel.DEBUG.marker + ": " + message);
    }
  }

  public void info(String message) {
    if (logLevel.priority <= LogLevel.INFO.priority) {
      print(stdOut, LogLevel.INFO.marker + ": " + message);
    }
  }

  public void warn(String message) {
    if (logLevel.priority <= LogLevel.WARN.priority) {
      print(stdErr, LogLevel.WARN.marker + ": " + message);
    }
  }

  public void error(String message) {
    if (logLevel.priority <= LogLevel.ERROR.priority) {
      print(stdErr, LogLevel.ERROR.marker + ": " + message);
    }
  }

  public void error(String message, Throwable t) {
    print(stdErr, "ERROR: " + message);
    t.printStackTrace(stdErr);
  }

  private void print(PrintStream stream, String msg) {
    if (showTimestamp) {
      LocalTime currentTime = LocalTime.now();
      String timestamp = currentTime.format(timeFormatter);
      stream.println(timestamp + " " + msg);
    } else {
      stream.println(msg);
    }
  }

  public enum LogLevel {
    DEBUG(2, "DEBUG"), INFO(3, "INFO"), WARN(4, "WARN"), ERROR(5, "ERROR");

    private int priority;
    private String marker;

    LogLevel(int priority, String marker) {
      this.priority = priority;
      this.marker = marker;
    }
  }
}
