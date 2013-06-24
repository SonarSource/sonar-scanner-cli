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

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ServerConnectionTest {

  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void should_download_to_string() throws Exception {
    httpServer.setMockResponseData("abcde");
    Properties props = new Properties();
    props.setProperty("sonar.host.url", httpServer.url());

    ServerConnection connection = ServerConnection.create(props);
    String response = connection.downloadString("/batch/index.txt");

    assertThat(response).isEqualTo("abcde");
  }

  @Test
  public void should_download_to_file() throws Exception {
    httpServer.setMockResponseData("abcde");
    Properties props = new Properties();
    props.setProperty("sonar.host.url", httpServer.url());

    ServerConnection connection = ServerConnection.create(props);
    File toFile = temp.newFile();
    connection.download("/batch/index.txt", toFile);

    assertThat(FileUtils.readFileToString(toFile)).isEqualTo("abcde");
  }

  @Test
  public void should_not_download_file_when_host_is_down() throws Exception {
    Properties props = new Properties();
    props.setProperty("sonar.host.url", "http://localhost:" + NetworkUtil.getNextAvailablePort());

    ServerConnection connection = ServerConnection.create(props);
    File toFile = temp.newFile();
    try {
      connection.download("/batch/index.txt", toFile);
      fail();
    } catch (Exception e) {
      // success
    }
  }

  @Test
  public void should_not_download_string_when_host_is_down() throws Exception {
    Properties props = new Properties();
    props.setProperty("sonar.host.url", "http://localhost:" + NetworkUtil.getNextAvailablePort());

    ServerConnection connection = ServerConnection.create(props);
    try {
      connection.downloadString("/batch/index.txt");
      fail();
    } catch (Exception e) {
      // success
    }
  }
}
