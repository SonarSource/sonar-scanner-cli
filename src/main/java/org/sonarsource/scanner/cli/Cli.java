/*
 * SonarQube Scanner
 * Copyright (C) 2011-2020 SonarSource SA
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

import java.util.Properties;
import org.sonarsource.scanner.api.ScannerProperties;

import static java.util.Arrays.asList;

class Cli {

  private boolean debugEnabled = false;
  private boolean displayVersionOnly = false;
  private boolean embedded = false;
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

  boolean isEmbedded() {
    return embedded;
  }

  Properties properties() {
    return props;
  }

  Cli parse(String[] args) {
    reset();
    props.putAll(System.getProperties());
    if (args.length > 0) {
      int pos = 0;
      do {
        pos = processNextArg(args, pos);
      } while (pos < args.length);
    }
    return this;
  }

  private int processNextArg(String[] args, int pos) {
    String arg = args[pos];
    if (pos == 0 && arg.charAt(0) != '-') {
      props.setProperty(ScannerProperties.TASK, arg);

    } else if (asList("-h", "--help").contains(arg)) {
      printUsage();
      exit.exit(Exit.SUCCESS);

    } else if (asList("-v", "--version").contains(arg)) {
      displayVersionOnly = true;

    } else if (asList("-e", "--errors").contains(arg)) {
      logger.info("Option -e/--errors is no longer supported and will be ignored");

    } else if (asList("-X", "--debug").contains(arg)) {
      props.setProperty("sonar.verbose", "true");
      debugEnabled = true;
      logger.setDebugEnabled(true);

    } else if (asList("-D", "--define").contains(arg)) {
      return processProp(args, pos);

    } else if ("--embedded".equals(arg)) {
      embedded = true;

    } else if (arg.startsWith("-D")) {
      arg = arg.substring(2);
      appendPropertyTo(arg, props);

    } else {
      printErrorAndExit("Unrecognized option: " + arg);
    }
    return pos + 1;
  }

  private int processProp(String[] args, int pos) {
    int valuePos = pos + 1;
    if (valuePos >= args.length) {
      printErrorAndExit("Missing argument for option -D/--define");
    } else {
      appendPropertyTo(args[valuePos], props);
    }
    return valuePos + 1;
  }

  private void reset() {
    props.clear();
    debugEnabled = false;
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

  private void printErrorAndExit(String message) {
    logger.error(message);
    printUsage();
    exit.exit(Exit.INTERNAL_ERROR);
  }

  private void printUsage() {
    logger.info("");
    logger.info("usage: sonar-scanner [options]");
    logger.info("");
    logger.info("Options:");
    logger.info(" -D,--define <arg>     Define property");
    logger.info(" -h,--help             Display help information");
    logger.info(" -v,--version          Display version information");
    logger.info(" -X,--debug            Produce execution debug output");
  }
}
