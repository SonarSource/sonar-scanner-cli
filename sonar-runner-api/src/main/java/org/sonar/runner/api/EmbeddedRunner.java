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
package org.sonar.runner.api;

import org.sonar.runner.cache.Logger;

import org.sonar.runner.impl.ClassloadRules;

import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonar.runner.batch.IsolatedLauncher;
import org.sonar.runner.impl.InternalProperties;
import org.sonar.runner.impl.IsolatedLauncherFactory;
import org.sonar.runner.impl.VersionUtils;

/**
 * Entry point to run SonarQube analysis programmatically.
 * @since 2.2
 */
public class EmbeddedRunner {
  private final IsolatedLauncherFactory launcherFactory;
  private IsolatedLauncher launcher;
  private final LogOutput logOutput;
  private final Properties globalProperties = new Properties();
  private final List<Object> extensions = new ArrayList<>();
  private final Logger logger;
  private final Set<String> classloaderMask = new HashSet<>();
  private final Set<String> classloaderUnmask = new HashSet<>();

  EmbeddedRunner(IsolatedLauncherFactory bl, Logger logger, LogOutput logOutput) {
    this.logger = logger;
    this.launcherFactory = bl;
    this.logOutput = logOutput;
    this.classloaderUnmask.add("org.sonar.runner.batch.");
  }

  public static EmbeddedRunner create(final LogOutput logOutput) {
    Logger logger = new LoggerAdapter(logOutput);
    return new EmbeddedRunner(new IsolatedLauncherFactory(logger), logger, logOutput);
  }

  public Properties globalProperties() {
    Properties clone = new Properties();
    clone.putAll(globalProperties);
    return clone;
  }

  public EmbeddedRunner unmask(String fqcnPrefix) {
    checkLauncherDoesntExist();
    classloaderUnmask.add(fqcnPrefix);
    return this;
  }

  public EmbeddedRunner mask(String fqcnPrefix) {
    checkLauncherDoesntExist();
    classloaderMask.add(fqcnPrefix);
    return this;
  }

  /**
   * Declare Sonar properties, for example sonar.projectKey=>foo.
   * These might be used at different stages (on {@link #start() or #runAnalysis(Properties)}, depending on the 
   * property and SQ version.
   *
   * @see #setProperty(String, String)
   */
  public EmbeddedRunner addGlobalProperties(Properties p) {
    globalProperties.putAll(p);
    return this;
  }

  /**
   * Declare a SonarQube property.
   * These might be used at different stages (on {@link #start() or #runAnalysis(Properties)}, depending on the 
   * property and SQ version.
   *
   * @see RunnerProperties
   * @see ScanProperties
   */
  public EmbeddedRunner setGlobalProperty(String key, String value) {
    globalProperties.setProperty(key, value);
    return this;
  }

  public String globalProperty(String key, @Nullable String defaultValue) {
    return globalProperties.getProperty(key, defaultValue);
  }

  /**
   * User-agent used in the HTTP requests to the SonarQube server
   */
  public EmbeddedRunner setApp(String app, String version) {
    setGlobalProperty(InternalProperties.RUNNER_APP, app);
    setGlobalProperty(InternalProperties.RUNNER_APP_VERSION, version);
    return this;
  }

  public String app() {
    return globalProperty(InternalProperties.RUNNER_APP, null);
  }

  /**
   * Add extensions to the batch's object container.
   * Only supported until SQ 5.1. For more recent versions, an exception is thrown 
   * @param objs
   */
  public EmbeddedRunner addExtensions(Object... objs) {
    checkLauncherExists();
    if (VersionUtils.isAtLeast52(launcher.getVersion())) {
      throw new IllegalStateException("not supported in current SonarQube version: " + launcher.getVersion());
    }

    extensions.addAll(Arrays.asList(objs));
    return this;
  }

  public String appVersion() {
    return globalProperty(InternalProperties.RUNNER_APP_VERSION, null);
  }

  /**
   * Launch an analysis. 
   * Runner must have been started - see {@link #start()}.
   */
  public void runAnalysis(Properties analysisProperties) {
    runAnalysis(analysisProperties, null);
  }

  /**
   * Launch an analysis, providing optionally a issue listener.
   * Runner must have been started - see {@link #start()}.
   * Issue listener is supported starting in SQ 5.2. If a non-null listener is given for older versions, an exception is thrown
   */
  public void runAnalysis(Properties analysisProperties, @Nullable IssueListener issueListener) {
    checkLauncherExists();
    Properties copy = new Properties();
    copy.putAll(analysisProperties);
    initAnalysisProperties(copy);
    doExecute(copy, issueListener);
  }

