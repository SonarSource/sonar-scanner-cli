/*
 * SonarQube Scanner
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.Properties;
import org.sonarsource.scanner.api.ScannerProperties;

class Cli {

  private boolean debugEnabled = false;
  private boolean displayVersionOnly = false;
  private boolean displayStackTrace = false;
  private boolean interactive = false;
  private final Properties props = new Properties();
  private final Exit exit;
  private final Logs logger;

  public Cli(Exit exit, Logs logger) {
    this.exit = exit;
    this.logger = logger;
  }

  boolean isDebugEnabled() {
    return debugEnabled;
  }

  boolean isDisplayVersionOnly() {
    return displayVersionOnly;
  }

  boolean isDisplayStackTrace() {
    return displayStackTrace;
  }

  boolean isInteractive() {
    return interactive;
  }

  Properties properties() {
    return props;
  }

  Cli parse(String[] args) {
    reset();
    props.putAll(System.getProperties());
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (i == 0 && arg.charAt(0) != '-') {
        props.setProperty(ScannerProperties.TASK, arg);

      } else if ("-h".equals(arg) || "--help".equals(arg)) {
        printUsage();

      } else if ("-v".equals(arg) || "--version".equals(arg)) {
        displayVersionOnly = true;

      } else if ("-e".equals(arg) || "--errors".equals(arg)) {
        displayStackTrace = true;
        logger.setDisplayStackTrace(true);

      } else if ("-X".equals(arg) || "--debug".equals(arg)) {
        props.setProperty("sonar.verbose", "true");
        debugEnabled = true;
        logger.setDebugEnabled(true);
        logger.setDisplayStackTrace(true);

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

      } else if ("-i".equals(arg) || "--interactive".equals(arg)) {
        interactive = true;

      } else {
        printError("Unrecognized option: " + arg);
      }
    }

    return this;
  }

  public void verify() {
    if ("fork".equals(props.getProperty("sonarRunner.mode")) && isInteractive()) {
      printError("Cannot run interactively in fork mode.");
    }
  }

  private void reset() {
    props.clear();
    debugEnabled = false;
    displayStackTrace = false;
    displayVersionOnly = false;
  }

  private static void appendPropertyTo(String arg, Properties props) {
    final String key;
    final String value;
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
    logger.error(message);
    printUsage();
  }

  private void printUsage() {
    logger.info("");
    logger.info("usage: sonar-runner [options]");
    logger.info("");
    logger.info("Options:");
    logger.info(" -D,--define <arg>     Define property");
    logger.info(" -e,--errors           Produce execution error messages");
    logger.info(" -h,--help             Display help information");
    logger.info(" -v,--version          Display version information");
    logger.info(" -X,--debug            Produce execution debug output");
    logger.info(" -i,--interactive      Run interactively");
    exit.exit(Exit.SUCCESS);
  }
}
