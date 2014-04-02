/*
 * SonarQube Runner - Implementation
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

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class JarDownloaderTest {

  ServerConnection serverConnection = mock(ServerConnection.class);
  ServerVersion serverVersion = mock(ServerVersion.class);
  Properties props = new Properties();
  JarDownloader downloader = spy(new JarDownloader(serverConnection, serverVersion));

  @Test
  public void should_download_3_7_jar_files() {
    when(serverVersion.is37Compatible()).thenReturn(true);
    doReturn(new ArrayList()).when(downloader).download();
    List<File> jarFiles = downloader.checkVersionAndDownload();
    assertThat(jarFiles).isNotNull();
  }

  @Test
  public void should_fail_if_2_x() {
    when(serverVersion.version()).thenReturn("2.10");
    when(serverVersion.is37Compatible()).thenReturn(false);
    try {
      downloader.checkVersionAndDownload();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("SonarQube 2.10 is not supported. Please upgrade SonarQube to version 3.7 or more.");
    }
  }
}
