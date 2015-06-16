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

import org.sonar.home.log.LogListener;
import org.picocontainer.annotations.Nullable;
import com.google.common.annotations.VisibleForTesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.sonar.api.utils.SonarException;
import org.sonar.batch.bootstrapper.Batch;
import org.sonar.batch.bootstrapper.EnvironmentInformation;

/**
 * This class is executed within the classloader provided by the server. It contains the installed plugins and
 * the same version of sonar-batch as the server.
 */
public class BatchIsolatedLauncher implements IsolatedLauncher {

  private static final String WARN = "WARN";
  private static final String DEBUG = "DEBUG";
  private static final String FALSE = "false";

  private Batch batch = null;

  @Override
  public void start(Properties globalProperties, List<Object> extensions) {
    start(globalProperties, extensions, null);
  }

  @Override
  public void start(Properties globalProperties, List<Object> extensions, @Nullable LogListener logListener) {
    batch = createBatch(globalProperties, extensions, logListener);
    batch.start();
  }

  @Override
  public void stop() {
    batch.stop();
  }

  @Override
  public void execute(Properties properties) {
    batch.executeTask((Map) properties);
  }

  Batch createBatch(Properties properties, List<Object> extensions, @Nullable LogListener logListener) {
    EnvironmentInformation env = new EnvironmentInformation(properties.getProperty("sonarRunner.app"), properties.getProperty("sonarRunner.appVersion"));
    Batch.Builder builder = Batch.builder()
      .setEnvironment(env)
      .addComponents(extensions)
      .setBootstrapProperties((Map) properties);

    if (logListener != null) {
      builder.setLogListener(logListener);
    }

    return builder.build();
  }

  /**
   * This method exists for backward compatibility with SonarQube < 5.2. 
   */
  @Override
  public void executeOldVersion(Properties properties, List<Object> extensions) {
    createBatch(properties, extensions, null).execute();
  }

  @Override
  public String getVersion() {
    InputStream is = this.getClass().getClassLoader().getResourceAsStream("sq-version.txt");
    if (is == null) {
      return null;
    }
    try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
      return br.readLine();
    } catch (IOException e) {
      return null;
    }
  }
}
