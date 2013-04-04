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

import org.sonar.runner.api.EmbeddedRunner;
import org.sonar.runner.api.Runner;
import org.sonar.runner.api.RunnerVersion;
import org.sonar.runner.impl.Constants;
import org.sonar.runner.impl.Logs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Arguments :
 * <ul>
 * <li>runner.home: optional path to runner home (root directory with sub-directories bin, lib and conf)</li>
 * <li>runner.settings: optional path to runner global settings, usually ${runner.home}/conf/sonar-runner.properties.
 * This property is used only if ${runner.home} is not defined</li>
 * <li>project.home: path to project root directory. If not set, then it's supposed to be the directory where the runner is executed</li>
 * <li>project.settings: optional path to project settings. Default value is ${project.home}/sonar-project.properties.</li>
 * </ul>
 *
 * @since 1.0
 */
public final class Main {

  private static final String RUNNER_HOME = "runner.home";
  private static final String RUNNER_SETTINGS = "runner.settings";
  private static final String PROJECT_HOME = "project.home";
  private static final String PROJECT_SETTINGS = "project.settings";

  boolean debugMode = false;
  boolean displayVersionOnly = false;
  boolean displayStackTrace = false;

  /**
   * Entry point of the program.
   */
  public static void main(String[] args) {
    new Main().execute(args);
  }

  Main() {
  }

  private void execute(String[] args) {
    Properties argsProperties = parseArguments(args);
    System.out.println("Runner version: " + RunnerVersion.version());
    System.out.println("Java version: " + System.getProperty("java.version", "<unknown>")
      + ", vendor: " + System.getProperty("java.vendor", "<unknown>"));
    System.out
      .println("OS name: \"" + System.getProperty("os.name") + "\", version: \"" + System.getProperty("os.version") + "\", arch: \"" + System.getProperty("os.arch") + "\"");
    if (!displayVersionOnly) {
      int result = execute(argsProperties);
      System.exit(result);
    }
  }

  private int execute(Properties argsProperties) {
    if (displayStackTrace) {
      Logs.info("Error stacktraces are turned on.");
    }
    Stats stats = new Stats().start();
    try {
      Properties properties = loadProperties(argsProperties);
      Runner runner = EmbeddedRunner.create().addProperties(properties);

//      Logs.debug("Other system properties:");
//      Logs.debug("  - sun.arch.data.model: \"" + System.getProperty("sun.arch.data.model") + "\"");
//      Logs.info("Server: " + runner.getSonarServerURL());
//      try {
//        Logs.info("Work directory: " + runner.getWorkDir().getCanonicalPath());
      //Logs.info("Cache directory: " + runner.getCache().getCacheLocation());
//      } catch (IOException e) {
//        throw new RunnerException("Unable to resolve directory", e);
//      }
      runner.execute();
    } catch (Exception e) {
      displayExecutionResult(stats, "FAILURE");
      showError("Error during Sonar runner execution", e, displayStackTrace);
      return 1;
    }
    displayExecutionResult(stats, "SUCCESS");
    return 0;
  }

  private void displayExecutionResult(Stats stats, String resultMsg) {
    Logs.info("------------------------------------------------------------------------");
    Logs.info("EXECUTION " + resultMsg);
    Logs.info("------------------------------------------------------------------------");
    stats.stop();
    Logs.info("------------------------------------------------------------------------");
  }

  public void showError(String message, Throwable e, boolean showStackTrace) {
    if (showStackTrace) {
      Logs.error(message, e);
      if (!debugMode) {
        Logs.error("");
        suggestDebugMode();
      }
    } else {
      Logs.error(message);
      if (e != null) {
        Logs.error(e.getMessage());
        String previousMsg = "";
        for (Throwable cause = e.getCause(); cause != null
          && cause.getMessage() != null
          && !cause.getMessage().equals(previousMsg); cause = cause.getCause()) {
          Logs.error("Caused by: " + cause.getMessage());
          previousMsg = cause.getMessage();
        }
      }
      Logs.error("");
      Logs.error("To see the full stack trace of the errors, re-run Sonar Runner with the -e switch.");
      if (!debugMode) {
        suggestDebugMode();
      }
    }
  }

  private void suggestDebugMode() {
    Logs.error("Re-run Sonar Runner using the -X switch to enable full debug logging.");
  }

