/*
 * SonarSource :: IT :: SonarScanner CLI
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.http.HttpMethod;
import com.sonar.orchestrator.junit4.OrchestratorRule;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarqube.ws.Components.Component;
import org.sonarqube.ws.Measures.Measure;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.components.ShowRequest;
import org.sonarqube.ws.client.measures.ComponentRequest;

import static java.util.Arrays.asList;

public abstract class ScannerTestCase {
  private static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
  private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_FORMAT);

  private static final Logger LOG = LoggerFactory
    .getLogger(ScannerTestCase.class);

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @ClassRule
  public static OrchestratorRule orchestrator = SonarScannerTestSuite.ORCHESTRATOR;

  private static Version artifactVersion;

  private static Version artifactVersion() {
    if (artifactVersion == null) {
      String scannerVersion = System.getProperty("scanner.version");
      if (StringUtils.isNotBlank(scannerVersion)) {
        LOG.info("Use provided Scanner version: {}", scannerVersion);
        artifactVersion = Version.create(scannerVersion);
      } else if (StringUtils.isNotBlank(System.getenv("PROJECT_VERSION"))) {
        scannerVersion = System.getenv("PROJECT_VERSION");
        LOG.info("Use Scanner version from environment: {}", scannerVersion);
        artifactVersion = Version.create(scannerVersion);
      } else {
        try (FileInputStream fis = new FileInputStream(
          new File("../target/maven-archiver/pom.properties"))) {
          Properties props = new Properties();
          props.load(fis);
          artifactVersion = Version.create(props.getProperty("version"));
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
      }
    }
    return artifactVersion;
  }

  @After
  public void resetData() {
    String currentDateTime = ZonedDateTime.now().format(DATETIME_FORMATTER);

    orchestrator.getServer()
      .newHttpCall("/api/projects/bulk_delete")
      .setAdminCredentials()
      .setMethod(HttpMethod.POST)
      .setParams("analyzedBefore", currentDateTime)
      .execute();
  }

  SonarScanner newScannerWithToken(File baseDir, String token, String... keyValueProperties) {
    SonarScanner scannerCli = SonarScanner.create(baseDir, keyValueProperties);
    scannerCli.setScannerVersion(artifactVersion().toString());
    if (orchestrator.getServer().version().isGreaterThanOrEquals(10, 0)) {
      scannerCli.setProperty("sonar.token", token);
    } else {
      // Before SQ 10.0, the token was passed through the login property
      scannerCli.setProperty("sonar.login", token);
    }
    return scannerCli;
  }

  SonarScanner newScannerWithAdminCredentials(File baseDir, String... keyValueProperties) {
    SonarScanner scannerCli = SonarScanner.create(baseDir, keyValueProperties);
    scannerCli.setScannerVersion(artifactVersion().toString());
    scannerCli.setProperty("sonar.login", Server.ADMIN_LOGIN);
    scannerCli.setProperty("sonar.password", Server.ADMIN_PASSWORD);
    return scannerCli;
  }

  static Map<String, Measure> getMeasures(String componentKey, String... metricKeys) {
    return newAdminWsClient().measures().component(new ComponentRequest()
        .setComponent(componentKey)
        .setMetricKeys(asList(metricKeys)))
      .getComponent().getMeasuresList()
      .stream()
      .collect(Collectors.toMap(Measure::getMetric, Function.identity()));
  }

  static Component getComponent(String componentKey) {
    return newAdminWsClient().components()
      .show(new ShowRequest().setComponent(componentKey)).getComponent();
  }

  public static WsClient newAdminWsClient() {
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(orchestrator.getServer().getUrl())
      .credentials(Server.ADMIN_LOGIN, Server.ADMIN_PASSWORD)
      .build());
  }

}
