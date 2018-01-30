/*
 * SonarQube Scanner
 * Copyright (C) 2011-2018 SonarSource SA
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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.PrintStream;

import static org.mockito.Mockito.*;

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
    verify(stdErr).println("WARN: warn");
    verifyNoMoreInteractions(stdOut, stdErr);
  }

  @Test
  public void testWarnWithTimestamp() {
    logs.setShowTimestamp(true);
    logs.warn("warn");
    verify(stdErr).println(ArgumentMatchers.matches("\\d\\d:\\d\\d:\\d\\d.\\d\\d\\d WARN: warn"));
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
  public void should_show_timestamp_if_enabled() {
    logs.setLogLevel(Logs.LogLevel.DEBUG);
    logs.setShowTimestamp(true);

    logs.debug("debug");
    verify(stdOut).println(ArgumentMatchers.matches("\\d\\d:\\d\\d:\\d\\d.\\d\\d\\d DEBUG: debug$"));
  }

  @Test
  public void should_not_print_info_if_loglevel_is_warn() {
    logs.setLogLevel(Logs.LogLevel.WARN);
    logs.info("some information");
    verifyZeroInteractions(stdErr);
  }

  @Test
  public void should_not_print_warn_if_loglevel_is_error() {
    logs.setLogLevel(Logs.LogLevel.ERROR);
    logs.warn("warn");
    verifyZeroInteractions(stdErr);
  }
}
