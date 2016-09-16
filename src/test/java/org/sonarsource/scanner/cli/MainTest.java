/*
 * SonarQube Scanner
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sonar.api.utils.MessageException;
import org.sonarsource.scanner.api.EmbeddedScanner;
import org.sonarsource.scanner.api.ScanProperties;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MainTest {

  @Mock
  private Exit exit;
  @Mock
  private Cli cli;
  @Mock
  private Conf conf;
  @Mock
  private Properties properties;
  @Mock
  private ScannerFactory runnerFactory;
  @Mock
  private EmbeddedScanner runner;
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
    Main main = new Main(exit, cli, conf, runnerFactory, logs);
    main.execute();

    verify(exit).exit(Exit.SUCCESS);
    verify(runnerFactory).create(properties);

    verify(runner, times(1)).start();
    verify(runner, times(1)).runAnalysis(properties);
    verify(runner, times(1)).stop();
  }

  @Test
  public void should_stop_on_error() {
    EmbeddedScanner runner = mock(EmbeddedScanner.class);
    Exception e = new NullPointerException("NPE");
    e = new IllegalStateException("Error", e);
    doThrow(e).when(runner).runAnalysis(any(Properties.class));
    when(runnerFactory.create(any(Properties.class))).thenReturn(runner);

    Main main = new Main(exit, cli, conf, runnerFactory, logs);
    main.execute();

    verify(runner).stop();
    verify(exit).exit(Exit.ERROR);
    verify(logs).error("Error during SonarQube Scanner execution", e);
  }

  @Test
  public void show_error() {
    Exception e = createException(false);
    testException(e, false);

    verify(logs).error("Error during SonarQube Scanner execution", e);
    verify(logs).error("Re-run SonarQube Scanner using the -X switch to enable full debug logging.");
  }

  @Test
  public void show_error_MessageException() {
    Exception e = createException(true);
    testException(e, false);

    verify(logs).error("Error during SonarQube Scanner execution");
    verify(logs).error("Re-run SonarQube Scanner using the -X switch to enable full debug logging.");
  }

  @Test
  public void show_error_debug() {
    Exception e = createException(false);
    testException(e, true);

    verify(logs).error("Error during SonarQube Scanner execution", e);
    verify(logs, never()).error("Re-run SonarQube Scanner using the -X switch to enable full debug logging.");
  }

  private void testException(Exception e, boolean debugEnabled) {
    when(cli.isDebugEnabled()).thenReturn(debugEnabled);

    EmbeddedScanner runner = mock(EmbeddedScanner.class);
    doThrow(e).when(runner).runAnalysis(any(Properties.class));
    when(runnerFactory.create(any(Properties.class))).thenReturn(runner);

    Main main = new Main(exit, cli, conf, runnerFactory, logs);
    main.execute();

    verify(runner).stop();
    verify(exit).exit(Exit.ERROR);
  }

  private Exception createException(boolean messageException) {
    Exception e;
    if (messageException) {
      e = new MessageException("my message");
    } else {
      e = new IllegalStateException("Error", new NullPointerException("NPE"));
    }

    return e;
  }

  @Test
  public void should_only_display_version() throws IOException {

    Properties p = new Properties();
    when(cli.isDisplayVersionOnly()).thenReturn(true);
    when(conf.properties()).thenReturn(p);

    Main main = new Main(exit, cli, conf, runnerFactory, logs);
    main.execute();

    InOrder inOrder = Mockito.inOrder(exit, runnerFactory);

    inOrder.verify(exit, times(1)).exit(Exit.SUCCESS);
    inOrder.verify(runnerFactory, times(1)).create(p);
    inOrder.verify(exit, times(1)).exit(Exit.SUCCESS);
  }

  @Test
  public void should_skip() throws IOException {
    Properties p = new Properties();
    p.setProperty(ScanProperties.SKIP, "true");
    when(conf.properties()).thenReturn(p);

    Main main = new Main(exit, cli, conf, runnerFactory, logs);
    main.execute();

    verify(logs).info("SonarQube Scanner analysis skipped");
    InOrder inOrder = Mockito.inOrder(exit, runnerFactory);

    inOrder.verify(exit, times(1)).exit(Exit.SUCCESS);
    inOrder.verify(runnerFactory, times(1)).create(p);
    inOrder.verify(exit, times(1)).exit(Exit.SUCCESS);
  }

  @Test
  public void shouldLogServerVersion() throws IOException {
    when(runner.serverVersion()).thenReturn("5.5");
    Properties p = new Properties();
    when(cli.isDisplayVersionOnly()).thenReturn(true);
    when(conf.properties()).thenReturn(p);

    Main main = new Main(exit, cli, conf, runnerFactory, logs);
    main.execute();
    verify(logs).info("SonarQube server 5.5");
  }

  @Test
  public void should_configure_logging() throws IOException {
    Properties analysisProps = testLogging("sonar.verbose", "true");
    assertThat(analysisProps.getProperty("sonar.verbose")).isEqualTo("true");
  }

  @Test
  public void should_configure_logging_trace() throws IOException {
    Properties analysisProps = testLogging("sonar.log.level", "TRACE");
    assertThat(analysisProps.getProperty("sonar.log.level")).isEqualTo("TRACE");
  }

  @Test
  public void should_configure_logging_debug() throws IOException {
    Properties analysisProps = testLogging("sonar.log.level", "DEBUG");
    assertThat(analysisProps.getProperty("sonar.log.level")).isEqualTo("DEBUG");
  }

  private Properties testLogging(String propKey, String propValue) throws IOException {
    Properties p = new Properties();
    p.put(propKey, propValue);
    when(conf.properties()).thenReturn(p);

    Main main = new Main(exit, cli, conf, runnerFactory, logs);
    main.execute();

    // Logger used for callback should have debug enabled
    verify(logs).setDebugEnabled(true);

    ArgumentCaptor<Properties> propertiesCapture = ArgumentCaptor.forClass(Properties.class);
    verify(runner).runAnalysis(propertiesCapture.capture());

    return propertiesCapture.getValue();
  }

}
