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
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class GradleStrategyTest {

  @TempDir
  Path tempDir;

  @Test
  void builds_gradle_sonar_command_with_sonar_properties() {
    RecordingCommandRunner runner = new RecordingCommandRunner(0);
    GradleStrategy strategy = new GradleStrategy(runner) {
      @Override
      protected String resolveExecutable(Path baseDir) {
        return "gradle";
      }
    };

    Properties props = new Properties();
    props.setProperty("sonar.projectKey", "my-project");
    strategy.execute(tempDir, props);

    List<String> command = runner.lastCommand;
    assertThat(command).containsSequence("gradle", "sonar");
    assertThat(command).contains("-Dsonar.projectKey=my-project");
  }

  static class RecordingCommandRunner implements CommandRunner {
    List<String> lastCommand;
    private final int exitCode;

    RecordingCommandRunner(int exitCode) {
      this.exitCode = exitCode;
    }

    @Override
    public int run(List<String> command, Path workingDir) {
      this.lastCommand = command;
      return exitCode;
    }
  }
}
