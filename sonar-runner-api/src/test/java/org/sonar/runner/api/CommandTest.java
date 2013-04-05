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

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.fest.assertions.MapAssert.entry;

public class CommandTest {
  @Test
  public void test_simple_build() throws Exception {
    Command command = Command.builder().setExecutable("java").build();
    assertThat(command.executable()).isEqualTo("java");
    assertThat(command.envVariables()).isEmpty();
    assertThat(command.arguments()).isEmpty();
    assertThat(command.toStrings()).containsOnly("java");
    assertThat(command.toString()).isEqualTo("java");
  }

  @Test
  public void test_arguments_and_env_variables() throws Exception {
    Map<String, String> env = new HashMap<String, String>();
    env.put("USER_HOME", "/user");

    Command command = Command.builder()
        .setExecutable("java")
        .addArguments("-Dfoo=bar", "-Djava.io.tmpdir=/tmp")
        .addArguments(Arrays.asList("-Xmx512m"))
        .setEnvVariable("JAVA_HOME", "/path/to/jdk")
        .addEnvVariables(env)
        .build();

    assertThat(command.executable()).isEqualTo("java");
    assertThat(command.envVariables()).hasSize(2).includes(
        entry("JAVA_HOME", "/path/to/jdk"),
        entry("USER_HOME", "/user")
    );
    assertThat(command.arguments()).containsSequence("-Dfoo=bar", "-Djava.io.tmpdir=/tmp", "-Xmx512m");
    assertThat(command.toStrings()).containsOnly("java", "-Dfoo=bar", "-Djava.io.tmpdir=/tmp", "-Xmx512m");
    assertThat(command.toString()).isEqualTo("java -Dfoo=bar -Djava.io.tmpdir=/tmp -Xmx512m");
  }

  @Test
  public void executable_should_be_required() {
    try {
      Command.builder().build();
      fail();
    } catch (IllegalArgumentException e) {
      // success
    }
  }
}
