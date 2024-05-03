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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.event.Level;
import testutils.LogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CliTest {

  @RegisterExtension
  LogTester logTester = new LogTester();

  private final Exit exit = mock(Exit.class);
  private Cli cli = new Cli(exit);

  @Test
  void should_parse_empty_arguments() {
    cli.parse(new String[0]);
    assertThat(cli.properties()).isNotEmpty();
    assertThat(cli.isDebugEnabled()).isFalse();
    assertThat(cli.isDisplayVersionOnly()).isFalse();
    assertThat(cli.isEmbedded()).isFalse();
  }

  @Test
  void should_extract_properties() {
    cli.parse(new String[]{"-D", "foo=bar", "--define", "hello=world", "-Dboolean"});
    assertThat(cli.properties()).contains(
      entry("foo", "bar"),
      entry("hello", "world"),
      entry("boolean", "true"));
  }

  @Test
  void should_warn_on_duplicate_properties() {
    cli = new Cli(exit);
    cli.parse(new String[]{"-D", "foo=bar", "--define", "foo=baz"});
    assertThat(logTester.logs(Level.WARN)).contains("Property 'foo' with value 'bar' is overridden with value 'baz'");
  }

  @Test
  void should_fail_on_missing_prop() {
    cli = new Cli(exit);
    cli.parse(new String[]{"-D"});
    assertThat(logTester.logs(Level.ERROR)).contains("Missing argument for option -D/--define");
    verify(exit).exit(Exit.INTERNAL_ERROR);
  }

  @Test
  void should_not_fail_with_errors_option() {
    assertThatNoException().isThrownBy(() -> cli.parse(new String[]{"-e"}));
  }

  @Test
  void should_enable_debug_mode() {
    cli.parse(new String[]{"-X"});
    assertThat(cli.isDebugEnabled()).isTrue();
    assertThat(cli.properties()).containsEntry("sonar.verbose", "true");
  }

  @Test
  void should_enable_debug_mode_full() {
    cli.parse(new String[]{"--debug"});
    assertThat(cli.isDebugEnabled()).isTrue();
    assertThat(cli.properties()).containsEntry("sonar.verbose", "true");
  }

  @Test
  void should_show_version() {
    cli.parse(new String[]{"-v"});
    assertThat(cli.isDisplayVersionOnly()).isTrue();
  }

  @Test
  void should_show_version_full() {
    cli.parse(new String[]{"--version"});
    assertThat(cli.isDisplayVersionOnly()).isTrue();
  }

  @Test
  void should_enable_stacktrace_log() {
    cli.parse(new String[]{"-e"});
    assertThat(cli.isDebugEnabled()).isFalse();
    assertThat(cli.properties().get("sonar.verbose")).isNull();
  }

  @Test
  void should_enable_stacktrace_log_full() {
    cli.parse(new String[]{"--errors"});
    assertThat(cli.isDebugEnabled()).isFalse();
    assertThat(cli.properties().get("sonar.verbose")).isNull();
  }

  @Test
  void should_parse_from_argument() {
    cli.parse(new String[]{"--from=ScannerMSBuild/4.8"});
    assertThat(cli.getInvokedFrom()).isNotEmpty();
    assertThat(cli.getInvokedFrom()).isEqualTo("ScannerMSBuild/4.8");
  }

  @Test
  void from_argument_is_only_from_let_value_empty() {
    cli.parse(new String[]{"--from="});
    assertThat(cli.getInvokedFrom()).isEmpty();
  }

  @Test
  void should_disable_debug_mode_and_stacktrace_log_by_default() {
    cli.parse(new String[0]);
    assertThat(cli.isDebugEnabled()).isFalse();
    assertThat(cli.properties().get("sonar.verbose")).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"-h", "--help"})
  void should_show_usage(String arg) {
    var baos = parseAndCaptureStdOut(arg);
    assertThat(baos.toString()).contains("usage: sonar-scanner [options]");
    verify(exit).exit(Exit.SUCCESS);
  }

  private ByteArrayOutputStream parseAndCaptureStdOut(String arg) {
    var baos = new ByteArrayOutputStream();
    var savedOut = System.out;
    try {
      System.setOut(new PrintStream(baos));
      cli = new Cli(exit);
      cli.parse(new String[]{arg});
    } finally {
      System.setOut(savedOut);
    }
    return baos;
  }

  @Test
  void should_show_usage_on_bad_syntax() {
    var baos = parseAndCaptureStdOut("-w");
    assertThat(baos.toString()).contains("usage: sonar-scanner [options]");
    assertThat(logTester.logs(Level.ERROR)).contains("Unrecognized option: -w");
    verify(exit).exit(Exit.INTERNAL_ERROR);
  }

  @Test
  void should_enable_embedded_mode() {
    cli.parse(new String[]{"--embedded"});
    assertThat(cli.isEmbedded()).isTrue();
  }
}
