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

import java.nio.charset.Charset;

import static org.fest.assertions.Assertions.assertThat;

public class SourceEncodingTest {

  SourceEncoding encoding = new SourceEncoding();
  Runner runner = new SimpleRunner();

  @Test
  public void should_set_default_platform_encoding() throws Exception {
    runner.setProperty("sonar.task", "scan");
    encoding.init(runner);
    assertThat(runner.property("sonar.sourceEncoding", null)).isEqualTo(Charset.defaultCharset().name());
  }

  @Test
  public void should_use_parameterized_encoding() throws Exception {
    runner.setProperty("sonar.task", "scan");
    runner.setProperty("sonar.sourceEncoding", "THE_ISO_1234");
    encoding.init(runner);
    assertThat(runner.property("sonar.sourceEncoding", null)).isEqualTo("THE_ISO_1234");
  }

  @Test
  public void should_not_init_encoding_if_not_project_task() throws Exception {
    runner.setProperty("sonar.task", "views");
    encoding.init(runner);
    assertThat(runner.property("sonar.sourceEncoding", null)).isNull();
  }
}
