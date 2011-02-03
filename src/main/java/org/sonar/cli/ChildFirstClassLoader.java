/*
 * Sonar CLI
 * Copyright (C) 2009 SonarSource
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

package org.sonar.cli;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class ChildFirstClassLoader extends URLClassLoader {
  public ChildFirstClassLoader() {
    super(new URL[0]);
  }

  @Override
  public void addURL(URL url) {
    super.addURL(url);
  }

  public void addFile(File file) {
    try {
      addURL(file.toURI().toURL());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Searches for classes using child-first strategy.
   */
  @Override
  protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    // First, check if the class has already been loaded
    Class<?> c = findLoadedClass(name);
    // If not loaded, search the local (child) resources
    if (c == null) {
      try {
        c = findClass(name);
      } catch (ClassNotFoundException e) {
        // ignore
      }
    }
    // If we could not find it, delegate to parent
    // Note that we don't attempt to catch any ClassNotFoundException
    if (c == null) {
      if (getParent() != null) {
        c = getParent().loadClass(name);
      } else {
        c = getSystemClassLoader().loadClass(name);
      }
    }
    if (resolve) {
      resolveClass(c);
    }
    return c;
  }
}
