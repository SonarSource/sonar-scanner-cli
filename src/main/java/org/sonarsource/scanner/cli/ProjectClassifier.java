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
import java.util.List;
import javax.annotation.Nullable;

class ProjectClassifier {

  private static final List<ProjectTypeDetector> DETECTORS = List.of(
    new MavenDetector(),
    new GradleDetector()
  );

  ProjectType classify(Path baseDir, @Nullable String override) {
    if (override != null && !override.isBlank()) {
      return parseOverride(override);
    }

    List<ProjectType> matched = DETECTORS.stream()
      .filter(d -> d.matches(baseDir))
      .map(ProjectTypeDetector::type)
      .collect(java.util.stream.Collectors.toList());

    if (matched.isEmpty()) {
      return ProjectType.GENERIC;
    }
    if (matched.size() == 1) {
      return matched.get(0);
    }
    throw new AmbiguousProjectException(matched);
  }

  private static ProjectType parseOverride(String override) {
    try {
      return ProjectType.valueOf(override.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
        "Unknown sonar.scanner.projectType value: '" + override + "'. "
          + "Valid values are: maven, gradle, generic.");
    }
  }
}
