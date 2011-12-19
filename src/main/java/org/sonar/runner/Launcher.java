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

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.util.Properties;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.apache.commons.configuration.*;
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

public class Launcher {

  private Runner runner;

  public Launcher(Runner runner) {
    this.runner = runner;
  }

  /**
   * This method invoked from {@link Main}. Do not rename it.
   */
  public void execute() {
    initLogging();
    executeBatch();
  }

  private void executeBatch() {
    ProjectDefinition project = defineProject();
    Reactor reactor = new Reactor(project);
    Batch batch = new Batch(getInitialConfiguration(project), new EnvironmentInformation("Runner", runner.getRunnerVersion()), reactor);
    batch.execute();
  }

  private void initLogging() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    JoranConfigurator jc = new JoranConfigurator();
    jc.setContext(context);
    context.reset();
    InputStream input = Batch.class.getResourceAsStream("/org/sonar/batch/logback.xml");
    System.setProperty("ROOT_LOGGER_LEVEL", runner.isDebug() ? "DEBUG" : "INFO");
    try {
      jc.doConfigure(input);

    } catch (JoranException e) {
      throw new SonarException("can not initialize logging", e);

    } finally {
      IOUtils.closeQuietly(input);
    }
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
      for (File file : getLibraries(pattern)) {
        definition.addLibrary(file.getAbsolutePath());
      }
    }
    return definition;
  }

  /**
   * Returns files matching specified pattern.
   * Visibility has been relaxed to make code testable.
   */
  static File[] getLibraries(String pattern) {
    final int i = Math.max(pattern.lastIndexOf('/'), pattern.lastIndexOf('\\'));
    final String dir, filePattern;
    if (i == -1) {
      dir = ".";
      filePattern = pattern;
    } else {
      dir = pattern.substring(0, i);
      filePattern = pattern.substring(i + 1);
    }
    FileFilter fileFilter = new AndFileFilter(FileFileFilter.FILE, new WildcardFileFilter(filePattern));
    return new File(dir).listFiles(fileFilter);
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
