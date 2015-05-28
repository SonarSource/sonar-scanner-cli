/*
 * SonarQube Runner - Batch
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
package org.sonar.runner.batch;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.bootstrapper.Batch;
import org.sonar.batch.bootstrapper.EnvironmentInformation;

/**
 * This class is executed within the classloader provided by the server. It contains the installed plugins and
 * the same version of sonar-batch as the server.
 */
public class IsolatedLauncher {

  private static final String WARN = "WARN";
  private static final String DEBUG = "DEBUG";
  private static final String FALSE = "false";

  public void execute(Properties properties, List<Object> extensions) {
    createBatch(properties, extensions).execute();
  }

  Batch createBatch(Properties properties, List<Object> extensions) {
    initLogging(properties);
    EnvironmentInformation env = new EnvironmentInformation(properties.getProperty("sonarRunner.app"), properties.getProperty("sonarRunner.appVersion"));
    return Batch.builder()
      .setEnvironment(env)
      .addComponents(extensions)
      .setBootstrapProperties((Map) properties)
      .build();
  }

  private void initLogging(Properties props) {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    JoranConfigurator jc = new JoranConfigurator();
    jc.setContext(context);
    context.reset();
    try (InputStream input = Batch.class.getResourceAsStream("/org/sonar/batch/logback.xml")) {
      System.setProperty("ROOT_LOGGER_LEVEL", isDebug(props) ? DEBUG : "INFO");
      context.putProperty("SQL_LOGGER_LEVEL", getSqlLevel(props));
      context.putProperty("SQL_RESULTS_LOGGER_LEVEL", getSqlResultsLevel(props));
      jc.doConfigure(input);
    } catch (JoranException e) {
      throw new SonarException("can not initialize logging", e);
    } catch (IOException e1) {
      throw new SonarException("couldn't close resource", e1);
    }
  }

  @VisibleForTesting
  protected boolean isDebug(Properties props) {
    return Boolean.parseBoolean(props.getProperty("sonar.verbose", FALSE));
  }

  @VisibleForTesting
  protected static String getSqlLevel(Properties props) {
    boolean showSql = "true".equals(props.getProperty("sonar.showSql", FALSE));
    return showSql ? DEBUG : WARN;
  }

  @VisibleForTesting
  protected static String getSqlResultsLevel(Properties props) {
    boolean showSql = "true".equals(props.getProperty("sonar.showSqlResults", FALSE));
    return showSql ? DEBUG : WARN;
  }
}
