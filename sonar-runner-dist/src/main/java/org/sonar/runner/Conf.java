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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

class Conf {
  private static final String RUNNER_HOME = "runner.home";
  private static final String RUNNER_SETTINGS = "runner.settings";
  private static final String PROJECT_HOME = "project.home";
  private static final String PROJECT_SETTINGS = "project.settings";

  private final Cli cli;

  Conf(Cli cli) {
    this.cli = cli;
  }

  Properties properties() throws IOException {
    Properties result = new Properties();
    result.putAll(loadGlobalProperties());
    result.putAll(loadProjectProperties());
    result.putAll(System.getProperties());
    result.putAll(cli.properties());

    if (result.containsKey(PROJECT_HOME)) {
      // the real property of the Sonar Runner is "sonar.projectBaseDir"
      String baseDir = result.getProperty(PROJECT_HOME);
      result.remove(PROJECT_HOME);
      result.put("sonar.projectBaseDir", baseDir);
    }
    return result;
  }

  private Properties loadGlobalProperties() throws IOException {
    File settingsFile = locatePropertiesFile(cli.properties(), RUNNER_HOME, "conf/sonar-runner.properties", RUNNER_SETTINGS);
    if (settingsFile != null && settingsFile.isFile() && settingsFile.exists()) {
      Logs.info("Runner configuration file: " + settingsFile.getAbsolutePath());
      return toProperties(settingsFile);
    }
    Logs.info("Runner configuration file: NONE");
    return new Properties();
  }

  private Properties loadProjectProperties() throws IOException {
    File settingsFile = locatePropertiesFile(cli.properties(), PROJECT_HOME, "sonar-project.properties", PROJECT_SETTINGS);
    if (settingsFile != null && settingsFile.isFile() && settingsFile.exists()) {
      Logs.info("Project configuration file: " + settingsFile.getAbsolutePath());
      return toProperties(settingsFile);
    }
    Logs.info("Project configuration file: NONE");
    return new Properties();
  }

  private File locatePropertiesFile(Properties props, String homeKey, String relativePathFromHome, String settingsKey) {
    File settingsFile = null;
    String runnerHome = props.getProperty(homeKey, "");
    if (!"".equals(runnerHome)) {
      settingsFile = new File(runnerHome, relativePathFromHome);
    }

    if (settingsFile == null || !settingsFile.exists()) {
      String settingsPath = props.getProperty(settingsKey, "");
      if (!"".equals(settingsPath)) {
        settingsFile = new File(settingsPath);
      }
    }
    return settingsFile;
  }

  private static Properties toProperties(File file) throws IOException {
    InputStream in = null;
    try {
      Properties properties = new Properties();
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
}
