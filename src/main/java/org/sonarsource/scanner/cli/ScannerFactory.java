/*
 * SonarQube Scanner
 * Copyright (C) 2011-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.scanner.cli;

import java.util.Map;
import java.util.Properties;
import org.sonarsource.scanner.api.EmbeddedScanner;
import org.sonarsource.scanner.api.LogOutput;

class ScannerFactory {

  private final Logs logger;

  public ScannerFactory(Logs logger) {
    this.logger = logger;
  }

  EmbeddedScanner create(Properties props) {
    return EmbeddedScanner.create("ScannerCli", ScannerVersion.version(), new DefaultLogOutput())
      .addGlobalProperties((Map) props);
  }

  class DefaultLogOutput implements LogOutput {
    @Override
    public void log(String formattedMessage, Level level) {
      switch (level) {
        case TRACE:
        case DEBUG:
          logger.debug(formattedMessage);
          break;
        case ERROR:
          logger.error(formattedMessage);
          break;
        case WARN:
          logger.warn(formattedMessage);
          break;
        case INFO:
        default:
          logger.info(formattedMessage);
      }
    }
  }
}
