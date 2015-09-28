/*
 * SonarQube Runner - CLI - Distribution
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
package org.sonar.runner.cli;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sonar.runner.api.EmbeddedRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MainTest {

  @Mock
  private Shutdown shutdown;
  @Mock
  private Cli cli;
  @Mock
  private Conf conf;
  @Mock
  private Properties properties;
  @Mock
  private RunnerFactory runnerFactory;
  @Mock
  private EmbeddedRunner runner;
  @Mock
  private Logs logs;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    when(runnerFactory.create(any(Properties.class))).thenReturn(runner);
    when(conf.properties()).thenReturn(properties);

  }

  @Test
  public void should_execute_runner() {
    Main main = new Main(shutdown, cli, conf, runnerFactory, logs);
    main.execute();

    verify(shutdown).exit(Exit.SUCCESS);
    verify(runnerFactory).create(properties);

    verify(runner, times(1)).start();
    verify(runner, times(1)).runAnalysis(properties);
    verify(runner, times(1)).stop();
  }

  @Test
  public void should_stop_on_error() {
    EmbeddedRunner runner = mock(EmbeddedRunner.class);
    Exception e = new NullPointerException("NPE");
    e = new IllegalStateException("Error", e);
    doThrow(e).when(runner).runAnalysis(any(Properties.class));
    when(runnerFactory.create(any(Properties.class))).thenReturn(runner);

    Main main = new Main(shutdown, cli, conf, runnerFactory, logs);
    main.execute();

    verify(runner).stop();
    verify(shutdown).exit(Exit.ERROR);
    verify(logs).error("Caused by: NPE");

  }

  @Test
  public void show_error_stacktrace() {
    Exception e = new NullPointerException("NPE");
    e = new IllegalStateException("Error", e);
    when(cli.isDisplayStackTrace()).thenReturn(true);

    EmbeddedRunner runner = mock(EmbeddedRunner.class);
    doThrow(e).when(runner).runAnalysis(any(Properties.class));
    when(runnerFactory.create(any(Properties.class))).thenReturn(runner);

    Main main = new Main(shutdown, cli, conf, runnerFactory, logs);
    main.execute();

    verify(runner).stop();
    verify(shutdown).exit(Exit.ERROR);
    verify(logs).error("Error during Sonar runner execution", e);
  }

  @Test
  public void should_not_stop_on_error_in_interactive_mode() throws Exception {
    EmbeddedRunner runner = mock(EmbeddedRunner.class);
    doThrow(new IllegalStateException("Error")).when(runner).runAnalysis(any(Properties.class));
    when(runnerFactory.create(any(Properties.class))).thenReturn(runner);
    when(cli.isInteractive()).thenReturn(true);

    Main main = new Main(shutdown, cli, conf, runnerFactory, logs);
    BufferedReader inputReader = mock(BufferedReader.class);
    when(inputReader.readLine()).thenReturn("");
    when(shutdown.shouldExit()).thenReturn(false).thenReturn(true);
    main.setInputReader(inputReader);
    main.execute();

    verify(runner, times(2)).runAnalysis(any(Properties.class));
    verify(runner).stop();
    verify(shutdown).exit(Exit.SUCCESS);
  }

  @Test
  public void should_only_display_version() throws IOException {

    Properties p = new Properties();
    when(cli.isDisplayVersionOnly()).thenReturn(true);
    when(conf.properties()).thenReturn(p);

    Main main = new Main(shutdown, cli, conf, runnerFactory, logs);
    main.execute();

    InOrder inOrder = Mockito.inOrder(shutdown, runnerFactory);

    inOrder.verify(shutdown, times(1)).exit(Exit.SUCCESS);
    inOrder.verify(runnerFactory, times(1)).create(p);
    inOrder.verify(shutdown, times(1)).exit(Exit.SUCCESS);
  }

  @Test(timeout = 30000)
  public void test_interactive_mode() throws IOException {
    String inputStr = "qwe" + System.lineSeparator() + "qwe" + System.lineSeparator();
    InputStream input = new ByteArrayInputStream(inputStr.getBytes(StandardCharsets.UTF_8));
    System.setIn(input);
    input.close();

    when(cli.isInteractive()).thenReturn(true);
    when(cli.isDebugMode()).thenReturn(true);
    when(cli.isDisplayStackTrace()).thenReturn(true);

    Main main = new Main(shutdown, cli, conf, runnerFactory, logs);
    main.execute();

    verify(runner, times(1)).start();
    verify(runner, times(3)).runAnalysis(any(Properties.class));
    verify(runner, times(1)).stop();
  }
}