  Properties loadProperties(Properties arguments) throws IOException {
    Properties props = new Properties();
    props.putAll(System.getProperties());
    props.putAll(arguments);

    Properties result = new Properties();
    result.putAll(loadGlobalProperties(arguments));
    result.putAll(loadProjectProperties(arguments));
    result.putAll(props);
    return result;
  }

  Properties loadGlobalProperties(Properties argsProperties) throws IOException {
    Properties commandLineProps = new Properties();
    commandLineProps.putAll(System.getProperties());
    commandLineProps.putAll(argsProperties);

    Properties result = new Properties();
    result.putAll(loadRunnerConfiguration(commandLineProps));
    result.putAll(commandLineProps);

    return result;
  }

  Properties loadProjectProperties(Properties argsProperties) throws IOException {
    Properties commandLineProps = new Properties();
    commandLineProps.putAll(System.getProperties());
    commandLineProps.putAll(argsProperties);

    Properties result = new Properties();
    result.putAll(loadProjectConfiguration(commandLineProps));
    result.putAll(commandLineProps);

    if (result.containsKey(PROJECT_HOME)) {
      // the real property of the Sonar Runner is "sonar.projectDir"
      String baseDir = result.getProperty(PROJECT_HOME);
      result.remove(PROJECT_HOME);

      result.put("sonar.projectBaseDir", baseDir);
    }

    return result;
  }

  Properties loadRunnerConfiguration(Properties props) throws IOException {
    File settingsFile = locatePropertiesFile(props, RUNNER_HOME, "conf/sonar-runner.properties", RUNNER_SETTINGS);
    if (settingsFile != null && settingsFile.isFile() && settingsFile.exists()) {
      Logs.info("Runner configuration file: " + settingsFile.getAbsolutePath());
      return toProperties(settingsFile);
    }
    Logs.info("Runner configuration file: NONE");
    return new Properties();
  }

  private Properties loadProjectConfiguration(Properties props) throws IOException {
    File settingsFile = locatePropertiesFile(props, PROJECT_HOME, "sonar-project.properties", PROJECT_SETTINGS);
    if (settingsFile != null && settingsFile.isFile() && settingsFile.exists()) {
      Logs.info("Project configuration file: " + settingsFile.getAbsolutePath());
      return toProperties(settingsFile);
    }
    Logs.info("Project configuration file: NONE");
    return new Properties();
  }

  private File locatePropertiesFile(Properties props, String homeKey, String relativePathFromHome, String settingsKey) {
    File settingsFile = null;
    String runnerHome = props.getProperty(homeKey);
    if (runnerHome != null && !"".equals(runnerHome)) {
      settingsFile = new File(runnerHome, relativePathFromHome);
    }

    if (settingsFile == null || !settingsFile.exists()) {
      String settingsPath = props.getProperty(settingsKey);
      if (settingsPath != null && !"".equals(settingsPath)) {
        settingsFile = new File(settingsPath);
      }
    }
    return settingsFile;
  }

  private Properties toProperties(File file) throws IOException {
    InputStream in = null;
    Properties properties = new Properties();
    try {
      in = new FileInputStream(file);
      properties.load(in);
      return properties;

    } catch (Exception e) {
      throw new IllegalStateException("Fail to load file: " + file.getAbsolutePath(), e);

    } finally {
      if (in != null) {
        in.close();
      }
    }
  }

  Properties parseArguments(String[] args) {
    Properties props = new Properties();
    int i = 0;
    if (args.length > 0 && !args[0].startsWith("-")) {
      String task = args[0];
      props.setProperty(Constants.TASK, task);
      i++;
    }
    for (; i < args.length; i++) {
      String arg = args[i];
      if ("-h".equals(arg) || "--help".equals(arg)) {
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
    return props;
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
    Logs.info("usage: sonar-runner [command] [options]");
    Logs.info("");
    Logs.info("Command:");
    Logs.info(" analyse-project       Run Sonar analysis task on the current project (default)");
    Logs.info(" list-tasks            Display all tasks available");
    Logs.info("Options:");
    Logs.info(" -D,--define <arg>     Define property");
    Logs.info(" -e,--errors           Produce execution error messages");
    Logs.info(" -h,--help             Display help information");
    Logs.info(" -v,--version          Display version information");
    Logs.info(" -X,--debug            Produce execution debug output");
    System.exit(0);
  }
}
