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
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectClassifierTest {

  private final ProjectClassifier classifier = new ProjectClassifier();

  @TempDir
  Path tempDir;

  @Test
  void returns_generic_when_no_markers_present() {
    assertThat(classifier.classify(tempDir, null)).isEqualTo(ProjectType.GENERIC);
  }

  @Test
  void detects_maven_from_pom_xml() throws IOException {
    Files.createFile(tempDir.resolve("pom.xml"));
    assertThat(classifier.classify(tempDir, null)).isEqualTo(ProjectType.MAVEN);
  }

  @Test
  void detects_gradle_from_build_gradle() throws IOException {
    Files.createFile(tempDir.resolve("build.gradle"));
    assertThat(classifier.classify(tempDir, null)).isEqualTo(ProjectType.GRADLE);
  }

  @Test
  void throws_when_both_maven_and_gradle_present() throws IOException {
    Files.createFile(tempDir.resolve("pom.xml"));
    Files.createFile(tempDir.resolve("build.gradle"));
    assertThatThrownBy(() -> classifier.classify(tempDir, null))
      .isInstanceOf(AmbiguousProjectException.class)
      .hasMessageContaining("sonar.scanner.projectType");
  }

  @Test
  void override_forces_project_type() throws IOException {
    Files.createFile(tempDir.resolve("pom.xml"));
    assertThat(classifier.classify(tempDir, "generic")).isEqualTo(ProjectType.GENERIC);
  }

  @Test
  void unknown_override_throws() {
    assertThatThrownBy(() -> classifier.classify(tempDir, "dotnet"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("dotnet");
  }
}
