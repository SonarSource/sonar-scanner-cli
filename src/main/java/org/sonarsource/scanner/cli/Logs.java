/*
 * SonarQube Scanner
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
package org.sonarsource.scanner.cli;

import java.io.PrintStream;

public class Logs {
  private boolean debugEnabled = false;
  private boolean displayStackTrace = false;
  private PrintStream stdOut;
  private PrintStream stdErr;

  public Logs(PrintStream stdOut, PrintStream stdErr) {
    this.stdErr = stdErr;
    this.stdOut = stdOut;
  }

  public void setDebugEnabled(boolean debugEnabled) {
    this.debugEnabled = debugEnabled;
  }

  public void setDisplayStackTrace(boolean displayStackTrace) {
    this.displayStackTrace = displayStackTrace;
  }

  public boolean isDebugEnabled() {
    return debugEnabled;
  }

  public void debug(String message) {
    if (isDebugEnabled()) {
      stdOut.println("DEBUG: " + message);
    }
  }

  public void info(String message) {
    stdOut.println("INFO: " + message);
  }

  public void warn(String message) {
    stdOut.println("WARN: " + message);
  }

  public void error(String message) {
    stdErr.println("ERROR: " + message);
  }

  public void error(String message, Throwable t) {
    stdErr.println("ERROR: " + message);
    if (t != null && displayStackTrace) {
      t.printStackTrace(stdErr);
    }
  }
}
