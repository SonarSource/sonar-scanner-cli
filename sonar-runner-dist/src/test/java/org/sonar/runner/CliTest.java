/*
 * SonarQube Runner - Distribution
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

import static org.fest.assertions.Assertions.assertThat;

public class CliTest {

  Cli cli = new Cli();

  @Test
  public void should_parse_empty_arguments() {
    cli.parse(new String[0]);
    assertThat(cli.properties()).isNotEmpty();
    assertThat(cli.isDebugMode()).isFalse();
    assertThat(cli.isDisplayStackTrace()).isFalse();
    assertThat(cli.isDisplayVersionOnly()).isFalse();
  }

  @Test
  public void should_extract_properties() {
    cli.parse(new String[]{"-D", "foo=bar", "--define", "hello=world", "-Dboolean"});
    assertThat(cli.properties().get("foo")).isEqualTo("bar");
    assertThat(cli.properties().get("hello")).isEqualTo("world");
    assertThat(cli.properties().get("boolean")).isEqualTo("true");
  }

  @Test
  public void should_parse_optional_task() {
    cli.parse(new String[]{"-D", "foo=bar"});
    assertThat(cli.properties().get("sonar.task")).isNull();

    cli.parse(new String[]{"views", "-D", "foo=bar"});
    assertThat(cli.properties().get("sonar.task")).isEqualTo("views");
  }

  @Test
  public void should_enable_debug_mode() {
    cli.parse(new String[]{"-X"});
    assertThat(cli.isDebugMode()).isTrue();
    assertThat(cli.isDisplayStackTrace()).isTrue();
    assertThat(cli.properties().get("sonar.verbose")).isEqualTo("true");
  }

  @Test
  public void should_enable_stacktrace_log() {
    cli.parse(new String[]{"-e"});
    assertThat(cli.isDebugMode()).isFalse();
    assertThat(cli.isDisplayStackTrace()).isTrue();
    assertThat(cli.properties().get("sonar.verbose")).isNull();
  }

  @Test
  public void should_disable_debug_mode_and_stacktrace_log_by_default() {
    cli.parse(new String[0]);
    assertThat(cli.isDebugMode()).isFalse();
    assertThat(cli.isDisplayStackTrace()).isFalse();
    assertThat(cli.properties().get("sonar.verbose")).isNull();
  }
}
