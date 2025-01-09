/*
 * SonarScanner CLI
 * Copyright (C) 2011-2025 SonarSource SA
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.sonar.api.utils.MessageException;
import org.sonarsource.scanner.lib.ScannerEngineBootstrapper;
import org.sonarsource.scanner.lib.ScannerEngineFacade;
import org.sonarsource.scanner.lib.ScannerProperties;
import testutils.LogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MainTest {

  @RegisterExtension
  LogTester logTester = new LogTester();

  private final Exit exit = mock();
  private final Cli cli = mock();
  private final Conf conf = mock();
  private final Properties properties = mock();
  private final ScannerEngineBootstrapperFactory scannerEngineBootstrapperFactory = mock();
  private final ScannerEngineBootstrapper bootstrapper = mock();
  private final ScannerEngineFacade engine = mock();

  @BeforeEach
  void setUp() {
    when(scannerEngineBootstrapperFactory.create(any(Properties.class), any(String.class))).thenReturn(bootstrapper);
    when(bootstrapper.bootstrap()).thenReturn(engine);
    when(engine.analyze(any())).thenReturn(true);
    when(conf.properties()).thenReturn(properties);
  }

  @Test
  void should_execute_scanner_engine() {
    when(cli.getInvokedFrom()).thenReturn("");
    Main main = new Main(exit, cli, conf, scannerEngineBootstrapperFactory);
    main.analyze();

    verify(exit).exit(Exit.SUCCESS);
    verify(scannerEngineBootstrapperFactory).create(properties, "");

    verify(bootstrapper, times(1)).bootstrap();
    verify(engine, times(1)).analyze((Map) properties);
  }

  @Test
  void should_exit_with_error_on_error_during_analysis() {
    Exception e = new NullPointerException("NPE");
    e = new IllegalStateException("Error", e);
    doThrow(e).when(engine).analyze(any());
    when(cli.getInvokedFrom()).thenReturn("");
    when(cli.isDebugEnabled()).thenReturn(true);
    Main main = new Main(exit, cli, conf, scannerEngineBootstrapperFactory);
    main.analyze();

    verify(exit).exit(Exit.INTERNAL_ERROR);
    assertThat(logTester.logs(Level.ERROR)).contains("Error during SonarScanner CLI execution");
  }

  @Test
  void should_exit_with_error_on_error_during_bootstrap() {
    Exception e = new NullPointerException("NPE");
    e = new IllegalStateException("Error", e);
    doThrow(e).when(bootstrapper).bootstrap();
    when(cli.getInvokedFrom()).thenReturn("");
    when(cli.isDebugEnabled()).thenReturn(true);

    Main main = new Main(exit, cli, conf, scannerEngineBootstrapperFactory);
    main.analyze();

    verify(bootstrapper).bootstrap();
    verify(engine, never()).analyze(any());
    verify(exit).exit(Exit.INTERNAL_ERROR);
    assertThat(logTester.logs(Level.ERROR)).contains("Error during SonarScanner CLI execution");
  }

  @Test
  void show_stacktrace() {
    Exception e = createException(false);
    testException(e, false, false, Exit.INTERNAL_ERROR);

    assertThat(logTester.logs(Level.ERROR)).contains("Error during SonarScanner CLI execution");
    assertThat(logTester.logs(Level.ERROR)).contains("Re-run SonarScanner CLI using the -X switch to enable full debug logging.");
  }

  @Test
  void dont_show_MessageException_stacktrace() {
    Exception e = createException(true);
    testException(e, false, false, Exit.USER_ERROR);

    assertThat(logTester.logs(Level.ERROR)).containsOnly("Error during SonarScanner CLI execution",
      "my message",
      "Caused by: A functional cause",
      "",
      "Re-run SonarScanner CLI using the -X switch to enable full debug logging.");
  }

  @Test
  void dont_show_MessageException_stacktrace_embedded() {
    Exception e = createException(true);
    testException(e, false, true, Exit.USER_ERROR);

    assertThat(logTester.logs(Level.ERROR)).containsOnly("Error during SonarScanner CLI execution",
      "my message",
      "Caused by: A functional cause",
      "");
  }

  @Test
  void show_MessageException_stacktrace_in_debug() {
    Exception e = createException(true);
    testException(e, true, false, Exit.USER_ERROR);

    assertThat(logTester.logs(Level.ERROR)).containsOnly("Error during SonarScanner CLI execution");
  }

  @Test
  void show_MessageException_stacktrace_in_debug_embedded() {
    Exception e = createException(true);
    testException(e, true, true, Exit.USER_ERROR);

    assertThat(logTester.logs(Level.ERROR)).containsOnly("Error during SonarScanner CLI execution");
  }

  @Test
  void show_stacktrace_in_debug() {
    Exception e = createException(false);
    testException(e, true, false, Exit.INTERNAL_ERROR);

    assertThat(logTester.logs(Level.ERROR)).containsOnly("Error during SonarScanner CLI execution");
  }

  private void testException(Exception e, boolean debugEnabled, boolean isEmbedded, int expectedExitCode) {
    when(cli.isDebugEnabled()).thenReturn(debugEnabled);
    when(cli.isEmbedded()).thenReturn(isEmbedded);
    when(cli.getInvokedFrom()).thenReturn("");


    doThrow(e).when(engine).analyze(any());

    when(scannerEngineBootstrapperFactory.create(any(Properties.class), any(String.class))).thenReturn(bootstrapper);

    Main main = new Main(exit, cli, conf, scannerEngineBootstrapperFactory);
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
  void should_only_display_version() {
    Properties p = new Properties();
    when(cli.isDisplayVersionOnly()).thenReturn(true);
    when(cli.getInvokedFrom()).thenReturn("");
    when(conf.properties()).thenReturn(p);

    Main main = new Main(exit, cli, conf, scannerEngineBootstrapperFactory);
    main.analyze();

    InOrder inOrder = Mockito.inOrder(exit, scannerEngineBootstrapperFactory);

    inOrder.verify(exit, times(1)).exit(Exit.SUCCESS);
    inOrder.verify(scannerEngineBootstrapperFactory, times(1)).create(p, "");
    inOrder.verify(exit, times(1)).exit(Exit.SUCCESS);
  }

  @Test
  void should_skip() {
    Properties p = new Properties();
    p.setProperty(ScannerProperties.SKIP, "true");
    when(conf.properties()).thenReturn(p);
    when(cli.getInvokedFrom()).thenReturn("");

    Main main = new Main(exit, cli, conf, scannerEngineBootstrapperFactory);
    main.analyze();

    assertThat(logTester.logs(Level.INFO)).contains("SonarScanner CLI analysis skipped");
    InOrder inOrder = Mockito.inOrder(exit, scannerEngineBootstrapperFactory);

    inOrder.verify(exit, times(1)).exit(Exit.SUCCESS);
    inOrder.verify(scannerEngineBootstrapperFactory, times(1)).create(p, "");
  }

  @Test
  void shouldLogServerVersion() {
    when(engine.isSonarCloud()).thenReturn(false);
    when(engine.getServerVersion()).thenReturn("5.5");
    Properties p = new Properties();
    when(cli.isDisplayVersionOnly()).thenReturn(true);
    when(cli.getInvokedFrom()).thenReturn("");
    when(conf.properties()).thenReturn(p);

    Main main = new Main(exit, cli, conf, scannerEngineBootstrapperFactory);
    main.analyze();
    assertThat(logTester.logs(Level.INFO)).contains("Communicating with SonarQube Server 5.5");
  }

  @Test
  void should_log_SonarCloud_server() {
    when(engine.isSonarCloud()).thenReturn(true);
    Properties p = new Properties();
    when(conf.properties()).thenReturn(p);
    when(cli.getInvokedFrom()).thenReturn("");

    Main main = new Main(exit, cli, conf, scannerEngineBootstrapperFactory);
    main.analyze();
    assertThat(logTester.logs(Level.INFO)).contains("Communicating with SonarCloud");
  }

  @Test
  void should_configure_logging() {
    Properties analysisProps = testLogging("sonar.verbose", "true");
    assertThat(analysisProps.getProperty("sonar.verbose")).isEqualTo("true");
  }

  @Test
  void should_configure_logging_trace() {
    Properties analysisProps = testLogging("sonar.log.level", "TRACE");
    assertThat(analysisProps.getProperty("sonar.log.level")).isEqualTo("TRACE");
  }

  @Test
  void should_set_bootstrap_start_time_in_millis() {
    Properties analysisProps = execute("sonar.scanner.bootstrapStartTime", "1714137496104");
    assertThat(analysisProps.getProperty("sonar.scanner.bootstrapStartTime")).isEqualTo("1714137496104");
  }

  @Test
  void should_configure_logging_debug() {
    Properties analysisProps = testLogging("sonar.log.level", "DEBUG");
    assertThat(analysisProps.getProperty("sonar.log.level")).isEqualTo("DEBUG");
  }

  private Properties testLogging(String propKey, String propValue) {
    Properties actualProps = execute(propKey, propValue);

    // Logger used for callback should have debug enabled
    assertThat(LoggerFactory.getLogger(getClass()).isDebugEnabled()).isTrue();

    return actualProps;
  }

  private Properties execute(String propKey, String propValue) {
    Properties p = new Properties();
    p.put(propKey, propValue);

    when(conf.properties()).thenReturn(p);
    when(cli.getInvokedFrom()).thenReturn("");

    Main main = new Main(exit, cli, conf, scannerEngineBootstrapperFactory);
    main.analyze();

    ArgumentCaptor<Properties> propertiesCapture = ArgumentCaptor.forClass(Properties.class);
    verify(engine).analyze((Map) propertiesCapture.capture());

    return propertiesCapture.getValue();
  }

}
