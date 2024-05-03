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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonarsource.scanner.lib.LogOutput;
import testutils.LogTester;

import static org.assertj.core.api.Assertions.assertThat;

class Slf4jLogOutputTest {

  @RegisterExtension
  LogTester logTester = new LogTester().setLevel(Level.TRACE);

  @Test
  void make_coverage_happy() {
    var underTest = new Slf4jLogOutput();
    underTest.log("trace", LogOutput.Level.TRACE);
    underTest.log("debug", LogOutput.Level.DEBUG);
    underTest.log("info", LogOutput.Level.INFO);
    underTest.log("warn", LogOutput.Level.WARN);
    underTest.log("error", LogOutput.Level.ERROR);

    assertThat(logTester.logs(Level.TRACE)).containsOnly("trace");
    assertThat(logTester.logs(Level.DEBUG)).containsOnly("debug");
    assertThat(logTester.logs(Level.INFO)).containsOnly("info");
    assertThat(logTester.logs(Level.WARN)).containsOnly("warn");
    assertThat(logTester.logs(Level.ERROR)).containsOnly("error");
  }

}
