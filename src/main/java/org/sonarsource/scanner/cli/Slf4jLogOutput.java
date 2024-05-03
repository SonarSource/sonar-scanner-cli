/*
 * SonarScanner CLI
 * Copyright (C) 2011-2024 SonarSource SA
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarsource.scanner.lib.LogOutput;

public class Slf4jLogOutput implements LogOutput {

  private static final Logger LOG = LoggerFactory.getLogger(Slf4jLogOutput.class);

  @Override
  public void log(String s, Level level) {
    switch (level) {
      case TRACE:
        LOG.trace(s);
        break;
      case DEBUG:
        LOG.debug(s);
        break;
      case INFO:
        LOG.info(s);
        break;
      case WARN:
        LOG.warn(s);
        break;
      case ERROR:
        LOG.error(s);
        break;
    }
  }
}
