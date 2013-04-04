/*
 * Sonar Runner - Implementation
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
package org.sonar.runner.impl;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class JarExtractorTest {
  @Test
  public void test_extract() throws Exception {
    File jarFile = new JarExtractor().extract("fake");
    assertThat(jarFile).isFile().exists();
    assertThat(FileUtils.readFileToString(jarFile, "UTF-8")).isEqualTo("Fake jar for unit tests");
    assertThat(jarFile.toURI().toURL().toString()).doesNotContain("jar:file");
  }
}
