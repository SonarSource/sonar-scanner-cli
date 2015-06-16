/*
 * SonarQube Runner - Distribution
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
package org.sonar.runner;

import org.sonar.runner.impl.Logs;

import org.sonar.runner.api.RunnerVersion;

class SystemInfo {

  private SystemInfo() {
    // only static methods
  }

  static void print() {
    Logs.info("SonarQube Runner " + RunnerVersion.version());
    Logs.info(java());
    Logs.info(os());
    String runnerOpts = System.getenv("SONAR_RUNNER_OPTS");
    if (runnerOpts != null) {
      Logs.info("SONAR_RUNNER_OPTS=" + runnerOpts);
    }
  }

  static String java() {
    StringBuilder sb = new StringBuilder();
    sb
      .append("Java ")
      .append(System.getProperty("java.version"))
      .append(" ")
      .append(System.getProperty("java.vendor"));
    String bits = System.getProperty("sun.arch.data.model");
    if ("32".equals(bits) || "64".equals(bits)) {
      sb.append(" (").append(bits).append("-bit)");
    }
    return sb.toString();
  }

  static String os() {
    StringBuilder sb = new StringBuilder();
    sb
      .append(System.getProperty("os.name"))
      .append(" ")
      .append(System.getProperty("os.version"))
      .append(" ")
      .append(System.getProperty("os.arch"));
    return sb.toString();
  }
}
