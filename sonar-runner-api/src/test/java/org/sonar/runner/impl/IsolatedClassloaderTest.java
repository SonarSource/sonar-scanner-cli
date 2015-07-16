/*
 * SonarQube Runner - API
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
package org.sonar.runner.impl;

import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.fest.assertions.Assertions.assertThat;

public class IsolatedClassloaderTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void should_use_isolated_system_classloader_when_parent_is_excluded() throws ClassNotFoundException, IOException {
    thrown.expect(ClassNotFoundException.class);
    thrown.expectMessage("org.junit.Test");
    ClassLoader parent = getClass().getClassLoader();
    IsolatedClassloader classLoader = new IsolatedClassloader(parent);

    // JUnit is available in the parent classloader (classpath used to execute this test) but not in the core JVM
    assertThat(classLoader.loadClass("java.lang.String", false)).isNotNull();
    classLoader.loadClass("org.junit.Test", false);
    classLoader.close();
  }
}
