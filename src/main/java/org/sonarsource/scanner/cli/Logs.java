/*
 * SonarScanner CLI
 * Copyright (C) 2011-2024 SonarSource SA
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
import org.sonarsource.scanner.lib.LogOutput;

public class Logs {
  private DateTimeFormatter timeFormatter;
  private boolean debugEnabled = false;
  private PrintStream stdOut;
  private PrintStream stdErr;

  public Logs(PrintStream stdOut, PrintStream stdErr) {
    this.stdErr = stdErr;
    this.stdOut = stdOut;
    this.timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
  }

  public void setDebugEnabled(boolean debugEnabled) {
    this.debugEnabled = debugEnabled;
  }

  public boolean isDebugEnabled() {
    return debugEnabled;
  }

  public void debug(String message) {
    if (isDebugEnabled()) {
      LocalTime currentTime = LocalTime.now();
      String timestamp = currentTime.format(timeFormatter);
      stdOut.println(timestamp + " DEBUG: " + message);
    }
  }

  public void info(String message) {
    print(stdOut, "INFO: " + message);
  }

  public void warn(String message) {
    print(stdOut, "WARN: " + message);
  }

  public void error(String message) {
    print(stdErr, "ERROR: " + message);
  }

  public void error(String message, Throwable t) {
    print(stdErr, "ERROR: " + message);
    t.printStackTrace(stdErr);
  }

  private void print(PrintStream stream, String msg) {
    if (debugEnabled) {
      LocalTime currentTime = LocalTime.now();
      String timestamp = currentTime.format(timeFormatter);
      stream.println(timestamp + " " + msg);
    } else {
      stream.println(msg);
    }
  }

  /**
   * Adapter for the scanner library.
   */
  public LogOutput getLogOutputAdapter() {
    return new LogOutputAdapter(this);
  }

  static class LogOutputAdapter implements LogOutput {
    private final Logs logs;

    public LogOutputAdapter(Logs logs) {
      this.logs = logs;
    }

    @Override
    public void log(String formattedMessage, Level level) {
      switch (level) {
        case TRACE, DEBUG:
          logs.debug(formattedMessage);
          break;
        case ERROR:
          logs.error(formattedMessage);
          break;
        case WARN:
          logs.warn(formattedMessage);
          break;
        case INFO:
        default:
          logs.info(formattedMessage);
      }
    }
  }
}
