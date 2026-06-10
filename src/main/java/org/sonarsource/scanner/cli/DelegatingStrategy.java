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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

abstract class DelegatingStrategy {

  private static final String SONAR_PREFIX = "sonar.";

  private final CommandRunner commandRunner;

  DelegatingStrategy(CommandRunner commandRunner) {
    this.commandRunner = commandRunner;
  }

  int execute(Path baseDir, Properties props) {
    String executable = resolveExecutable(baseDir);
    List<String> command = buildCommand(executable, sonarArgs(props));
    return commandRunner.run(command, baseDir);
  }

  protected abstract String resolveExecutable(Path baseDir);

  protected abstract List<String> buildCommand(String executable, List<String> sonarArgs);

  protected final String resolveWrapper(Path baseDir, String wrapperUnix, String wrapperWindows, String fallback) {
    String wrapper = isWindows() ? wrapperWindows : wrapperUnix;
    Path wrapperPath = baseDir.resolve(wrapper);
    if (Files.isExecutable(wrapperPath) || Files.exists(wrapperPath)) {
      return wrapperPath.toString();
    }
    return fallback;
  }

  private static List<String> sonarArgs(Properties props) {
    List<String> args = new ArrayList<>();
    for (Map.Entry<Object, Object> entry : props.entrySet()) {
      String key = entry.getKey().toString();
      if (key.startsWith(SONAR_PREFIX)) {
        args.add("-D" + key + "=" + entry.getValue());
      }
    }
    return args;
  }

  private static boolean isWindows() {
    return System.getProperty("os.name", "").toLowerCase().contains("win");
  }
}
