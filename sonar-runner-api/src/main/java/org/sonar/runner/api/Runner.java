/*
 * SonarQube Runner - API
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
package org.sonar.runner.api;

import org.sonar.runner.impl.InternalProperties;

import javax.annotation.Nullable;

import java.util.Properties;

/**
 * @since 2.2
 */
public abstract class Runner<T extends Runner> {

  private final Properties properties = new Properties();

  protected Runner() {
  }

  public Properties properties() {
    Properties clone = new Properties();
    clone.putAll(properties);
    return clone;
  }

  /**
   * Declare Sonar properties, for example sonar.projectKey=>foo.
   *
   * @see #setProperty(String, String)
   */
  public T addProperties(Properties p) {
    properties.putAll(p);
    return (T) this;
  }

  /**
   * Declare a Sonar property.
   *
   * @see RunnerProperties
   * @see ScanProperties
   */
  public T setProperty(String key, String value) {
    properties.setProperty(key, value);
    return (T) this;
  }

  public String property(String key, @Nullable String defaultValue) {
    return properties.getProperty(key, defaultValue);
  }

  /**
   * User-agent used in the HTTP requests to the Sonar server
   */
  public T setApp(String app, String version) {
    setProperty(InternalProperties.RUNNER_APP, app);
    setProperty(InternalProperties.RUNNER_APP_VERSION, version);
    return (T) this;
  }

  public String app() {
    return property(InternalProperties.RUNNER_APP, null);
  }

  public String appVersion() {
    return property(InternalProperties.RUNNER_APP_VERSION, null);
  }

  public void execute() {
    initDefaultValues();
    new SourceEncoding().init(this);
    new Dirs().init(this);
    doExecute();
  }

  protected abstract void doExecute();

  private void initDefaultValues() {
    setDefaultValue(RunnerProperties.HOST_URL, "http://localhost:9000");
    setDefaultValue(InternalProperties.RUNNER_APP, "SonarRunner");
    setDefaultValue(InternalProperties.RUNNER_APP_VERSION, RunnerVersion.version());
  }

  private void setDefaultValue(String key, String value) {
    if (!properties.containsKey(key)) {
      setProperty(key, value);
    }
  }

}
