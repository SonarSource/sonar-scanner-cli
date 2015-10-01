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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.runner.cache.FileCache;
import org.sonar.runner.cache.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class JarsTest {
  ServerConnection connection = mock(ServerConnection.class);
  JarExtractor jarExtractor = mock(JarExtractor.class);
  FileCache fileCache = mock(FileCache.class);

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void should_download_jar_files() throws Exception {
    File batchJar = temp.newFile("sonar-runner-batch.jar");
    when(jarExtractor.extractToTemp("sonar-runner-batch")).thenReturn(batchJar);
    // index of the files to download
    when(connection.loadString("/batch_bootstrap/index")).thenReturn(
      "cpd.jar|CA124VADFSDS\n" +
        "squid.jar|34535FSFSDF\n");

    Jars jars35 = new Jars(fileCache, connection, jarExtractor, mock(Logger.class));
    List<File> files = jars35.download();

    assertThat(files).isNotNull();
    verify(connection, times(1)).loadString("/batch_bootstrap/index");
    verifyNoMoreInteractions(connection);
    verify(fileCache, times(1)).get(eq("cpd.jar"), eq("CA124VADFSDS"), any(FileCache.Downloader.class));
    verify(fileCache, times(1)).get(eq("squid.jar"), eq("34535FSFSDF"), any(FileCache.Downloader.class));
    verifyNoMoreInteractions(fileCache);
  }

  @Test
  public void should_honor_sonarUserHome() throws IOException {
    Properties props = new Properties();
    File f = temp.newFolder();
    props.put("sonar.userHome", f.getAbsolutePath());
    Jars jars = new Jars(connection, jarExtractor, mock(Logger.class), props);
    assertThat(jars.getFileCache().getDir()).isEqualTo(new File(f, "cache"));
  }

  @Test
  public void should_fail_to_download_files() throws Exception {
    File batchJar = temp.newFile("sonar-runner-batch.jar");
    when(jarExtractor.extractToTemp("sonar-runner-batch")).thenReturn(batchJar);
    // index of the files to download
    when(connection.loadString("/batch_bootstrap/index")).thenThrow(new IllegalStateException());

    Jars jars35 = new Jars(fileCache, connection, jarExtractor, mock(Logger.class));
    try {
      jars35.download();
      fail();
    } catch (RuntimeException e) {
      assertThat(e).hasMessage("Fail to download libraries from server");
    }
  }

  @Test
  public void test_jar_downloader() throws Exception {
    Jars.BatchFileDownloader downloader = new Jars.BatchFileDownloader(connection);
    File toFile = temp.newFile();
    downloader.download("squid.jar", toFile);
    verify(connection).download("/batch/squid.jar", toFile);
  }
}
