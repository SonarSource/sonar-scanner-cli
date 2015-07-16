/*
 * SonarQube Runner - Batch
 * Copyright (C) 2011 SonarSource
 * sonarqube@googlegroups.com
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import org.picocontainer.annotations.Nullable;
import org.sonar.batch.bootstrapper.Batch;
import org.sonar.batch.bootstrapper.EnvironmentInformation;

/**
 * This class is executed within the classloader provided by the server. It contains the installed plugins and
 * the same version of sonar-batch as the server.
 */
public class BatchIsolatedLauncher implements IsolatedLauncher {

  private Batch batch = null;

  @Override
  public void start(Properties globalProperties, org.sonar.runner.batch.LogOutput logOutput) {
    batch = createBatch(globalProperties, logOutput);
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

  Batch createBatch(Properties properties, @Nullable final org.sonar.runner.batch.LogOutput logOutput) {
    EnvironmentInformation env = new EnvironmentInformation(properties.getProperty("sonarRunner.app"), properties.getProperty("sonarRunner.appVersion"));
    Batch.Builder builder = Batch.builder()
      .setEnvironment(env)
      .setBootstrapProperties((Map) properties);

    if (logOutput != null) {
      // Do that is a separate class to avoid NoClassDefFoundError for org/sonar/batch/bootstrapper/LogOutput
      Compatibility.setLogOutputFor5dot2(builder, logOutput);
    }

    return builder.build();
  }

  /**
   * This method exists for backward compatibility with SonarQube < 5.2. 
   */
  @Override
  public void executeOldVersion(Properties properties) {
    createBatch(properties, null).execute();
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
