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
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProxyTest extends ScannerTestCase {

  private static final String PROXY_USER = "scott";
  private static final String PROXY_PASSWORD = "tiger";

  private static final String SERVER_KEYSTORE = "/ProxyTest/server.p12";
  private static final String SERVER_KEYSTORE_PASSWORD = "pwdServerP12";
  private static final String KEYSTORE_CLIENT_WITH_CA = "/ProxyTest/client-with-ca-keytool.p12";
  private static final String KEYSTORE_CLIENT_WITH_CA_PASSWORD = "pwdClientCAP12";

  private ProxyServer proxyServer;
  private static final WireMockServer httpsReverseProxy = new WireMockServer(WireMockConfiguration.wireMockConfig()
    .dynamicHttpsPort()
    .keystorePath(getResourcePath(SERVER_KEYSTORE).toString())
    .keystorePassword(SERVER_KEYSTORE_PASSWORD)
    .keyManagerPassword(SERVER_KEYSTORE_PASSWORD)
    .keystoreType("PKCS12"));

  /**
   * Starts a WireMock HTTPS server that transparently forwards all traffic to the Orchestrator
   * SonarQube instance. Used as the HTTPS target in proxy-CONNECT tests.
   */
  @BeforeClass
  public static void startHttpsReverseProxy() {
    httpsReverseProxy.start();

    httpsReverseProxy.stubFor(com.github.tomakehurst.wiremock.client.WireMock.any(
      com.github.tomakehurst.wiremock.client.WireMock.anyUrl())
      .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
        .proxiedFrom(orchestrator.getServer().getUrl())));
  }

  @AfterClass
  public static void stopHttpsReverseProxy() {
    httpsReverseProxy.stop();
  }

  @After
  public void stopProxy() throws Exception {
    if (proxyServer != null) {
      proxyServer.stop();
    }
  }

  @Test
  public void analysis_without_proxy_configured_should_not_hit_proxy() throws Exception {
    proxyServer = ProxyServer.start();

    SonarScanner build = newScannerWithAdminCredentials(new File("projects/simple-js"))
      .setProjectKey("no-proxy-test");
    BuildResult result = orchestrator.executeBuildQuietly(build);
    assertThat(result.getLastStatus()).isZero();
    assertThat(proxyServer.getRequestsSeenByProxy()).isEmpty();
  }

  @Test
  public void analysis_with_proxy_not_requesting_authentication_should_succeed() throws Exception {
    proxyServer = ProxyServer.start();

    // Scan with old-style JVM proxy properties passed via SONAR_SCANNER_OPTS
    // (http.nonProxyHosts is cleared so that localhost is routed through the proxy)
    SonarScanner build = newScannerWithAdminCredentials(new File("projects/simple-js"))
      .setProjectKey("proxy-no-auth-test")
      .setEnvironmentVariable("SONAR_SCANNER_OPTS",
        "-Dhttp.nonProxyHosts= -Dhttp.proxyHost=localhost -Dhttp.proxyPort=" + proxyServer.getPort());
    BuildResult result = orchestrator.executeBuildQuietly(build);
    assertThat(result.getLastStatus()).isZero();
    assertThat(proxyServer.getRequestsSeenByProxy()).isNotEmpty();
  }

  @Test
  public void analysis_with_proxy_requesting_authentication_should_fail_if_no_credentials_provided() throws Exception {
    proxyServer = ProxyServer.start(PROXY_USER, PROXY_PASSWORD);

    SonarScanner build = newScan("proxy-http-auth-fail-test", false);
    BuildResult result = orchestrator.executeBuildQuietly(build);
    assertThat(result.getLastStatus()).isNotZero();
    assertThat(result.getLogs()).containsPattern(
      "Failed to query server version: GET http://(.*)/api/server/version failed with HTTP 407 Proxy Authentication Required.");
    assertThat(proxyServer.getRequestsSeenByProxy()).isEmpty();
  }

  @Test
  public void analysis_with_proxy_requesting_authentication_should_succeed_if_credentials_provided() throws Exception {
    proxyServer = ProxyServer.start(PROXY_USER, PROXY_PASSWORD);

    SonarScanner build = newScan("proxy-http-auth-success-test", false)
      .setProperty("sonar.scanner.proxyUser", PROXY_USER)
      .setProperty("sonar.scanner.proxyPassword", PROXY_PASSWORD);
    BuildResult result = orchestrator.executeBuildQuietly(build);
    assertThat(result.getLastStatus()).isZero();
    assertThat(proxyServer.getRequestsSeenByProxy()).isNotEmpty();
  }

  @Test
  public void analysis_with_proxy_requesting_authentication_and_https_server_should_fail_if_no_credentials_provided() throws Exception {
    proxyServer = ProxyServer.start(PROXY_USER, PROXY_PASSWORD);

    Path clientTruststore = getResourcePath(KEYSTORE_CLIENT_WITH_CA);
    assertThat(clientTruststore).exists();

    SonarScanner build = newScan("proxy-https-auth-fail-test", true);

    BuildResult result = orchestrator.executeBuildQuietly(build);
    assertThat(result.getLastStatus()).isNotZero();
    assertThat(result.getLogs()).containsIgnoringCase("Failed to query server version");
    assertThat(proxyServer.getConnectRequestsSeenByProxy()).isEmpty();
  }

  /**
   * Reproduces the regression reported for SonarScanner CLI 8.0 (java-library 4.0):
   * HTTPS proxy authentication was broken — the {@code Proxy-Authorization} header was not sent on
   * the CONNECT tunnel, so the proxy kept returning 407.
   * <p>
   * This test uses a local HTTP forward proxy that enforces authentication on CONNECT requests, plus
   * a local HTTPS reverse-proxy that forwards to the running SonarQube instance. This mirrors the
   * real-world topology: scanner → HTTP proxy (CONNECT) → HTTPS SonarQube.
   * <p>
   * The fix requires two parts: the scanner library sending {@code Proxy-Authorization} preemptively,
   * and the launcher script setting {@code -Djdk.http.auth.tunneling.disabledSchemes=} so the JDK
   * honours Basic auth on CONNECT tunnel requests. This test verifies the end-to-end behaviour of the CLI.
   */
  @Test
  public void analysis_with_proxy_auth_and_https_server_should_succeed() throws Exception {
    proxyServer = ProxyServer.start(PROXY_USER, PROXY_PASSWORD);

    // With proxy credentials the CONNECT tunnel must succeed and the full analysis must pass.
    // This relies on the launcher script having set -Djdk.http.auth.tunneling.disabledSchemes=
    // so that the JDK HttpClient performs Basic auth on CONNECT tunnel requests.
    SonarScanner build = newScan("proxy-https-auth-success-test", true)
      .setProperty("sonar.scanner.proxyUser", PROXY_USER)
      .setProperty("sonar.scanner.proxyPassword", PROXY_PASSWORD);
    BuildResult result = orchestrator.executeBuildQuietly(build);
    assertThat(result.getLastStatus()).isZero();
    assertThat(proxyServer.getConnectRequestsSeenByProxy()).isNotEmpty();
  }

  private SonarScanner newScan(String projectKey, boolean useHttps) {
    Path clientTruststore = getResourcePath(KEYSTORE_CLIENT_WITH_CA);
    assertThat(clientTruststore).exists();

    SonarScanner scan =  newScannerWithAdminCredentials(new File("projects/simple-js"))
      .setProjectKey(projectKey)
      .setEnvironmentVariable("SONAR_SCANNER_OPTS", "-Dhttp.nonProxyHosts=")
      .setProperty("sonar.scanner.proxyHost", "localhost")
      .setProperty("sonar.scanner.proxyPort", "" + proxyServer.getPort())
      .setProperty("sonar.scanner.truststorePath", clientTruststore.toString())
      .setProperty("sonar.scanner.truststorePassword", KEYSTORE_CLIENT_WITH_CA_PASSWORD);
    if (useHttps) {
      scan.setProperty("sonar.host.url", "https://localhost:" + httpsReverseProxy.httpsPort());
    }
    return scan;
  }

  private static Path getResourcePath(String resourceName) {
    try {
      return Paths.get(ProxyTest.class.getResource(resourceName).toURI()).toAbsolutePath();
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }
}
