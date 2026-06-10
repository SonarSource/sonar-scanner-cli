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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class MavenStrategy extends DelegatingStrategy {

  MavenStrategy(CommandRunner commandRunner) {
    super(commandRunner);
  }

  @Override
  protected String resolveExecutable(Path baseDir) {
    return resolveWrapper(baseDir, "./mvnw", "mvnw.cmd", "mvn");
  }

  @Override
  protected List<String> buildCommand(String executable, List<String> sonarArgs) {
    List<String> command = new ArrayList<>();
    command.add(executable);
    command.add("sonar:sonar");
    command.addAll(sonarArgs);
    return command;
  }

}
