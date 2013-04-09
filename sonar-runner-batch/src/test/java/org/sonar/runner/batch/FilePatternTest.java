/*
 * Sonar Runner - Batch
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
package org.sonar.runner.batch;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import static org.fest.assertions.Assertions.assertThat;

public class FilePatternTest {

  File basedir, rootFile, subFile;
  String basePath;

  @Before
  public void init() throws Exception {
    rootFile = new File(getClass().getResource("/org/sonar/runner/batch/FilePatternTest/root.txt").toURI());
    subFile = new File(getClass().getResource("/org/sonar/runner/batch/FilePatternTest/subdir/subfile.txt").toURI());
    basedir = rootFile.getParentFile();
    basePath = path(basedir);
  }

  @Test
  public void should_list_files_by_relative_path() throws Exception {
    assertThat(new FilePattern().listFiles(basedir, "subdir/*.txt")).containsOnly(subFile);
    assertThat(new FilePattern().listFiles(basedir, "*.txt")).containsOnly(rootFile);
    assertThat(new FilePattern().listFiles(basedir, "root.txt")).containsOnly(rootFile);
    assertThat(new FilePattern().listFiles(basedir, "ro*t.txt")).containsOnly(rootFile);
    assertThat(new FilePattern().listFiles(basedir, "ro?t.txt")).containsOnly(rootFile);
    assertThat(new FilePattern().listFiles(basedir, "r?t.txt")).isEmpty();
    assertThat(new FilePattern().listFiles(basedir, "*")).containsOnly(rootFile);
    assertThat(new FilePattern().listFiles(basedir, "**/*")).containsOnly(rootFile, subFile);
    assertThat(new FilePattern().listFiles(basedir, "**/*.txt")).containsOnly(subFile, rootFile);
    assertThat(new FilePattern().listFiles(basedir, "**/*.jar")).isEmpty();
    assertThat(new FilePattern().listFiles(basedir, "elsewhere/root.txt")).isEmpty();
    assertThat(new FilePattern().listFiles(basedir, "elsewhere/subfile.txt")).isEmpty();
  }

  @Test
  public void should_list_files_by_absolute_path() throws Exception {
    assertOnly(new FilePattern().listFiles(basedir, basePath + "/*.txt"), rootFile);
    assertOnly(new FilePattern().listFiles(basedir, basePath + "/**/subdir/*"), subFile);
    assertOnly(new FilePattern().listFiles(basedir, path(rootFile)), rootFile);
    assertOnly(new FilePattern().listFiles(basedir, path(basedir) + "/*/subfile.txt"), subFile);
    assertThat(new FilePattern().listFiles(basedir, path(basedir) + "/**/*.txt")).containsOnly(subFile, rootFile);
    assertThat(new FilePattern().listFiles(basedir, path(basedir) + "/ElseWhere/**/*.txt")).isEmpty();
    assertThat(new FilePattern().listFiles(basedir, "/ElseWhere/**/*.txt")).isEmpty();
  }

  private void assertOnly(Collection<File> files, File file) throws Exception {
    assertThat(files).hasSize(1);
    assertThat(files.iterator().next().getCanonicalPath()).isEqualTo(file.getCanonicalPath());
  }

  private String path(File f) throws IOException {
    String s = FilenameUtils.separatorsToUnix(f.getCanonicalPath());
    return StringUtils.removeEnd(s, "/");
  }
}
