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

import java.util.List;
import java.util.stream.Collectors;

class AmbiguousProjectException extends RuntimeException {

  AmbiguousProjectException(List<ProjectType> detected) {
    super(buildMessage(detected));
  }

  private static String buildMessage(List<ProjectType> detected) {
    String types = detected.stream()
      .map(t -> t.name().toLowerCase())
      .collect(Collectors.joining(", "));
    return "Multiple build systems detected (" + types + "). "
      + "Set sonar.scanner.projectType to one of: "
      + detected.stream().map(t -> t.name().toLowerCase()).collect(Collectors.joining(", "))
      + ", generic.";
  }
}
