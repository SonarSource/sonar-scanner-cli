/*
 * SonarQube Scanner
 * Copyright (C) 2011 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonarsource.scanner.cli;

import org.sonar.runner.api.LogOutput.Level;
import org.sonarsource.scanner.cli.Logs;
import org.sonarsource.scanner.cli.RunnerFactory;
import org.sonar.runner.api.LogOutput;
import org.junit.Before;

import java.util.Properties;

import static org.mockito.Mockito.verifyNoMoreInteractions;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import org.junit.Test;
import org.sonar.runner.api.EmbeddedRunner;
import static org.fest.assertions.Assertions.assertThat;

public class RunnerFactoryTest {

  Properties props = new Properties();
  Logs logs;

  @Before
  public void setUp() {
    logs = mock(Logs.class);
  }

  @Test
  public void should_create_embedded_runner() {
    props.setProperty("foo", "bar");
    EmbeddedRunner runner = new RunnerFactory(logs).create(props);

    assertThat(runner).isInstanceOf(EmbeddedRunner.class);
    assertThat(runner.globalProperties().get("foo")).isEqualTo("bar");
  }

  @Test
  public void should_fwd_logs() {
    LogOutput logOutput = new RunnerFactory(logs).new DefaultLogOutput();

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
    verify(logs).info(msg);
    verifyNoMoreInteractions(logs);
    reset(logs);

    logOutput.log(msg, Level.TRACE);
    verify(logs).debug(msg);
    verifyNoMoreInteractions(logs);
    reset(logs);
  }

}
