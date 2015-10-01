/*
 * SonarQube Runner - API
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
package org.sonar.runner.api;


import org.sonar.runner.cache.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;

class LoggerAdapter implements Logger {
  private LogOutput logOutput;

  LoggerAdapter(LogOutput logOutput) {
    this.logOutput = logOutput;
  }

  @Override
  public void warn(String msg) {
    logOutput.log(msg, LogOutput.Level.WARN);
  }

  @Override
  public void info(String msg) {
    logOutput.log(msg, LogOutput.Level.INFO);
  }

  @Override
  public void error(String msg, Throwable t) {
    StringWriter errors = new StringWriter();
    t.printStackTrace(new PrintWriter(errors));
    logOutput.log(msg + "\n" + errors.toString(), LogOutput.Level.ERROR);
  }

  @Override
  public void error(String msg) {
    logOutput.log(msg, LogOutput.Level.ERROR);
  }

  @Override
  public void debug(String msg) {
    logOutput.log(msg, LogOutput.Level.DEBUG);
  }
}
