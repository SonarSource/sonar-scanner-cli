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
import org.apache.commons.configuration.*;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.platform.Environment;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.Batch;
import org.sonar.batch.bootstrapper.ProjectDefinition;
import org.sonar.batch.bootstrapper.Reactor;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

public class Launcher {

  private Main task;

  public Launcher(Main launcher) {
    this.task = launcher;
  }

  /**
   * This method invoked from {@link Main}.
   */
  public void execute() {
    initLogging();
    executeBatch();
  }

  private void executeBatch() {
    ProjectDefinition project = defineProject();
    Reactor reactor = new Reactor(project);
    Batch batch = new Batch(getInitialConfiguration(project), Environment.ANT, reactor); // TODO environment
    batch.execute();
  }

  private void initLogging() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    JoranConfigurator jc = new JoranConfigurator();
    jc.setContext(context);
    context.reset();
    InputStream input = Batch.class.getResourceAsStream("/org/sonar/batch/logback.xml");
    // System.setProperty("ROOT_LOGGER_LEVEL", getLog().isDebugEnabled() ? "DEBUG" : "INFO");
    System.setProperty("ROOT_LOGGER_LEVEL", "INFO");
    try {
      jc.doConfigure(input);

    } catch (JoranException e) {
      throw new SonarException("can not initialize logging", e);

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  private ProjectDefinition defineProject() {
    File baseDir = task.getProjectDir();
    Properties properties = task.getProperties();
    ProjectDefinition definition = new ProjectDefinition(baseDir, task.getWorkDir(), properties);
    // TODO for some reason it can't be relative
    definition.addSourceDir(new File(baseDir, "src").getAbsolutePath()); // TODO hard-coded value
    // TODO definition.addTestDir(path);
    // TODO definition.addBinaryDir(path);
    // TODO definition.addLibrary(path);
    return definition;
  }

  private Configuration getInitialConfiguration(ProjectDefinition project) {
    CompositeConfiguration configuration = new CompositeConfiguration();
    configuration.addConfiguration(new SystemConfiguration());
    configuration.addConfiguration(new EnvironmentConfiguration());
    configuration.addConfiguration(new MapConfiguration(project.getProperties()));
    return configuration;
  }

}
