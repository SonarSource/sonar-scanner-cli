/*
 * Sonar Runner - API
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
package org.sonar.runner;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BootstrapperTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void shouldRemoveLastUrlSlash() {
    Bootstrapper bootstrapper = new Bootstrapper("", "http://test/", new File("target/tmp"), null);
    assertThat(bootstrapper.getServerUrl()).isEqualTo("http://test");
  }

  @Test(expected = Exception.class)
  public void shouldFailIfCanNotConnectServer() {
    Bootstrapper bootstrapper = new Bootstrapper("", "http://unknown.foo", new File("target/tmp"), null);
    bootstrapper.getServerVersion();
  }

  @Test
  public void shouldReturnUserAgent() {
    Bootstrapper bootstrapper = new Bootstrapper("test/0.1", "http://unknown.foo", new File("target/tmp"), null);
    String userAgent = bootstrapper.getUserAgent();

    assertThat(userAgent.length()).isGreaterThan(0);
    assertThat(userAgent).startsWith("sonar-bootstrapper/");
    assertThat(userAgent).endsWith(" test/0.1");
  }

  @Test
  public void shouldReturnValidVersion() {
    Bootstrapper bootstrapper = new Bootstrapper("", "http://test", new File("target/tmp"), null) {
      @Override
      String remoteContent(String path) throws IOException {
        return "2.6";
      }
    };
    assertThat(bootstrapper.getServerVersion()).isEqualTo("2.6");
  }

  @Test
  public void shouldParseEncodingFromContentType() {
    assertThat(Bootstrapper.getCharsetFromContentType("text/html; charset=EUC-JP")).isEqualTo("EUC-JP");
    assertThat(Bootstrapper.getCharsetFromContentType("text/html")).isNull();
  }

  @Test
  public void shouldCheckVersionForCache() {
    assertThat(Bootstrapper.isUnsupportedVersionForCache("1.0")).isTrue();
    assertThat(Bootstrapper.isUnsupportedVersionForCache("2.0")).isTrue();
    assertThat(Bootstrapper.isUnsupportedVersionForCache("2.1")).isTrue();
    assertThat(Bootstrapper.isUnsupportedVersionForCache("2.2")).isTrue();
    assertThat(Bootstrapper.isUnsupportedVersionForCache("2.3")).isTrue();
    assertThat(Bootstrapper.isUnsupportedVersionForCache("2.4")).isTrue();
    assertThat(Bootstrapper.isUnsupportedVersionForCache("2.4.1")).isTrue();
    assertThat(Bootstrapper.isUnsupportedVersionForCache("2.5")).isTrue();
    assertThat(Bootstrapper.isUnsupportedVersionForCache("2.11")).isTrue();
    assertThat(Bootstrapper.isUnsupportedVersionForCache("3.0")).isTrue();
    assertThat(Bootstrapper.isUnsupportedVersionForCache("3.1")).isTrue();
    assertThat(Bootstrapper.isUnsupportedVersionForCache("3.2")).isTrue();
    assertThat(Bootstrapper.isUnsupportedVersionForCache("3.3")).isTrue();
    assertThat(Bootstrapper.isUnsupportedVersionForCache("3.4")).isTrue();
    assertThat(Bootstrapper.isUnsupportedVersionForCache("3.5")).isFalse();
  }

  @Test
  public void shouldCacheWhenNecessary() throws Exception {
    File cacheLocation = tempFolder.newFolder();
    final MockedConnectionFactory connections = new MockedConnectionFactory("http://test");
    connections.register("/api/server/version", "3.5");
    connections.register("/batch_bootstrap/index", "foo.jar|922afef30ca31573d7131347d01b76c4\nbar.jar|69155f65900fbabbf21e28abb33dd06a");
    connections.register("/batch/foo.jar", "fakecontent1");
    connections.register("/batch/bar.jar", "fakecontent2");
    Bootstrapper bootstrapper = new Bootstrapper("", "http://test", new File("target/tmp"), cacheLocation) {
      @Override
      HttpURLConnection newHttpConnection(URL url) throws IOException {
        return connections.get(url);
      }
    };
    bootstrapper.createClassLoader(new URL[] {}, this.getClass().getClassLoader());
    assertThat(new File(new File(cacheLocation, "922afef30ca31573d7131347d01b76c4"), "foo.jar")).exists();
    assertThat(new File(new File(cacheLocation, "69155f65900fbabbf21e28abb33dd06a"), "bar.jar")).exists();

    // Should not download during the second execution
    final MockedConnectionFactory connections2 = new MockedConnectionFactory("http://test");
    connections2.register("/api/server/version", "3.5");
    connections2.register("/batch_bootstrap/index", "foo.jar|922afef30ca31573d7131347d01b76c4\nbar.jar|69155f65900fbabbf21e28abb33dd06a");
    Bootstrapper bootstrapper2 = new Bootstrapper("", "http://test", new File("target/tmp"), cacheLocation) {
      @Override
      HttpURLConnection newHttpConnection(URL url) throws IOException {
        return connections2.get(url);
      }
    };
    bootstrapper2.createClassLoader(new URL[] {}, this.getClass().getClassLoader());
  }

  @Test
  public void shouldDownloadFromOldURL() throws Exception {
    File cacheLocation = tempFolder.newFolder();
    final MockedConnectionFactory connections = new MockedConnectionFactory("http://test");
    connections.register("/api/server/version", "3.4");
    connections.register("/batch/", "foo.jar,bar.jar");
    connections.register("/batch/foo.jar", "fakecontent1");
    connections.register("/batch/bar.jar", "fakecontent2");
    Bootstrapper bootstrapper = new Bootstrapper("", "http://test", new File("target/tmp"), cacheLocation) {
      @Override
      HttpURLConnection newHttpConnection(URL url) throws IOException {
        return connections.get(url);
      }
    };
    bootstrapper.createClassLoader(new URL[] {}, this.getClass().getClassLoader());
    verify(connections.get("/batch/foo.jar")).getInputStream();
    verify(connections.get("/batch/bar.jar")).getInputStream();
  }

  private class MockedConnectionFactory {
    private Map<URL, HttpURLConnection> mockedConnections = new HashMap<URL, HttpURLConnection>();
    private String serverUrl;

    public MockedConnectionFactory(String serverUrl) {
      this.serverUrl = serverUrl;
    }

    public void register(String path, String content) throws Exception {
      HttpURLConnection mockConnection = mock(HttpURLConnection.class);
      when(mockConnection.getInputStream()).thenReturn(IOUtils.toInputStream(content));
      when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
      mockedConnections.put(new URL(serverUrl + path), mockConnection);
    }

    public HttpURLConnection get(URL url) {
      return mockedConnections.get(url);
    }

    public HttpURLConnection get(String path) throws MalformedURLException {
      return mockedConnections.get(new URL(serverUrl + path));
    }

  }
}
