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

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.home.cache.Logger;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class TempCleaningTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void should_clean_jvm_tmp_dir() {
    TempCleaning cleaning = new TempCleaning(mock(Logger.class));
    assertThat(cleaning.tempDir).isDirectory().exists();
  }

  @Test
  public void should_clean() throws Exception {
    File dir = temp.newFolder();
    File oldBatch = new File(dir, "sonar-runner-batch656.jar");
    FileUtils.write(oldBatch, "foo");
    oldBatch.setLastModified(System.currentTimeMillis() - 3 * TempCleaning.ONE_DAY_IN_MILLISECONDS);

    File youngBatch = new File(dir, "sonar-runner-batch123.jar");
    FileUtils.write(youngBatch, "foo");

    File doNotDelete = new File(dir, "jacoco.txt");
    FileUtils.write(doNotDelete, "foo");

    assertThat(oldBatch).exists();
    assertThat(youngBatch).exists();
    assertThat(doNotDelete).exists();
    new TempCleaning(dir, mock(Logger.class)).clean();

    assertThat(oldBatch).doesNotExist();
    assertThat(youngBatch).exists();
    assertThat(doNotDelete).exists();
  }
}
