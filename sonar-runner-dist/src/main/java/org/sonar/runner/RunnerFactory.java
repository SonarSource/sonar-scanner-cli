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
import org.sonar.runner.api.ForkedRunner;
import org.sonar.runner.api.Runner;

import java.util.Properties;

class RunnerFactory {

  Runner<?> create(Properties props) {
    Runner<?> runner;
    if ("fork".equals(props.getProperty("sonarRunner.mode"))) {
      runner = ForkedRunner.create();
      String jvmArgs = props.getProperty("sonarRunner.fork.jvmArgs", "");
      if (!"".equals(jvmArgs)) {
        ((ForkedRunner) runner).addJvmArguments(jvmArgs.split(" "));
      }

    } else {
      runner = EmbeddedRunner.create();
    }
    runner.addProperties(props);
    return runner;
  }
}
