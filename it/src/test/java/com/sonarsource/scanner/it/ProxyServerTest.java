/*
 * SonarSource :: IT :: SonarScanner CLI
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.sonarsource.scanner.it;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class ProxyServerTest {

  @Parameterized.Parameters(name = "CONNECT={0}")
  public static Object[] protocols() {
    return new Object[] {false, true};
  }

  @Parameterized.Parameter
  public boolean connect;

  private final WireMockServer target = new WireMockServer(wireMockConfig().dynamicPort());
  private ProxyServer proxy;

  @Before
  public void startTarget() {
    target.start();
    target.stubFor(get(urlEqualTo("/forward")).willReturn(aResponse().withBody("forwarded")));
  }

  @After
  public void stopServers() throws Exception {
    try {
      if (proxy != null) {
        proxy.stop();
      }
    } finally {
      target.stop();
    }
  }

  @Test
  public void forwards_without_authentication() throws Exception {
    proxy = ProxyServer.start();

    assertForwarded(null);
  }

  @Test
  public void challenges_missing_credentials() throws Exception {
    proxy = ProxyServer.start("user", "password");

    assertChallenged(null);
  }

  @Test
  public void challenges_incorrect_credentials() throws Exception {
    proxy = ProxyServer.start("user", "password");

    assertChallenged("user:wrong");
  }

  @Test
  public void forwards_with_valid_credentials() throws Exception {
    proxy = ProxyServer.start("user", "password");

    assertForwarded("user:password");
  }

  private void assertForwarded(String credentials) throws Exception {
    try (Socket socket = new Socket("localhost", proxy.getPort())) {
      socket.setSoTimeout(10000);
      BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
      sendProxyRequest(socket, credentials);
      assertThat(readHeaders(reader)).startsWith("HTTP/1.1 200");
      if (connect) {
        // Send HTTP through the established tunnel to verify that bytes flow in both directions.
        write(socket, "GET /forward HTTP/1.1\r\nHost: localhost:" + target.port() + "\r\nConnection: close\r\n\r\n");
        assertThat(readHeaders(reader)).startsWith("HTTP/1.1 200");
      }
      assertThat(reader.lines().reduce("", String::concat)).isEqualTo("forwarded");
    }
    target.verify(1, getRequestedFor(urlEqualTo("/forward")));
    if (connect) {
      assertThat(proxy.getConnectRequestsSeenByProxy()).containsExactly("localhost:" + target.port());
      assertThat(proxy.getRequestsSeenByProxy()).isEmpty();
    } else {
      assertThat(proxy.getRequestsSeenByProxy()).containsExactly("/forward");
      assertThat(proxy.getConnectRequestsSeenByProxy()).isEmpty();
    }
  }

  private void assertChallenged(String credentials) throws Exception {
    try (Socket socket = new Socket("localhost", proxy.getPort())) {
      socket.setSoTimeout(10000);
      sendProxyRequest(socket, credentials);
      BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
      assertThat(readHeaders(reader))
        .startsWith("HTTP/1.1 407")
        .contains("Proxy-Authenticate: Basic realm=\"proxy\"", "Content-Length: 0");
    }
    target.verify(0, getRequestedFor(urlEqualTo("/forward")));
    assertThat(proxy.getRequestsSeenByProxy()).isEmpty();
    assertThat(proxy.getConnectRequestsSeenByProxy()).isEmpty();
  }

  private void sendProxyRequest(Socket socket, String credentials) throws Exception {
    String authority = "localhost:" + target.port();
    String request = connect ? "CONNECT " + authority : "GET http://" + authority + "/forward";
    request += " HTTP/1.1\r\nHost: " + authority + "\r\n";
    if (credentials != null) {
      request += "Proxy-Authorization: Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.ISO_8859_1)) + "\r\n";
    }
    if (!connect) {
      request += "Connection: close\r\n";
    }
    write(socket, request + "\r\n");
  }

  private static void write(Socket socket, String request) throws Exception {
    socket.getOutputStream().write(request.getBytes(StandardCharsets.ISO_8859_1));
    socket.getOutputStream().flush();
  }

  private static String readHeaders(BufferedReader reader) throws Exception {
    StringBuilder headers = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null && !line.isEmpty()) {
      headers.append(line).append('\n');
    }
    return headers.toString();
  }
}
