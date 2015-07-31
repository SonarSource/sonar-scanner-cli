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

import javax.annotation.concurrent.Immutable;

@Immutable
public final class Issue {
  private final String key;
  private final String componentKey;
  private final Integer line;
  private final String message;
  private final String rule;
  private final String status;
  private final String resolution;
  private final boolean isNew;
  private final String assignee;

  private Issue(String key, String componentKey, Integer line, String message, String rule, String status, String resolution, boolean isNew, String assignee) {
    super();
    this.key = key;
    this.componentKey = componentKey;
    this.line = line;
    this.message = message;
    this.rule = rule;
    this.status = status;
    this.resolution = resolution;
    this.isNew = isNew;
    this.assignee = assignee;
  }

  public static class Builder {
    private String key;
    private String componentKey;
    private Integer line;
    private String message;
    private String rule;
    private String status;
    private String resolution;
    private boolean isNew;
    private String assignee;

    public String getKey() {
      return key;
    }

    public Builder setKey(String key) {
      this.key = key;
      return this;
    }

    public String getComponentKey() {
      return componentKey;
    }

    public Builder setComponentKey(String componentKey) {
      this.componentKey = componentKey;
      return this;
    }

    public Integer getLine() {
      return line;
    }

    public Builder setLine(Integer line) {
      this.line = line;
      return this;
    }

    public String getMessage() {
      return message;
    }

    public Builder setMessage(String message) {
      this.message = message;
      return this;
    }

    public String getRule() {
      return rule;
    }

    public Builder setRule(String rule) {
      this.rule = rule;
      return this;
    }

    public String getStatus() {
      return status;
    }

    public Builder setStatus(String status) {
      this.status = status;
      return this;
    }

    public String getResolution() {
      return resolution;
    }

    public Builder setResolution(String resolution) {
      this.resolution = resolution;
      return this;
    }

    public boolean isNew() {
      return isNew;
    }

    public Builder setNew(boolean isNew) {
      this.isNew = isNew;
      return this;
    }

    public String getAssignee() {
      return assignee;
    }

    public Builder setAssignee(String assignee) {
      this.assignee = assignee;
      return this;
    }

    public Issue build() {
      return new Issue(key, componentKey, line, message, rule, status, resolution, isNew, assignee);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getKey() {
    return key;
  }

  public String getComponentKey() {
    return componentKey;
  }

  public Integer getLine() {
    return line;
  }

  public String getMessage() {
    return message;
  }

  public String getRule() {
    return rule;
  }

  public String getStatus() {
    return status;
  }

  public String getResolution() {
    return resolution;
  }

  public boolean isNew() {
    return isNew;
  }

  public String getAssignee() {
    return assignee;
  }

}
