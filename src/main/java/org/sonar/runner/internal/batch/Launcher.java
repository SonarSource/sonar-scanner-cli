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
package org.sonar.runner.internal.batch;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.configuration.*;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.Batch;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.runner.Runner;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * Contrary to {@link org.sonar.runner.Runner}, this class is executed within the classloader
 * provided by the server. It contains the installed plugins and the same version of sonar-batch as the server.
 */
public class Launcher {

  private Properties propertiesFromRunner;
  private List<Object> containerExtensions;

  public Launcher(Properties properties, List<Object> containerExtensions) {
    this.propertiesFromRunner = properties;
    this.containerExtensions = containerExtensions;
  }

  /**
   * Main entry point.
   */
  public void execute() {
    ProjectDefinition project = SonarProjectBuilder.create(propertiesFromRunner).generateProjectDefinition();
    Configuration initialConfiguration = getInitialConfiguration(project);
    initLogging(initialConfiguration);
    executeBatch(project, initialConfiguration);
  }

  private void executeBatch(ProjectDefinition project, Configuration initialConfiguration) {
    setContainerExtensionsOnProject(project);
    String envKey = propertiesFromRunner.getProperty(Runner.PROPERTY_ENVIRONMENT_INFORMATION_KEY);
    String envVersion = propertiesFromRunner.getProperty(Runner.PROPERTY_ENVIRONMENT_INFORMATION_VERSION);
    Batch batch = Batch.create(new ProjectReactor(project), initialConfiguration, new EnvironmentInformation(envKey, envVersion));
    batch.execute();
  }

  private void setContainerExtensionsOnProject(ProjectDefinition projectDefinition) {
    for (Object extension : containerExtensions) {
      projectDefinition.addContainerExtension(extension);
    }
    for (ProjectDefinition module : projectDefinition.getSubProjects()) {
      setContainerExtensionsOnProject(module);
    }
  }

  private void initLogging(Configuration initialConfiguration) {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    JoranConfigurator jc = new JoranConfigurator();
    jc.setContext(context);
    context.reset();
    InputStream input = Batch.class.getResourceAsStream("/org/sonar/batch/logback.xml");
    System.setProperty("ROOT_LOGGER_LEVEL", isDebug() ? "DEBUG" : "INFO");
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

  @VisibleForTesting
  protected boolean isDebug() {
    return Boolean.parseBoolean(propertiesFromRunner.getProperty(Runner.PROPERTY_VERBOSE, propertiesFromRunner.getProperty(Runner.PROPERTY_OLD_DEBUG_MODE, "false")));
  }

  @VisibleForTesting
  protected static String getSqlLevel(Configuration config) {
    boolean showSql = config.getBoolean("sonar.showSql", false);
    return showSql ? "DEBUG" : "WARN";
  }

  @VisibleForTesting
  protected static String getSqlResultsLevel(Configuration config) {
    boolean showSql = config.getBoolean("sonar.showSqlResults", false);
    return showSql ? "DEBUG" : "WARN";
  }

  private Configuration getInitialConfiguration(ProjectDefinition project) {
    CompositeConfiguration configuration = new CompositeConfiguration();
    configuration.addConfiguration(new SystemConfiguration());
    configuration.addConfiguration(new EnvironmentConfiguration());
    configuration.addConfiguration(new MapConfiguration(project.getProperties()));
    return configuration;
  }

}
