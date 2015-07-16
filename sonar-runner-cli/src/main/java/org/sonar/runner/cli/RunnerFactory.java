/*
 * SonarQube Runner - CLI - Distribution
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
package org.sonar.runner.cli;

import java.util.Properties;
import org.sonar.runner.api.EmbeddedRunner;
import org.sonar.runner.api.LogOutput;

class RunnerFactory {

  EmbeddedRunner create(Properties props) {
    return EmbeddedRunner.create(new LogOutput() {

      @Override
      public void log(String formattedMessage, Level level) {
        switch (level) {
          case TRACE:
          case DEBUG:
            Logs.debug(formattedMessage);
            break;
          case ERROR:
            Logs.error(formattedMessage);
            break;
          case INFO:
          case WARN:
          default:
            Logs.info(formattedMessage);
        }
      }
    }).addGlobalProperties(props);
  }

}
