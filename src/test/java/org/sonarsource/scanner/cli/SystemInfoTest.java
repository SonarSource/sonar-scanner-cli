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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonarsource.scanner.cli.SystemInfo.System2;
import testutils.LogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemInfoTest {
  @RegisterExtension
  LogTester logTester = new LogTester();

  private final System2 mockSystem = mock(System2.class);

  @BeforeEach
  void setUp() {
    SystemInfo.setSystem(mockSystem);
  }

  @Test
  void test_java() {
    mockJava();
    assertThat(SystemInfo.java()).isEqualTo("Java 1.9 oracle (64-bit)");

    when(mockSystem.getProperty("sun.arch.data.model")).thenReturn("32");
    assertThat(SystemInfo.java()).isEqualTo("Java 1.9 oracle (32-bit)");

    when(mockSystem.getProperty("sun.arch.data.model")).thenReturn(null);
    assertThat(SystemInfo.java()).isEqualTo("Java 1.9 oracle");
  }

  @Test
  void test_os() {
    mockOs();

    assertThat(SystemInfo.os()).isEqualTo("linux 2.5 x64");
  }

  private void mockJava() {
    when(mockSystem.getProperty("java.version")).thenReturn("1.9");
    when(mockSystem.getProperty("java.vendor")).thenReturn("oracle");
    when(mockSystem.getProperty("sun.arch.data.model")).thenReturn("64");
  }

  private void mockOs() {
    when(mockSystem.getProperty("os.version")).thenReturn("2.5");
    when(mockSystem.getProperty("os.arch")).thenReturn("x64");
    when(mockSystem.getProperty("os.name")).thenReturn("linux");
  }

  @Test
  void should_print() {
    mockOs();
    mockJava();
    when(mockSystem.getenv("SONAR_SCANNER_OPTS")).thenReturn("arg");

    SystemInfo.print();

    verify(mockSystem).getProperty("java.version");
    verify(mockSystem).getProperty("os.version");
    verify(mockSystem).getenv("SONAR_SCANNER_OPTS");

    assertThat(logTester.logs(Level.INFO))
      .containsOnly("SonarScanner CLI " + ScannerVersion.version(), "Java 1.9 oracle (64-bit)", "linux 2.5 x64", "SONAR_SCANNER_OPTS=arg");
  }

  @Test
  void should_not_print_sensitive_data() {
    mockOs();
    mockJava();
    when(mockSystem.getenv("SONAR_SCANNER_OPTS"))
      .thenReturn("-Dsonar.login=login -Dsonar.whatever=whatever -Dsonar.password=password -Dsonar.whatever2=whatever2 -Dsonar.token=token");

    SystemInfo.print();

    assertThat(logTester.logs(Level.INFO)).contains("SONAR_SCANNER_OPTS=-Dsonar.login=* -Dsonar.whatever=whatever -Dsonar.password=* -Dsonar.whatever2=whatever2 -Dsonar.token=*");
  }
}