  /**
   * Synchronizes the project's data in the local cache with the server, allowing analysis of the project to be done offline.
   * Runner must have been started - see {@link #start()}.
   * Only supported starting in SQ 5.2. For older versions, an exception is thrown
   */
  public void syncProject(String projectKey) {
    checkLauncherExists();
    if (!VersionUtils.isAtLeast52(launcher.getVersion())) {
      throw new IllegalStateException("not supported in current SonarQube version: " + launcher.getVersion());
    }
    launcher.syncProject(projectKey);
  }

  public void start() {
    start(false);
  }

  public void start(boolean preferCache) {
    initGlobalDefaultValues();
    doStart(preferCache);
  }

  /**
   * Stops the batch.
   * Only supported starting in SQ 5.2. For older versions, this is a no-op.
   */
  public void stop() {
    checkLauncherExists();
    doStop();
  }

  public String serverVersion() {
    checkLauncherExists();
    return launcher.getVersion();
  }

  /**
   * @deprecated since 2.5 use {@link #start()}, {@link #runAnalysis(Properties)} and then {@link #stop()}
   */
  @Deprecated
  public final void execute() {
    start();
    runAnalysis(new Properties());
    stop();
  }

  private void initGlobalDefaultValues() {
    setGlobalDefaultValue(RunnerProperties.HOST_URL, "http://localhost:9000");
    setGlobalDefaultValue(InternalProperties.RUNNER_APP, "SonarQubeRunner");
    setGlobalDefaultValue(InternalProperties.RUNNER_APP_VERSION, RunnerVersion.version());
  }

  private void initAnalysisProperties(Properties p) {
    initSourceEncoding(p);
    new Dirs(logger).init(p);
  }

  void initSourceEncoding(Properties p) {
    boolean onProject = Utils.taskRequiresProject(p);
    if (onProject) {
      String sourceEncoding = p.getProperty(ScanProperties.PROJECT_SOURCE_ENCODING, "");
      boolean platformDependent = false;
      if ("".equals(sourceEncoding)) {
        sourceEncoding = Charset.defaultCharset().name();
        platformDependent = true;
        p.setProperty(ScanProperties.PROJECT_SOURCE_ENCODING, sourceEncoding);
      }
      logger.info("Default locale: \"" + Locale.getDefault() + "\", source code encoding: \"" + sourceEncoding + "\""
        + (platformDependent ? " (analysis is platform dependent)" : ""));
    }
  }

  private void setGlobalDefaultValue(String key, String value) {
    if (!globalProperties.containsKey(key)) {
      setGlobalProperty(key, value);
    }
  }

  protected void doStart(boolean preferCache) {
    checkLauncherDoesntExist();
    ClassloadRules rules = new ClassloadRules(classloaderMask, classloaderUnmask);
    launcher = launcherFactory.createLauncher(globalProperties(), rules, preferCache);
    if (VersionUtils.isAtLeast52(launcher.getVersion())) {
      launcher.start(globalProperties(), new org.sonar.runner.batch.LogOutput() {

        @Override
        public void log(String formattedMessage, Level level) {
          logOutput.log(formattedMessage, LogOutput.Level.valueOf(level.name()));
        }

      }, preferCache);
    }
  }

  protected void doStop() {
    if (VersionUtils.isAtLeast52(launcher.getVersion())) {
      launcher.stop();
      launcher = null;
    }
  }

  protected void doExecute(Properties analysisProperties, @Nullable IssueListener issueListener) {
    if (VersionUtils.isAtLeast52(launcher.getVersion())) {
      if (issueListener != null) {
        launcher.execute(analysisProperties, new IssueListenerAdapter(issueListener));
      } else {
        launcher.execute(analysisProperties);
      }
    } else {
      if (issueListener != null) {
        throw new InvalidParameterException("Issue listeners not supported in current version: " + launcher.getVersion());
      }
      Properties prop = new Properties();
      prop.putAll(globalProperties());
      prop.putAll(analysisProperties);
      launcher.executeOldVersion(prop, extensions);
    }
  }

  private void checkLauncherExists() {
    if (launcher == null) {
      throw new IllegalStateException("not started");
    }
  }

  private void checkLauncherDoesntExist() {
    if (launcher != null) {
      throw new IllegalStateException("already started");
    }
  }
}
