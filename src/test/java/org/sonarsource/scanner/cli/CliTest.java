/*
 * SonarQube Scanner
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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CliTest {
  private Exit exit = mock(Exit.class);
  private Logs logs = new Logs(System.out, System.err);
  private Cli cli = new Cli(exit, logs);

  @Test
  public void should_parse_empty_arguments() {
    cli.parse(new String[0]);
    assertThat(cli.properties()).isNotEmpty();
    assertThat(cli.isDebugEnabled()).isFalse();
    assertThat(cli.isDisplayVersionOnly()).isFalse();
    assertThat(cli.isEmbedded()).isFalse();
  }

  @Test
  public void should_extract_properties() {
    cli.parse(new String[] {"-D", "foo=bar", "--define", "hello=world", "-Dboolean"});
    assertThat(cli.properties().get("foo")).isEqualTo("bar");
    assertThat(cli.properties().get("hello")).isEqualTo("world");
    assertThat(cli.properties().get("boolean")).isEqualTo("true");
  }

  @Test
  public void should_fail_on_missing_prop() {
    logs = mock(Logs.class);
    cli = new Cli(exit, logs);
    cli.parse(new String[] {"-D"});
    verify(logs).error("Missing argument for option -D/--define");
    verify(exit).exit(Exit.INTERNAL_ERROR);
  }

  @Test
  public void should_not_fail_with_errors_option() {
    cli.parse(new String[] {"-e"});
  }

  @Test
  public void should_parse_optional_task() {
    cli.parse(new String[] {"-D", "foo=bar"});
    assertThat(cli.properties().get("sonar.task")).isNull();

    cli.parse(new String[] {"views", "-D", "foo=bar"});
    assertThat(cli.properties().get("sonar.task")).isEqualTo("views");
  }

  @Test
  public void should_enable_debug_mode() {
    cli.parse(new String[] {"-X"});
    assertThat(cli.isDebugEnabled()).isTrue();
    assertThat(cli.properties().get("sonar.verbose")).isEqualTo("true");
  }

  @Test
  public void should_enable_debug_mode_full() {
    cli.parse(new String[] {"--debug"});
    assertThat(cli.isDebugEnabled()).isTrue();
    assertThat(cli.properties().get("sonar.verbose")).isEqualTo("true");
  }

  @Test
  public void should_show_version() {
    cli.parse(new String[] {"-v"});
    assertThat(cli.isDisplayVersionOnly()).isTrue();
  }

  @Test
  public void should_show_version_full() {
    cli.parse(new String[] {"--version"});
    assertThat(cli.isDisplayVersionOnly()).isTrue();
  }

  @Test
  public void should_enable_stacktrace_log() {
    cli.parse(new String[] {"-e"});
    assertThat(cli.isDebugEnabled()).isFalse();
    assertThat(cli.properties().get("sonar.verbose")).isNull();
  }

  @Test
  public void should_enable_stacktrace_log_full() {
    cli.parse(new String[] {"--errors"});
    assertThat(cli.isDebugEnabled()).isFalse();
    assertThat(cli.properties().get("sonar.verbose")).isNull();
  }

  @Test
  public void should_disable_debug_mode_and_stacktrace_log_by_default() {
    cli.parse(new String[0]);
    assertThat(cli.isDebugEnabled()).isFalse();
    assertThat(cli.properties().get("sonar.verbose")).isNull();
  }

  @Test
  public void should_show_usage() {
    logs = mock(Logs.class);
    cli = new Cli(exit, logs);
    cli.parse(new String[] {"-h"});
    verify(logs).info("usage: sonar-scanner [options]");
    verify(exit).exit(Exit.SUCCESS);
  }

  @Test
  public void should_show_usage_full() {
    logs = mock(Logs.class);
    cli = new Cli(exit, logs);
    cli.parse(new String[] {"--help"});
    verify(logs).info("usage: sonar-scanner [options]");
    verify(exit).exit(Exit.SUCCESS);
  }

  @Test
  public void should_show_usage_on_bad_syntax() {
    logs = mock(Logs.class);
    cli = new Cli(exit, logs);
    cli.parse(new String[] {"-w"});
    verify(logs).error("Unrecognized option: -w");
    verify(logs).info("usage: sonar-scanner [options]");
    verify(exit).exit(Exit.INTERNAL_ERROR);
  }

  @Test
  public void should_enable_embedded_mode() {
    cli.parse(new String[] {"--embedded"});
    assertThat(cli.isEmbedded()).isTrue();
  }
}
