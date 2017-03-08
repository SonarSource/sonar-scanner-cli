/*
 * SonarSource :: IT :: SonarQube Scanner
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package com.sonar.runner.it;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.ResourceLocation;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarqube.ws.WsMeasures.Measure;

import static java.lang.Integer.parseInt;
import static org.assertj.core.api.Assertions.assertThat;

public class DistributionTest extends ScannerTestCase {

  private static final int SCRIPT_TIMEOUT_SECONDS = 15;

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  private static Path sonarScannerPath;

  enum OS {
    LINUX,
    WINDOWS,
    MACOSX
  }

  @BeforeClass
  public static void setUpBeforeClass() throws IOException {
    Path workDir = temp.newFolder().toPath();

    String version = artifactVersion().toString();
    OS os = getOS();
    String zipPath = String.format("../target/sonar-scanner-%s-%s.zip", os.name().toLowerCase(), version);
    File zipFile = new File(zipPath);
    assertThat(zipFile).isFile();

    unzip(zipFile, workDir);

    Path scannerHome = Files.list(workDir).findFirst().get();
    Path javaPath;

    switch (os) {
      case LINUX:
      case MACOSX:
        sonarScannerPath = scannerHome.resolve("bin/sonar-scanner");
        sonarScannerPath.toFile().setExecutable(true);
        javaPath = scannerHome.resolve("lib/jre/bin/java");
        javaPath.toFile().setExecutable(true);
        break;
      case WINDOWS:
        sonarScannerPath = scannerHome.resolve("bin/sonar-scanner.bat");
        javaPath = scannerHome.resolve("lib/jre/bin/java.exe");
        assertThat(javaPath).isRegularFile();
        break;
    }

    assertThat(sonarScannerPath).isExecutable();
  }

  @After
  public void cleanup() {
    orchestrator.resetData();
  }

  @Test
  public void script_should_push_report_to_sonarqube() throws IOException, InterruptedException {
    String projectKey = "java:basedir-with-source";
    orchestrator.getServer().restoreProfile(ResourceLocation.create("/sonar-way-profile.xml"));
    orchestrator.getServer().provisionProject(projectKey, "Basedir with source");
    orchestrator.getServer().associateProjectToQualityProfile(projectKey, "java", "sonar-way");

    File projectDir = new File("projects/basedir-with-source");
    executeShellScript(projectDir, projectKey, orchestrator);

    Map<String, Measure> projectMeasures = getMeasures(projectKey, "files", "ncloc");
    assertThat(parseInt(projectMeasures.get("files").getValue())).isEqualTo(1);
    assertThat(parseInt(projectMeasures.get("ncloc").getValue())).isGreaterThan(1);
  }

  private void executeShellScript(File projectDir, String projectKey, Orchestrator orchestrator) throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder(sonarScannerPath.toString(), "-Dsonar.host.url=" + orchestrator.getServer().getUrl());
    pb.directory(projectDir);
    Process p = pb.start();
    // needed on windows, otherwise process will not exit
    p.getInputStream().close();
    p.waitFor(SCRIPT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(p.exitValue()).isEqualTo(0);

    waitForAnalysisToComplete(projectDir, orchestrator, projectKey);
  }

  private void waitForAnalysisToComplete(File projectDir, Orchestrator orchestrator, String projectKey) {
    // re-run an analysis using orchestrator: when this analysis is finished, the original must have finished too
    SonarScanner build = newScanner(projectDir, "sonar.projectKey", projectKey + "-dummy", "sonar.projectName", "dummy");
    orchestrator.executeBuild(build, true);
  }

  private static void unzip(File zipFile, Path outDir) throws IOException {
    byte[] buffer = new byte[1024];
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
      while (true) {
        ZipEntry entry = zis.getNextEntry();
        if (entry == null) {
          break;
        }

        Path opath = outDir.resolve(entry.getName());
        Files.createDirectories(opath.getParent());

        if (entry.isDirectory()) continue;

        try (FileOutputStream fos = new FileOutputStream(opath.toFile())) {
          int len;
          while ((len = zis.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
          }
        }
      }

      zis.closeEntry();
    }
  }

  private static OS getOS() {
    String osName = System.getProperty("os.name").toLowerCase();
    if (isUnix(osName)) {
      return OS.LINUX;
    }
    if (isWindows(osName)) {
      return OS.WINDOWS;
    }
    if (isMac(osName)) {
      return OS.MACOSX;
    }
    throw new IllegalStateException("Unsupported os: " + osName);
  }

  private static boolean isWindows(String osName) {
    return osName.contains("win");
  }

  private static boolean isMac(String osName) {
    return osName.contains("max");
  }

  private static boolean isUnix(String osName) {
    return osName.contains("nix") || osName.contains("nux");
  }
}
