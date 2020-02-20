/*
 * SonarSource :: IT :: SonarQube Scanner
 * Copyright (C) 2009-2020 SonarSource SA
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

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarqube.ws.Components.Component;
import org.sonarqube.ws.Measures;
import org.sonarqube.ws.Measures.Measure;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.components.ShowRequest;
import org.sonarqube.ws.client.measures.ComponentRequest;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public abstract class ScannerTestCase {

  private static final Logger LOG = LoggerFactory
    .getLogger(ScannerTestCase.class);

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @ClassRule
  public static Orchestrator orchestrator = SonarScannerTestSuite.ORCHESTRATOR;

  private static Version artifactVersion;

  private static Version artifactVersion() {
    if (artifactVersion == null) {
      String scannerVersion = System.getProperty("scanner.version");
      if (StringUtils.isNotBlank(scannerVersion)) {
        LOG.info("Use provided Scanner version: " + scannerVersion);
        artifactVersion = Version.create(scannerVersion);
      } else {
        try (FileInputStream fis = new FileInputStream(
          new File("../target/maven-archiver/pom.properties"))) {
          Properties props = new Properties();
          props.load(fis);
          artifactVersion = Version.create(props.getProperty("version"));
          return artifactVersion;
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
      }
    }
    return artifactVersion;
  }

  SonarScanner newScanner(File baseDir, String... keyValueProperties) {
    SonarScanner scannerCli = SonarScanner.create(baseDir, keyValueProperties);
    scannerCli.setScannerVersion(artifactVersion().toString());
    return scannerCli;
  }

  @CheckForNull
  static Map<String, Measure> getMeasures(String componentKey,
    String... metricKeys) {
    return newWsClient().measures().component(new ComponentRequest()
      .setComponent(componentKey)
      .setMetricKeys(asList(metricKeys)))
      .getComponent().getMeasuresList()
      .stream()
      .collect(Collectors.toMap(Measure::getMetric, Function.identity()));
  }

  @CheckForNull
  static Measure getMeasure(String componentKey, String metricKey) {
    Measures.ComponentWsResponse response = newWsClient().measures()
      .component(new ComponentRequest()
        .setComponent(componentKey)
        .setMetricKeys(singletonList(metricKey)));
    List<Measure> measures = response.getComponent().getMeasuresList();
    return measures.size() == 1 ? measures.get(0) : null;
  }

  @CheckForNull
  static Integer getMeasureAsInteger(String componentKey, String metricKey) {
    Measure measure = getMeasure(componentKey, metricKey);
    return (measure == null) ? null : Integer.parseInt(measure.getValue());
  }

  @CheckForNull
  static Double getMeasureAsDouble(String componentKey, String metricKey) {
    Measure measure = getMeasure(componentKey, metricKey);
    return (measure == null) ? null : Double.parseDouble(measure.getValue());
  }

  @CheckForNull
  static Component getComponent(String componentKey) {
    return newWsClient().components()
      .show(new ShowRequest().setComponent(componentKey)).getComponent();
  }

  static WsClient newWsClient() {
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(orchestrator.getServer().getUrl())
      .build());
  }

}
