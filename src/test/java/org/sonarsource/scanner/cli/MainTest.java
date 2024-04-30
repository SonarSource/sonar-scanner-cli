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

import java.util.Map;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sonar.api.utils.MessageException;
import org.sonarsource.scanner.lib.ScanProperties;
import org.sonarsource.scanner.lib.ScannerEngineBootstrapper;
import org.sonarsource.scanner.lib.ScannerEngineFacade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
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
  private ScannerEngineBootstrapperFactory scannerEngineBootstrapperFactory;
  @Mock
  private ScannerEngineBootstrapper bootstrapper;
  @Mock
  private ScannerEngineFacade engine;
  @Mock
  private Logs logs;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(scannerEngineBootstrapperFactory.create(any(Properties.class), any(String.class))).thenReturn(bootstrapper);
    when(bootstrapper.bootstrap()).thenReturn(engine);
    when(conf.properties()).thenReturn(properties);
  }

  @Test
  public void should_execute_scanner_engine() {
    when(cli.getInvokedFrom()).thenReturn("");
    Main main = new Main(exit, cli, conf, scannerEngineBootstrapperFactory, logs);
    main.analyze();

    verify(exit).exit(Exit.SUCCESS);
    verify(scannerEngineBootstrapperFactory).create(properties, "");

    verify(bootstrapper, times(1)).bootstrap();
    verify(engine, times(1)).analyze((Map) properties);
  }

  @Test
  public void should_exit_with_error_on_error_during_analysis() {
    Exception e = new NullPointerException("NPE");
    e = new IllegalStateException("Error", e);
    doThrow(e).when(engine).analyze(any());
    when(cli.getInvokedFrom()).thenReturn("");
    when(cli.isDebugEnabled()).thenReturn(true);
    Main main = new Main(exit, cli, conf, scannerEngineBootstrapperFactory, logs);
    main.analyze();

    verify(exit).exit(Exit.INTERNAL_ERROR);
    verify(logs).error("Error during SonarScanner execution", e);
  }

  @Test
  public void should_exit_with_error_on_error_during_bootstrap() {
    Exception e = new NullPointerException("NPE");
    e = new IllegalStateException("Error", e);
    doThrow(e).when(bootstrapper).bootstrap();
    when(cli.getInvokedFrom()).thenReturn("");
    when(cli.isDebugEnabled()).thenReturn(true);

    Main main = new Main(exit, cli, conf, scannerEngineBootstrapperFactory, logs);
    main.analyze();

    verify(bootstrapper).bootstrap();
    verify(engine, never()).analyze(any());
    verify(exit).exit(Exit.INTERNAL_ERROR);
    verify(logs).error("Error during SonarScanner execution", e);
  }

  @Test
  public void show_stacktrace() {
    Exception e = createException(false);
    testException(e, false, false, Exit.INTERNAL_ERROR);

    verify(logs).error("Error during SonarScanner execution", e);
    verify(logs).error("Re-run SonarScanner using the -X switch to enable full debug logging.");
  }

  @Test
  public void dont_show_MessageException_stacktrace() {
    Exception e = createException(true);
    testException(e, false, false, Exit.USER_ERROR);

    verify(logs, times(5)).error(anyString());
    verify(logs).error("Error during SonarScanner execution");
    verify(logs).error("my message");
    verify(logs).error("Caused by: A functional cause");
    verify(logs).error("");
    verify(logs).error("Re-run SonarScanner using the -X switch to enable full debug logging.");
  }

  @Test
  public void dont_show_MessageException_stacktrace_embedded() {
    Exception e = createException(true);
    testException(e, false, true, Exit.USER_ERROR);

    verify(logs, times(4)).error(anyString());
    verify(logs).error("Error during SonarScanner execution");
    verify(logs).error("my message");
    verify(logs).error("Caused by: A functional cause");
    verify(logs).error("");
  }

  @Test
  public void show_MessageException_stacktrace_in_debug() {
    Exception e = createException(true);
    testException(e, true, false, Exit.USER_ERROR);

    verify(logs, times(1)).error(anyString(), any(Throwable.class));
    verify(logs).error("Error during SonarScanner execution", e);
  }

  @Test
  public void show_MessageException_stacktrace_in_debug_embedded() {
    Exception e = createException(true);
    testException(e, true, true, Exit.USER_ERROR);

    verify(logs, times(1)).error(anyString(), any(Throwable.class));
    verify(logs).error("Error during SonarScanner execution", e);
  }

  @Test
  public void show_stacktrace_in_debug() {
    Exception e = createException(false);
    testException(e, true, false, Exit.INTERNAL_ERROR);

    verify(logs).error("Error during SonarScanner execution", e);
    verify(logs, never()).error("Re-run SonarScanner using the -X switch to enable full debug logging.");
  }

  private void testException(Exception e, boolean debugEnabled, boolean isEmbedded, int expectedExitCode) {
    when(cli.isDebugEnabled()).thenReturn(debugEnabled);
    when(cli.isEmbedded()).thenReturn(isEmbedded);
    when(cli.getInvokedFrom()).thenReturn("");


    doThrow(e).when(engine).analyze(any());

    when(scannerEngineBootstrapperFactory.create(any(Properties.class), any(String.class))).thenReturn(bootstrapper);

    Main main = new Main(exit, cli, conf, scannerEngineBootstrapperFactory, logs);
    main.analyze();

    verify(exit).exit(expectedExitCode);
  }

  private Exception createException(boolean messageException) {
    Exception e;
    if (messageException) {
      e = new MessageException("my message", new IllegalStateException("A functional cause"));
    } else {
      e = new IllegalStateException("Error", new NullPointerException("NPE"));
    }

    return e;
  }

  @Test
  public void should_only_display_version() {
    Properties p = new Properties();
    when(cli.isDisplayVersionOnly()).thenReturn(true);
    when(cli.getInvokedFrom()).thenReturn("");
    when(conf.properties()).thenReturn(p);

    Main main = new Main(exit, cli, conf, scannerEngineBootstrapperFactory, logs);
    main.analyze();

    InOrder inOrder = Mockito.inOrder(exit, scannerEngineBootstrapperFactory);

    inOrder.verify(exit, times(1)).exit(Exit.SUCCESS);
    inOrder.verify(scannerEngineBootstrapperFactory, times(1)).create(p, "");
    inOrder.verify(exit, times(1)).exit(Exit.SUCCESS);
  }

  @Test
  public void should_skip() {
    Properties p = new Properties();
    p.setProperty(ScanProperties.SKIP, "true");
    when(conf.properties()).thenReturn(p);
    when(cli.getInvokedFrom()).thenReturn("");

    Main main = new Main(exit, cli, conf, scannerEngineBootstrapperFactory, logs);
    main.analyze();

    verify(logs).info("SonarScanner analysis skipped");
    InOrder inOrder = Mockito.inOrder(exit, scannerEngineBootstrapperFactory);

    inOrder.verify(exit, times(1)).exit(Exit.SUCCESS);
    inOrder.verify(scannerEngineBootstrapperFactory, times(1)).create(p, "");
    inOrder.verify(exit, times(1)).exit(Exit.SUCCESS);
  }

  @Test
  public void shouldLogServerVersion() {
    when(engine.isSonarCloud()).thenReturn(false);
    when(engine.getServerVersion()).thenReturn("5.5");
    Properties p = new Properties();
    when(cli.isDisplayVersionOnly()).thenReturn(true);
    when(cli.getInvokedFrom()).thenReturn("");
    when(conf.properties()).thenReturn(p);

    Main main = new Main(exit, cli, conf, scannerEngineBootstrapperFactory, logs);
    main.analyze();
    verify(logs).info("Analyzing on SonarQube server 5.5");
  }

  @Test
  public void should_log_SonarCloud_server() {
    when(engine.isSonarCloud()).thenReturn(true);
    Properties p = new Properties();
    when(conf.properties()).thenReturn(p);
    when(cli.getInvokedFrom()).thenReturn("");

    Main main = new Main(exit, cli, conf, scannerEngineBootstrapperFactory, logs);
    main.analyze();
    verify(logs).info("Analyzing on SonarCloud");
  }

  @Test
  public void should_configure_logging() {
    Properties analysisProps = testLogging("sonar.verbose", "true");
    assertThat(analysisProps.getProperty("sonar.verbose")).isEqualTo("true");
  }

  @Test
  public void should_configure_logging_trace() {
    Properties analysisProps = testLogging("sonar.log.level", "TRACE");
    assertThat(analysisProps.getProperty("sonar.log.level")).isEqualTo("TRACE");
  }

  @Test
  public void should_set_bootstrap_start_time_in_millis() {
    Properties analysisProps = execute("sonar.scanner.bootstrapStartTime", "1714137496104");
    assertThat(analysisProps.getProperty("sonar.scanner.bootstrapStartTime")).isEqualTo("1714137496104");
  }

  @Test
  public void should_configure_logging_debug() {
    Properties analysisProps = testLogging("sonar.log.level", "DEBUG");
    assertThat(analysisProps.getProperty("sonar.log.level")).isEqualTo("DEBUG");
  }

  private Properties testLogging(String propKey, String propValue) {
    Properties actualProps = execute(propKey, propValue);

    // Logger used for callback should have debug enabled
    verify(logs).setDebugEnabled(true);

    return actualProps;
  }

  private Properties execute(String propKey, String propValue) {
    Properties p = new Properties();
    p.put(propKey, propValue);

    when(conf.properties()).thenReturn(p);
    when(cli.getInvokedFrom()).thenReturn("");

    Main main = new Main(exit, cli, conf, scannerEngineBootstrapperFactory, logs);
    main.analyze();

    ArgumentCaptor<Properties> propertiesCapture = ArgumentCaptor.forClass(Properties.class);
    verify(engine).analyze((Map) propertiesCapture.capture());

    return propertiesCapture.getValue();
  }

}
