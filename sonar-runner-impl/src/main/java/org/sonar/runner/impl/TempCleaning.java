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
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.PrefixFileFilter;

import java.io.File;
import java.util.Collection;

/**
 * The file sonar-runner-batch.jar is locked by the classloader on Windows and can't be dropped at the end of the execution.
 * See {@link BatchLauncher}
 */
class TempCleaning {
  static final int ONE_DAY_IN_MILLISECONDS = 24 * 60 * 60 * 1000;

  final File tempDir;

  TempCleaning() {
    this(new File(System.getProperty("java.io.tmpdir")));
  }

  /**
   * For unit tests
   */
  TempCleaning(File tempDir) {
    this.tempDir = tempDir;
  }

  void clean() {
    long cutoff = System.currentTimeMillis() - ONE_DAY_IN_MILLISECONDS;
    Collection<File> files = FileUtils.listFiles(tempDir, new AndFileFilter(
        new PrefixFileFilter("sonar-runner-batch"),
        new AgeFileFilter(cutoff)
    ), null);

    for (File file : files) {
      FileUtils.deleteQuietly(file);
    }
  }
}
