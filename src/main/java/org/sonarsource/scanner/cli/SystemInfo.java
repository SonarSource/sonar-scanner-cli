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

import org.sonar.runner.api.RunnerVersion;

class SystemInfo {
  private static System2 system = new System2();

  private SystemInfo() {
  }

  static void setSystem(System2 system) {
    SystemInfo.system = system;
  }

  static void print(Logs logger) {
    logger.info("SonarQube Runner " + RunnerVersion.version());
    logger.info(java());
    logger.info(os());
    String runnerOpts = system.getenv("SONAR_RUNNER_OPTS");
    if (runnerOpts != null) {
      logger.info("SONAR_RUNNER_OPTS=" + runnerOpts);
    }
  }

  static String java() {
    StringBuilder sb = new StringBuilder();
    sb
      .append("Java ")
      .append(system.getProperty("java.version"))
      .append(" ")
      .append(system.getProperty("java.vendor"));
    String bits = system.getProperty("sun.arch.data.model");
    if ("32".equals(bits) || "64".equals(bits)) {
      sb.append(" (").append(bits).append("-bit)");
    }
    return sb.toString();
  }

  static String os() {
    StringBuilder sb = new StringBuilder();
    sb
      .append(system.getProperty("os.name"))
      .append(" ")
      .append(system.getProperty("os.version"))
      .append(" ")
      .append(system.getProperty("os.arch"));
    return sb.toString();
  }

  static class System2 {
    String getProperty(String key) {
      return System.getProperty(key);
    }

    String getenv(String key) {
      return System.getenv(key);
    }
  }
}
