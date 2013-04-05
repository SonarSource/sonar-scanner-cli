/*
 * Sonar Runner - Distribution
 * Copyright (C) 2011 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.runner;

import org.junit.Test;
import org.sonar.runner.api.Runner;

import java.util.Properties;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class MainTest {

  Exit exit = mock(Exit.class);
  Cli cli = mock(Cli.class);
  Conf conf = mock(Conf.class);
  RunnerFactory runnerFactory = mock(RunnerFactory.class, RETURNS_MOCKS);

  @Test
  public void should_execute_runner() {
    Main main = new Main(exit, cli, conf, runnerFactory);
    main.execute();

    verify(exit).exit(0);
  }

  @Test
  public void should_fail_on_error() {
    Runner runner = mock(Runner.class);
    doThrow(new IllegalStateException("Error")).when(runner).execute();
    when(runnerFactory.create(any(Properties.class))).thenReturn(runner);

    Main main = new Main(exit, cli, conf, runnerFactory);
    main.execute();

    verify(exit).exit(1);
  }

  @Test
  public void should_only_display_version() {
    when(cli.isDisplayVersionOnly()).thenReturn(true);
    Main main = new Main(exit, cli, conf, runnerFactory);
    main.execute();
    verifyZeroInteractions(runnerFactory);
  }
}
