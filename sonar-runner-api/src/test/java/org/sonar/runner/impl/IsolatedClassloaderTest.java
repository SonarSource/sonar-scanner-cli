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

import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static org.fest.assertions.Assertions.assertThat;

public class IsolatedClassloaderTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private IsolatedClassloader classLoader;

  @Before
  public void setUp() {
    ClassLoader parent = getClass().getClassLoader();
    classLoader = new IsolatedClassloader(parent, new ClassloadRules(new HashSet<String>(), new HashSet<String>()));
  }

  @Test
  public void should_use_isolated_system_classloader_when_parent_is_excluded() throws ClassNotFoundException, IOException {
    thrown.expect(ClassNotFoundException.class);
    thrown.expectMessage("org.junit.Test");

    // JUnit is available in the parent classloader (classpath used to execute this test) but not in the core JVM
    assertThat(classLoader.loadClass("java.lang.String", false)).isNotNull();
    classLoader.loadClass("org.junit.Test", false);
    classLoader.close();
  }

  @Test
  public void should_use_parent_to_load() throws ClassNotFoundException, IOException {
    ClassloadRules rules = mock(ClassloadRules.class);
    when(rules.canLoad("org.junit.Test")).thenReturn(true);
    classLoader = new IsolatedClassloader(getClass().getClassLoader(), rules);
    assertThat(classLoader.loadClass("org.junit.Test", false)).isNotNull();
  }

  @Test
  public void add_jars() throws MalformedURLException {
    File f = new File("dummy");
    File[] files = {f};
    classLoader.addFiles(Arrays.asList(files));

    assertThat(classLoader.getURLs()).contains(f.toURI().toURL());
  }

  @Test
  public void error_add_jars() {
    File f = mock(File.class);
    when(f.toURI()).thenThrow(MalformedURLException.class);
    File[] files = {f};

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Fail to create classloader");

    classLoader.addFiles(Arrays.asList(files));
  }

  @Test
  public void dont_get_resource_from_parent() {
    URL resource2 = classLoader.getParent().getResource("fake.jar");
    assertThat(resource2).isNotNull();

    // should not find resource through parent classloader
    URL resource = classLoader.getResource("fake.jar");
    assertThat(resource).isNull();
  }

  @Test
  public void dont_get_resources_from_parent() throws IOException {
    Enumeration<URL> resource2 = classLoader.getParent().getResources("fake.jar");
    assertThat(resource2.hasMoreElements()).isTrue();

    // should not find resource through parent classloader
    Enumeration<URL> resource = classLoader.getResources("fake.jar");
    assertThat(resource.hasMoreElements()).isFalse();
  }
}
