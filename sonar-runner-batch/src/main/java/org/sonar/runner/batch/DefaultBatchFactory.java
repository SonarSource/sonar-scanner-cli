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

import org.picocontainer.annotations.Nullable;
import org.sonar.batch.bootstrapper.Batch;
import org.sonar.batch.bootstrapper.EnvironmentInformation;

import java.util.List;
import java.util.Map;
import java.util.Properties;

class DefaultBatchFactory implements BatchFactory {
  @Override
  public Batch createBatch(Properties properties, @Nullable final org.sonar.runner.batch.LogOutput logOutput, @Nullable List<Object> extensions) {
    EnvironmentInformation env = new EnvironmentInformation(properties.getProperty("sonarRunner.app"), properties.getProperty("sonarRunner.appVersion"));
    Batch.Builder builder = Batch.builder()
      .setEnvironment(env)
      .setBootstrapProperties((Map) properties);

    if (extensions != null) {
      builder.addComponents(extensions);
    }

    if (logOutput != null) {
      // Do that is a separate class to avoid NoClassDefFoundError for org/sonar/batch/bootstrapper/LogOutput
      Compatibility.setLogOutputFor5dot2(builder, logOutput);
    }

    return builder.build();
  }
}
