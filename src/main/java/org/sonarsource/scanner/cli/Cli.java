/*
 * SonarScanner CLI
 * Copyright (C) 2011-2025 SonarSource SA
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

import ch.qos.logback.classic.Level;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.asList;

class Cli {

  private static final Logger LOG = LoggerFactory.getLogger(Cli.class);

  private boolean debugEnabled = false;
  private boolean displayVersionOnly = false;
  private boolean embedded = false;
  private String invokedFrom = "";
  private final Properties props = new Properties();
  private final Exit exit;

  public Cli(Exit exit) {
    this.exit = exit;
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

  String getInvokedFrom() {
    return invokedFrom;
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
    if (asList("-h", "--help").contains(arg)) {
      printUsage();
      exit.exit(Exit.SUCCESS);

    } else if (asList("-v", "--version").contains(arg)) {
      displayVersionOnly = true;

    } else if (asList("-e", "--errors").contains(arg)) {
      LOG
        .info("Option -e/--errors is no longer supported and will be ignored");

    } else if (asList("-X", "--debug").contains(arg)) {
      props.setProperty("sonar.verbose", "true");
      debugEnabled = true;
      var rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
      rootLogger.setLevel(Level.DEBUG);
    } else if (asList("-D", "--define").contains(arg)) {
      return processProp(args, pos);

    } else if ("--embedded".equals(arg)) {
      LOG.info(
        "Option --embedded is deprecated and will be removed in a future release.");
      embedded = true;

    } else if (arg.startsWith("--from")) {
      embedded = true;
      if (arg.length() > "--from=".length()) {
        invokedFrom = arg.substring("--from=".length());
      }

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
    Object oldValue = props.setProperty(key, value);
    if (oldValue != null) {
      LOG.warn("Property '{}' with value '{}' is overridden with value '{}'", key, oldValue, value);
    }
  }

  private void printErrorAndExit(String message) {
    LOG.error(message);
    printUsage();
    exit.exit(Exit.INTERNAL_ERROR);
  }

  private static void printUsage() {
    System.out.println();
    System.out.println("usage: sonar-scanner [options]");
    System.out.println();
    System.out.println("Options:");
    System.out.println(" -D,--define <arg>     Define property");
    System.out.println(" -h,--help             Display help information");
    System.out.println(" -v,--version          Display version information");
    System.out.println(" -X,--debug            Produce execution debug output");
  }
}
