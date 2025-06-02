/*
 * SonarScanner CLI
 * Copyright (C) 2011-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.scanner.cli;

import java.io.PrintStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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
}
