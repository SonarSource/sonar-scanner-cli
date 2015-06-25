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
package org.sonar.runner.impl;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URL;

public class JarExtractor {

  public File extractToTemp(String filenameWithoutSuffix) {
    String filename = filenameWithoutSuffix + ".jar";
    URL url = getClass().getResource("/" + filename);
    try {
      File copy = File.createTempFile(filenameWithoutSuffix, ".jar");
      FileUtils.copyURLToFile(url, copy);
      return copy;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to extract " + filename, e);
    }
  }
}

