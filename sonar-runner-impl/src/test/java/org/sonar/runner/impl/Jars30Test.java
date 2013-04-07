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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class Jars30Test {

  ServerConnection connection = mock(ServerConnection.class);
  JarExtractor jarExtractor = mock(JarExtractor.class);

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void should_download_jar_files() throws Exception {
    File batchJar = temp.newFile("sonar-runner-batch.jar");
    when(jarExtractor.extractToTemp("sonar-runner-batch")).thenReturn(batchJar);
    // index of the files to download
    when(connection.downloadString("/batch/")).thenReturn("cpd.jar,squid.jar");

    Jars30 jars30 = new Jars30(connection);
    List<File> files = jars30.download(temp.newFolder(), jarExtractor);

    assertThat(files).isNotNull();
    verify(connection, times(1)).downloadString("/batch/");
    verify(connection, times(1)).download(eq("/batch/cpd.jar"), any(File.class));
    verify(connection, times(1)).download(eq("/batch/squid.jar"), any(File.class));
    verifyNoMoreInteractions(connection);
  }

  @Test
  public void should_fail_to_download_files() throws Exception {
    File batchJar = temp.newFile("sonar-runner-batch.jar");
    when(jarExtractor.extractToTemp("sonar-runner-batch")).thenReturn(batchJar);
    // index of files to download
    when(connection.downloadString("/batch/")).thenReturn("cpd.jar,squid.jar");
    doThrow(new IllegalStateException()).when(connection).download(eq("/batch/squid.jar"), any(File.class));

    Jars30 jars30 = new Jars30(connection);
    try {
      jars30.download(temp.newFolder(), jarExtractor);
      fail();
    } catch (RuntimeException e) {
      assertThat(e).hasMessage("Fail to download libraries from server");
    }

  }
}
