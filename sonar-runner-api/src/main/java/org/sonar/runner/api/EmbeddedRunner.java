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

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.util.Locale;
import java.util.Properties;

import javax.annotation.Nullable;

import org.sonar.home.cache.Logger;
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
  private final Logger logger;

  EmbeddedRunner(IsolatedLauncherFactory bl, Logger logger, LogOutput logOutput) {
    this.logger = logger;
    this.launcherFactory = bl;
    this.logOutput = logOutput;
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

  /**
   * Declare Sonar properties, for example sonar.projectKey=>foo.
   *
   * @see #setProperty(String, String)
   */
  public EmbeddedRunner addGlobalProperties(Properties p) {
    globalProperties.putAll(p);
    return this;
  }

  /**
   * Declare a SonarQube property.
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

  public String appVersion() {
    return globalProperty(InternalProperties.RUNNER_APP_VERSION, null);
  }

  public void runAnalysis(Properties analysisProperties) {
    runAnalysis(analysisProperties, null);
  }

  public void runAnalysis(Properties analysisProperties, @Nullable IssueListener issueListener) {
    Properties copy = new Properties();
    copy.putAll(analysisProperties);
    initAnalysisProperties(copy);

    String dumpToFile = copy.getProperty(InternalProperties.RUNNER_DUMP_TO_FILE);
    if (dumpToFile != null) {
      File dumpFile = new File(dumpToFile);
      Utils.writeProperties(dumpFile, copy);
      logger.info("Simulation mode. Configuration written to " + dumpFile.getAbsolutePath());
    } else {
      doExecute(copy, issueListener);
    }
  }

  public void start() {
    initGlobalDefaultValues();
    doStart();
  }

  public void stop() {
    doStop();
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

  protected void doStart() {
    launcher = launcherFactory.createLauncher(globalProperties());
    if (VersionUtils.isAtLeast52(launcher.getVersion())) {
      launcher.start(globalProperties(), new org.sonar.runner.batch.LogOutput() {

        @Override
        public void log(String formattedMessage, Level level) {
          logOutput.log(formattedMessage, LogOutput.Level.valueOf(level.name()));
        }

      });
    }
  }

  protected void doStop() {
    if (VersionUtils.isAtLeast52(launcher.getVersion())) {
      launcher.stop();
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
      launcher.executeOldVersion(prop);
    }
  }

  static class IssueListenerAdapter implements org.sonar.runner.batch.IssueListener {
    private IssueListener apiIssueListener;

    public IssueListenerAdapter(IssueListener apiIssueListener) {
      this.apiIssueListener = apiIssueListener;
    }

    @Override
    public void handle(org.sonar.runner.batch.IssueListener.Issue issue) {
      apiIssueListener.handle(transformIssue(issue));
    }

    private static org.sonar.runner.api.Issue transformIssue(org.sonar.runner.batch.IssueListener.Issue batchIssue) {
      org.sonar.runner.api.Issue.Builder issueBuilder = org.sonar.runner.api.Issue.builder();

      issueBuilder.setAssignee(batchIssue.getAssignee());
      issueBuilder.setComponentKey(batchIssue.getComponentKey());
      issueBuilder.setKey(batchIssue.getKey());
      issueBuilder.setLine(batchIssue.getLine());
      issueBuilder.setMessage(batchIssue.getMessage());
      issueBuilder.setNew(batchIssue.isNew());
      issueBuilder.setResolution(batchIssue.getResolution());
      issueBuilder.setRule(batchIssue.getRule());
      issueBuilder.setStatus(batchIssue.getStatus());

      return issueBuilder.build();
    }
  }

  private static class LoggerAdapter implements Logger {
    private LogOutput logOutput;

    LoggerAdapter(LogOutput logOutput) {
      this.logOutput = logOutput;
    }

    @Override
    public void warn(String msg) {
      logOutput.log(msg, LogOutput.Level.WARN);
    }

    @Override
    public void info(String msg) {
      logOutput.log(msg, LogOutput.Level.INFO);
    }

    @Override
    public void error(String msg, Throwable t) {
      StringWriter errors = new StringWriter();
      t.printStackTrace(new PrintWriter(errors));
      logOutput.log(msg + "\n" + errors.toString(), LogOutput.Level.ERROR);
    }

    @Override
    public void error(String msg) {
      logOutput.log(msg, LogOutput.Level.ERROR);
    }

    @Override
    public void debug(String msg) {
      logOutput.log(msg, LogOutput.Level.DEBUG);
    }
  };
}
