/*
 * Sonar Standalone Runner
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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.EnvironmentConfiguration;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.Batch;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.batch.bootstrapper.ProjectDefinition;
import org.sonar.batch.bootstrapper.Reactor;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Launcher {

  private Runner runner;

  public Launcher(Runner runner) {
    this.runner = runner;
  }

  /**
   * This method invoked from {@link Main}. Do not rename it.
   */
  public void execute() {
    ProjectDefinition project = defineProject();
    Configuration initialConfiguration = getInitialConfiguration(project);
    initLogging(initialConfiguration);
    executeBatch(project, initialConfiguration);
  }

  private void executeBatch(ProjectDefinition project, Configuration initialConfiguration) {
    Reactor reactor = new Reactor(project);
    Batch batch = new Batch(initialConfiguration, new EnvironmentInformation("Runner", runner.getRunnerVersion()), reactor);
    batch.execute();
  }

  private void initLogging(Configuration initialConfiguration) {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    JoranConfigurator jc = new JoranConfigurator();
    jc.setContext(context);
    context.reset();
    InputStream input = Batch.class.getResourceAsStream("/org/sonar/batch/logback.xml");
    System.setProperty("ROOT_LOGGER_LEVEL", runner.isDebug() ? "DEBUG" : "INFO");
    context.putProperty("SQL_LOGGER_LEVEL", getSqlLevel(initialConfiguration));// since 2.14. Ignored on previous versions.
    context.putProperty("SQL_RESULTS_LOGGER_LEVEL", getSqlResultsLevel(initialConfiguration));// since 2.14. Ignored on previous versions.
    try {
      jc.doConfigure(input);

    } catch (JoranException e) {
      throw new SonarException("can not initialize logging", e);

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  protected static String getSqlLevel(Configuration config) {
    boolean showSql = config.getBoolean("sonar.showSql", false);
    return showSql ? "DEBUG" : "WARN";
  }

  protected static String getSqlResultsLevel(Configuration config) {
    boolean showSql = config.getBoolean("sonar.showSqlResults", false);
    return showSql ? "DEBUG" : "WARN";
  }

  private ProjectDefinition defineProject() {
    File baseDir = runner.getProjectDir();
    Properties properties = runner.getProperties();
    ProjectDefinition definition = new ProjectDefinition(baseDir, runner.getWorkDir(), properties);
    for (String dir : getList(properties, "sources")) {
      definition.addSourceDir(dir);
    }
    for (String dir : getList(properties, "tests")) {
      definition.addTestDir(dir);
    }
    for (String dir : getList(properties, "binaries")) {
      definition.addBinaryDir(dir);
    }
    for (String pattern : getList(properties, "libraries")) {
      for (File file : getLibraries(baseDir, pattern)) {
        definition.addLibrary(file.getAbsolutePath());
      }
    }
    return definition;
  }

  /**
   * Returns files matching specified pattern.
   * Visibility has been relaxed to make code testable.
   */
  static File[] getLibraries(File baseDir, String pattern) {
    final int i = Math.max(pattern.lastIndexOf('/'), pattern.lastIndexOf('\\'));
    final String dirPath, filePattern;
    if (i == -1) {
      dirPath = ".";
      filePattern = pattern;
    } else {
      dirPath = pattern.substring(0, i);
      filePattern = pattern.substring(i + 1);
    }
    FileFilter fileFilter = new AndFileFilter(FileFileFilter.FILE, new WildcardFileFilter(filePattern));
    File dir = resolvePath(baseDir, dirPath);
    File[] files = dir.listFiles(fileFilter);
    if (files == null || files.length == 0) {
      throw new RunnerException("No files matching pattern \"" + filePattern + "\" in directory \"" + dir + "\"");
    }
    return files;
  }

  private static File resolvePath(File baseDir, String path) {
    File file = new File(path);
    if (!file.isAbsolute()) {
      try {
        file = new File(baseDir, path).getCanonicalFile();
      } catch (IOException e) {
        throw new RunnerException("Unable to resolve path \"" + path + "\"", e);
      }
    }
    return file;
  }

  private String[] getList(Properties properties, String key) {
    return StringUtils.split(properties.getProperty(key, ""), ',');
  }

  private Configuration getInitialConfiguration(ProjectDefinition project) {
    CompositeConfiguration configuration = new CompositeConfiguration();
    configuration.addConfiguration(new SystemConfiguration());
    configuration.addConfiguration(new EnvironmentConfiguration());
    configuration.addConfiguration(new MapConfiguration(project.getProperties()));
    return configuration;
  }

}
