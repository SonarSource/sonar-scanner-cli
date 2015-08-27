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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.List;

/**
 * Special {@link java.net.URLClassLoader} to execute batch, which restricts loading from parent.
 */
class IsolatedClassloader extends URLClassLoader {
  private final ClassloadRules rules;

  /**
   * The parent classloader is used only for loading classes and resources in unmasked packages
   */
  IsolatedClassloader(ClassLoader parent, ClassloadRules rules) {
    super(new URL[0], parent);
    this.rules = rules;
  }

  void addFiles(List<File> files) {
    try {
      for (File file : files) {
        addURL(file.toURI().toURL());
      }
    } catch (MalformedURLException e) {
      throw new IllegalStateException("Fail to create classloader", e);
    }
  }

  /**
   * Same behavior as in {@link java.net.URLClassLoader#loadClass(String, boolean)}, except loading from parent.
   */
  @Override
  protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    // First, check if the class has already been loaded
    Class<?> c = findLoadedClass(name);
    if (c == null) {
      try {
        // Load from parent
        if (getParent() != null && rules.canLoad(name)) {
          c = getParent().loadClass(name);
        } else {

          // Load from system

          // I don't know for other vendors, but for Oracle JVM :
          // - ClassLoader.getSystemClassLoader() is sun.misc.Launcher$AppClassLoader. It contains app classpath.
          // - ClassLoader.getSystemClassLoader().getParent() is sun.misc.Launcher$ExtClassLoader. It contains core JVM
          ClassLoader systemClassLoader = getSystemClassLoader();
          if (systemClassLoader.getParent() != null) {
            systemClassLoader = systemClassLoader.getParent();
          }
          c = systemClassLoader.loadClass(name);
        }
      } catch (ClassNotFoundException e) {
        // If still not found, then invoke findClass in order
        // to find the class.
        c = findClass(name);
      }
    }
    if (resolve) {
      resolveClass(c);
    }
    return c;
  }

  /**
   * Unlike {@link java.net.URLClassLoader#getResource(String)} don't return resource from parent.
   * See http://jira.codehaus.org/browse/SONAR-2276
   */
  @Override
  public URL getResource(String name) {
    return findResource(name);
  }

  /**
   * Unlike {@link java.net.URLClassLoader#getResources(String)} don't return resources from parent.
   * See http://jira.codehaus.org/browse/SONAR-2276
   */
  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    return findResources(name);
  }

}
