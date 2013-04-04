/*
 * Sonar Runner - API
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
package org.sonar.runner.api;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.runner.impl.JarExtractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ForkedRunnerTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void should_create() {
    ForkedRunner runner = ForkedRunner.create();
    assertThat(runner).isNotNull().isInstanceOf(ForkedRunner.class);
  }

  @Test
  public void test_java_command() throws IOException {
    JarExtractor jarExtractor = mock(JarExtractor.class);
    File jar = temp.newFile();
    when(jarExtractor.extract("sonar-runner-impl")).thenReturn(jar);

    ForkedRunner runner = new ForkedRunner(jarExtractor);
    runner.setJavaCommand("java");
    runner.setProperty("sonar.dynamicAnalysis", "false");
    runner.setProperty("sonar.login", "admin");
    runner.addJvmArguments("-Xmx512m");
    runner.setJvmEnvVariable("SONAR_HOME", "/path/to/sonar");

    Command command = runner.createCommand();
    assertThat(command).isNotNull();
    assertThat(command.toStrings()).hasSize(6);
    assertThat(command.toStrings()[0]).isEqualTo("java");
    assertThat(command.toStrings()[1]).isEqualTo("-Xmx512m");
    assertThat(command.toStrings()[2]).isEqualTo("-cp");
    assertThat(command.toStrings()[3]).isEqualTo(jar.getAbsolutePath());
    assertThat(command.toStrings()[4]).isEqualTo("org.sonar.runner.impl.BatchLauncherMain");

    // the properties
    String propsPath = command.toStrings()[5];
    assertThat(propsPath).endsWith(".properties");
    Properties properties = new Properties();
    properties.load(new FileInputStream(propsPath));
    assertThat(properties.size()).isGreaterThan(2);
    assertThat(properties.getProperty("sonar.dynamicAnalysis")).isEqualTo("false");
    assertThat(properties.getProperty("sonar.login")).isEqualTo("admin");
    assertThat(properties.getProperty("-Xmx512m")).isNull();
    assertThat(properties.getProperty("SONAR_HOME")).isNull();
    // default values
    assertThat(properties.getProperty("sonar.task")).isEqualTo("scan");
    assertThat(properties.getProperty("sonar.host.url")).isEqualTo("http://localhost:9000");
  }
}
