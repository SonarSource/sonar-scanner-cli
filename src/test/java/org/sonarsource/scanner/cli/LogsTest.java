/*
 * SonarQube Scanner
 * Copyright (C) 2011-2020 SonarSource SA
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

import java.io.PrintStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class LogsTest {
  @Mock
  private PrintStream stdOut;

  @Mock
  private PrintStream stdErr;

  private Logs logs;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    logs = new Logs(stdOut, stdErr);
  }

  @Test
  public void testInfo() {
    logs.info("info");
    verify(stdOut).println("INFO: info");
    verifyNoMoreInteractions(stdOut, stdErr);
  }

  @Test
  public void testWarn() {
    logs.warn("warn");
    verify(stdOut).println("WARN: warn");
    verifyNoMoreInteractions(stdOut, stdErr);
  }

  @Test
  public void testWarnWithTimestamp() {
    logs.setDebugEnabled(true);
    logs.warn("warn");
    verify(stdOut).println(ArgumentMatchers.matches("\\d\\d:\\d\\d:\\d\\d.\\d\\d\\d WARN: warn"));
    verifyNoMoreInteractions(stdOut, stdErr);
  }

  @Test
  public void testError() {
    Exception e = new NullPointerException("exception");
    logs.error("error1");
    verify(stdErr).println("ERROR: error1");

    logs.error("error2", e);
    verify(stdErr).println("ERROR: error2");
    verify(stdErr).println(e);
    // other interactions to print the exception..
  }

  @Test
  public void testDebug() {
    logs.setDebugEnabled(true);

    logs.debug("debug");
    verify(stdOut).println(ArgumentMatchers.matches("\\d\\d:\\d\\d:\\d\\d.\\d\\d\\d DEBUG: debug$"));

    logs.setDebugEnabled(false);
    logs.debug("debug");
    verifyNoMoreInteractions(stdOut, stdErr);
  }
}
