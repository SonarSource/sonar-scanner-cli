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
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.home.cache.Logger;
import org.sonar.home.cache.PersistentCache;
import org.sonar.home.cache.PersistentCacheBuilder;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;

public class ServerConnectionTest {

  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private PersistentCache cache = null;

  @Before
  public void setUp() {
    cache = new PersistentCacheBuilder(mock(Logger.class)).setSonarHome(temp.getRoot().toPath()).build();
  }

  @Test
  public void should_download_to_string() throws Exception {
    httpServer.setMockResponseData("abcde");
    Properties props = new Properties();
    props.setProperty("sonar.host.url", httpServer.url());

    ServerConnection connection = ServerConnection.create(props, cache, mock(Logger.class));
    String response = connection.downloadStringCache("/batch/index.txt");

    assertThat(response).isEqualTo("abcde");
  }

  @Test
  public void should_download_to_file() throws Exception {
    httpServer.setMockResponseData("abcde");
    Properties props = new Properties();
    props.setProperty("sonar.host.url", httpServer.url());

    ServerConnection connection = ServerConnection.create(props, cache, mock(Logger.class));
    File toFile = temp.newFile();
    connection.download("/batch/index.txt", toFile);

    assertThat(FileUtils.readFileToString(toFile)).isEqualTo("abcde");
  }

  @Test
  public void should_cache_jar_list() throws Exception {
    File cacheDir = new File(temp.getRoot(), "ws_cache");
    httpServer.setMockResponseData("abcde");
    Properties props = new Properties();
    props.setProperty("sonar.host.url", httpServer.url() + "/");
    props.setProperty("sonar.analysis.mode", "preview");

    assertThat(cacheDir.list().length).isEqualTo(0);
    ServerConnection connection = ServerConnection.create(props, cache, mock(Logger.class));
    String str = connection.downloadStringCache("/batch/index.txt");

    assertThat(str).isEqualTo("abcde");
    assertThat(cacheDir.list().length).isEqualTo(2);

    httpServer.setMockResponseData("never requested");
    str = connection.downloadStringCache("/batch/index.txt");
    assertThat(str).isEqualTo("abcde");
  }

  @Test
  public void should_not_cache_not_preview() throws Exception {
    File cacheDir = new File(temp.getRoot(), "ws_cache");
    httpServer.setMockResponseData("abcde");
    Properties props = new Properties();
    props.setProperty("sonar.host.url", httpServer.url() + "/");

    assertThat(cacheDir.list().length).isEqualTo(0);
    ServerConnection connection = ServerConnection.create(props, cache, mock(Logger.class));
    String str = connection.downloadStringCache("/batch/index.txt");

    assertThat(str).isEqualTo("abcde");
    assertThat(cacheDir.list().length).isEqualTo(0);

    httpServer.setMockResponseData("request2");
    str = connection.downloadStringCache("/batch/index.txt");
    assertThat(str).isEqualTo("request2");
  }

  // SONARPLUGINS-3061
  @Test
  public void should_support_trailing_slash() throws Exception {
    httpServer.setMockResponseData("abcde");
    Properties props = new Properties();
    props.setProperty("sonar.host.url", httpServer.url() + "/");

    ServerConnection connection = ServerConnection.create(props, cache, mock(Logger.class));
    File toFile = temp.newFile();
    connection.download("/batch/index.txt", toFile);

    assertThat(FileUtils.readFileToString(toFile)).isEqualTo("abcde");
  }

  @Test
  public void should_not_download_file_when_host_is_down() throws Exception {
    Properties props = new Properties();
    props.setProperty("sonar.host.url", "http://localhost:" + NetworkUtil.getNextAvailablePort());

    ServerConnection connection = ServerConnection.create(props, cache, mock(Logger.class));
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

    ServerConnection connection = ServerConnection.create(props, cache, mock(Logger.class));
    try {
      connection.downloadStringCache("/batch/index.txt");
      fail();
    } catch (Exception e) {
      // success
    }
  }
}
