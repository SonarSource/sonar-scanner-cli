/*
 * Sonar Runner - Distribution
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

import org.sonar.runner.impl.Constants;
import org.sonar.runner.impl.Logs;

import java.util.Properties;

class Cli {

  private boolean debugMode = false;
  private boolean displayVersionOnly = false;
  private boolean displayStackTrace = false;
  private Properties props = new Properties();

  public boolean isDebugMode() {
    return debugMode;
  }

  public boolean isDisplayVersionOnly() {
    return displayVersionOnly;
  }

  public boolean isDisplayStackTrace() {
    return displayStackTrace;
  }

  public Properties properties() {
    return props;
  }

  Cli parse(String[] args) {
    reset();
    props.putAll(System.getProperties());
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (i == 0 && !arg.startsWith("-")) {
        props.setProperty(Constants.TASK, arg);

      } else if ("-h".equals(arg) || "--help".equals(arg)) {
        printUsage();

      } else if ("-v".equals(arg) || "--version".equals(arg)) {
        displayVersionOnly = true;

      } else if ("-e".equals(arg) || "--errors".equals(arg)) {
        displayStackTrace = true;

      } else if ("-X".equals(arg) || "--debug".equals(arg)) {
        props.setProperty("sonar.verbose", "true");
        displayStackTrace = true;
        debugMode = true;
        Logs.setDebugEnabled(true);

      } else if ("-D".equals(arg) || "--define".equals(arg)) {
        i++;
        if (i >= args.length) {
          printError("Missing argument for option --define");
        }
        arg = args[i];
        appendPropertyTo(arg, props);

      } else if (arg.startsWith("-D")) {
        arg = arg.substring(2);
        appendPropertyTo(arg, props);

      } else {
        printError("Unrecognized option: " + arg);
      }
    }
    return this;
  }

  private void reset() {
    props.clear();
    debugMode = false;
    displayStackTrace = false;
    displayVersionOnly = false;
  }

  private void appendPropertyTo(String arg, Properties props) {
    final String key, value;
    int j = arg.indexOf('=');
    if (j == -1) {
      key = arg;
      value = "true";
    } else {
      key = arg.substring(0, j);
      value = arg.substring(j + 1);
    }
    props.setProperty(key, value);
  }

  private void printError(String message) {
    Logs.error(message);
    printUsage();
  }

  private void printUsage() {
    Logs.info("");
    Logs.info("usage: sonar-runner [options]");
    Logs.info("");
    Logs.info("Options:");
    Logs.info(" -D,--define <arg>     Define property");
    Logs.info(" -e,--errors           Produce execution error messages");
    Logs.info(" -h,--help             Display help information");
    Logs.info(" -v,--version          Display version information");
    Logs.info(" -X,--debug            Produce execution debug output");
    System.exit(0);
  }
}
