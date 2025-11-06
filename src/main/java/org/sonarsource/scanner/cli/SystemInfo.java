/*
 * SonarScanner CLI
 * Copyright (C) 2011-2025 SonarSource Sàrl
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

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SystemInfo {
  private static final Logger LOG = LoggerFactory.getLogger(SystemInfo.class);

  private static final Set<String> SENSITIVE_JVM_ARGUMENTS = Set.of(
    "-Dsonar.login",
    "-Dsonar.password",
    "-Dsonar.token");
  private static final Pattern PATTERN_ARGUMENT_SEPARATOR = Pattern.compile("\\s+");
  private static System2 system = new System2();

  private SystemInfo() {
  }

  static void setSystem(System2 system) {
    SystemInfo.system = system;
  }

  static void print() {
    LOG.info("SonarScanner CLI {}", ScannerVersion.version());
    LOG.atDebug().log(SystemInfo::java);
    LOG.atInfo().log(SystemInfo::os);
    String scannerOpts = system.getenv("SONAR_SCANNER_OPTS");
    if (scannerOpts != null) {
      LOG.atDebug().addArgument(() -> redactSensitiveArguments(scannerOpts)).log("SONAR_SCANNER_OPTS={}");
    }
  }

  private static String redactSensitiveArguments(String scannerOpts) {
    return PATTERN_ARGUMENT_SEPARATOR.splitAsStream(scannerOpts)
      .map(SystemInfo::redactArgumentIfSensistive)
      .collect(Collectors.joining(" "));
  }

  private static String redactArgumentIfSensistive(String argument) {
    String[] elems = argument.split("=");
    if (elems.length > 0 && SENSITIVE_JVM_ARGUMENTS.contains(elems[0])) {
      return elems[0] + "=*";
    }
    return argument;
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
    return system.getProperty("os.name")
      + " "
      + system.getProperty("os.version")
      + " "
      + system.getProperty("os.arch");
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
