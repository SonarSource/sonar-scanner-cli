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
import org.sonar.runner.api.EmbeddedRunner;
import org.sonar.runner.api.ForkedRunner;
import org.sonar.runner.api.Runner;

import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;

public class RunnerFactoryTest {

  Properties props = new Properties();

  @Test
  public void should_create_embedded_runner_by_default() {
    props.setProperty("foo", "bar");
    Runner<?> runner = new RunnerFactory().create(props);

    assertThat(runner).isInstanceOf(EmbeddedRunner.class);
    assertThat(runner.globalProperties().get("foo")).isEqualTo("bar");
  }

  @Test
  public void should_create_forked_runner() {
    props.setProperty("foo", "bar");
    props.setProperty("sonarRunner.mode", "fork");
    props.setProperty("sonarRunner.fork.jvmArgs", "-Xms128m -Xmx512m");
    Runner<?> runner = new RunnerFactory().create(props);

    assertThat(runner).isInstanceOf(ForkedRunner.class);
    assertThat(runner.globalProperties().get("foo")).isEqualTo("bar");
    assertThat(((ForkedRunner) runner).jvmArguments()).contains("-Xms128m", "-Xmx512m");
  }

  @Test
    public void should_create_forked_runner_with_jvm_arguments() {
      props.setProperty("foo", "bar");
      props.setProperty("sonarRunner.mode", "fork");
      Runner<?> runner = new RunnerFactory().create(props);

      assertThat(runner).isInstanceOf(ForkedRunner.class);
      assertThat(runner.globalProperties().get("foo")).isEqualTo("bar");

    assertThat(runner).isInstanceOf(ForkedRunner.class);
    assertThat(runner.globalProperties().get("foo")).isEqualTo("bar");
    assertThat(((ForkedRunner) runner).jvmArguments()).isEmpty();
  }
}
