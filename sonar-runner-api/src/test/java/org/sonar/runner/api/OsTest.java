/*
 * SonarQube Runner - API
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

import org.apache.commons.lang.SystemUtils;
import org.junit.Test;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class OsTest {
  @Test
  public void testIsWindows() throws Exception {
    assertThat(new Os().isWindows()).isEqualTo(SystemUtils.IS_OS_WINDOWS);
  }

  @Test
  public void testUsedJavaHome() throws Exception {
    File javaHome = new Os().thisJavaHome();
    assertThat(javaHome).isNotNull().exists().isDirectory();
  }

  @Test
  public void testUsedJavaExe() throws Exception {
    File javaExe = new Os().thisJavaExe();
    assertThat(javaExe).isNotNull().isFile().exists();
    assertThat(javaExe.getName()).contains("java");
  }
}
