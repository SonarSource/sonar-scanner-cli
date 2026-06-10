/*
 * SonarScanner CLI
 * Copyright (C) SonarSource Sàrl
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ProcessCommandRunner implements CommandRunner {

  private static final Logger LOG = LoggerFactory.getLogger(ProcessCommandRunner.class);

  @Override
  public int run(List<String> command, Path workingDir) {
    LOG.info("Delegating to: {}", String.join(" ", command));
    try {
      Process process = new ProcessBuilder(command)
        .directory(workingDir.toFile())
        .inheritIO()
        .start();
      return process.waitFor();
    } catch (IOException | InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Failed to run delegated command: " + String.join(" ", command), e);
    }
  }
}
