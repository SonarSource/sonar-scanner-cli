/*
 * SonarQube Scanner
 * Copyright (C) 2011 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarsource.scanner.cli;

import org.mockito.Mockito;
import org.sonarsource.scanner.cli.Logs;
import org.sonarsource.scanner.cli.Stats;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import org.junit.Test;
import static org.fest.assertions.Assertions.assertThat;

public class StatsTest {
  private PrintStream stdOut = mock(PrintStream.class);
  private PrintStream stdErr;
  private Logs logs = new Logs(stdOut, stdErr);

  @Test
  public void shouldPrintStats() throws UnsupportedEncodingException {
    new Stats(logs).start().stop();

    verify(stdOut).println(Mockito.contains("Total time: "));
    verify(stdOut).println(Mockito.contains("Final Memory: "));
  }

  @Test
  public void shouldFormatTime() {
    assertThat(Stats.formatTime(1 * 60 * 60 * 1000 + 2 * 60 * 1000 + 3 * 1000 + 400)).isEqualTo("1:02:03.400s");
    assertThat(Stats.formatTime(2 * 60 * 1000 + 3 * 1000 + 400)).isEqualTo("2:03.400s");
    assertThat(Stats.formatTime(3 * 1000 + 400)).isEqualTo("3.400s");
    assertThat(Stats.formatTime(400)).isEqualTo("0.400s");
  }
}
