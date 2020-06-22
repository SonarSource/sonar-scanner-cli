/*
 * SonarScanner CLI
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

import java.util.Properties;
import org.junit.Test;
import org.sonarsource.scanner.api.EmbeddedScanner;
import org.sonarsource.scanner.api.LogOutput;
import org.sonarsource.scanner.api.LogOutput.Level;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ScannerFactoryTest {

  private final Properties props = new Properties();
  private final Logs logs = mock(Logs.class);

  @Test
  public void should_create_embedded_runner() {
    props.setProperty("foo", "bar");
    EmbeddedScanner runner = new ScannerFactory(logs).create(props, "");

    assertThat(runner).isInstanceOf(EmbeddedScanner.class);
    assertThat(runner.globalProperties()).containsEntry("foo", "bar");
    assertThat(runner.app()).isEqualTo("ScannerCLI");
    assertThat(runner.appVersion()).isNotNull();
  }

  @Test
  public void should_create_embedded_runner_with_scannername_from_argument() {
    props.setProperty("foo", "bar");
    EmbeddedScanner runner = new ScannerFactory(logs).create(props, "ScannerMSBuild/4.8.0");

    assertThat(runner).isInstanceOf(EmbeddedScanner.class);
    assertThat(runner.globalProperties()).containsEntry("foo", "bar");
    assertThat(runner.app()).isEqualTo("ScannerMSBuild");
    assertThat(runner.appVersion()).isEqualTo("4.8.0");
    assertThat(runner.appVersion()).isNotNull();
  }

  @Test
  public void should_create_embedded_runner_from_argument_is_not_regex_compliant_revert_to_default_scanner_name() {
    props.setProperty("foo", "bar");
    EmbeddedScanner runner = new ScannerFactory(logs).create(props, "ScannerMSBuild4.8.0");

    assertThat(runner).isInstanceOf(EmbeddedScanner.class);
    assertThat(runner.globalProperties()).containsEntry("foo", "bar");
    assertThat(runner.app()).isEqualTo("ScannerCLI");
    assertThat(runner.appVersion()).isNotNull();
  }

  @Test
  public void should_fwd_logs() {
    LogOutput logOutput = new ScannerFactory(logs).new DefaultLogOutput();

    String msg = "test";

    logOutput.log(msg, Level.DEBUG);
    verify(logs).debug(msg);
    verifyNoMoreInteractions(logs);
    reset(logs);

    logOutput.log(msg, Level.INFO);
    verify(logs).info(msg);
    verifyNoMoreInteractions(logs);
    reset(logs);

    logOutput.log(msg, Level.ERROR);
    verify(logs).error(msg);
    verifyNoMoreInteractions(logs);
    reset(logs);

    logOutput.log(msg, Level.WARN);
    verify(logs).warn(msg);
    verifyNoMoreInteractions(logs);
    reset(logs);

    logOutput.log(msg, Level.TRACE);
    verify(logs).debug(msg);
    verifyNoMoreInteractions(logs);
    reset(logs);
  }

}
