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

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.sonarsource.scanner.lib.ScannerEngineBootstrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScannerEngineBootstrapperFactoryTest {

  private final Properties props = new Properties();
  private final Logs logs = mock(Logs.class);
  private final ScannerEngineBootstrapperFactory underTest = new ScannerEngineBootstrapperFactory(logs);

  @Test
  void should_create_engine_bootstrapper_and_pass_app_and_properties() {
    props.setProperty("foo", "bar");
    var spy = spy(underTest);
    var mockedBootstrapper = mock(ScannerEngineBootstrapper.class);
    when(mockedBootstrapper.addBootstrapProperties(any())).thenReturn(mockedBootstrapper);
    when(spy.newScannerEngineBootstrapper(any(), any())).thenReturn(mockedBootstrapper);

    var bootstrapper = spy.create(props, "");

    assertThat(bootstrapper).isNotNull();
    verify(spy).newScannerEngineBootstrapper(eq("ScannerCLI"), notNull());
    verify(mockedBootstrapper).addBootstrapProperties(argThat(props::equals));
  }

  @Test
  void should_create_engine_bootstrapper_with_app_from_argument() {
    var spy = spy(underTest);
    var mockedBootstrapper = mock(ScannerEngineBootstrapper.class);
    when(mockedBootstrapper.addBootstrapProperties(any())).thenReturn(mockedBootstrapper);
    when(spy.newScannerEngineBootstrapper(any(), any())).thenReturn(mockedBootstrapper);

    var bootstrapper = spy.create(props, "ScannerMSBuild/4.8.0");

    assertThat(bootstrapper).isNotNull();
    verify(spy).newScannerEngineBootstrapper("ScannerMSBuild", "4.8.0");
  }

  @Test
  void if_from_argument_is_not_regex_compliant_revert_to_default_scanner_name() {
    var spy = spy(underTest);
    var mockedBootstrapper = mock(ScannerEngineBootstrapper.class);
    when(mockedBootstrapper.addBootstrapProperties(any())).thenReturn(mockedBootstrapper);
    when(spy.newScannerEngineBootstrapper(any(), any())).thenReturn(mockedBootstrapper);

    var bootstrapper = spy.create(props, "ScannerMSBuild4.8.0WithoutSlash");

    assertThat(bootstrapper).isNotNull();
    verify(spy).newScannerEngineBootstrapper(eq("ScannerCLI"), notNull());
  }


}
