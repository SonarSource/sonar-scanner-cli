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
  private final String message;
  private final String ruleKey;
  private final String ruleName;
  private final String status;
  private final String resolution;
  private final boolean isNew;
  private final String assigneeLogin;
  private final String assigneeName;
  private final String severity;
  private final Integer startLine;
  private final Integer startLineOffset;
  private final Integer endLine;
  private final Integer endLineOffset;

  private Issue(String key, String componentKey, String message, String ruleKey, String ruleName, String status, String resolution, boolean isNew,
    String assigneeLogin, String assigneeName, String severity, Integer startLine, Integer startLineOffset, Integer endLine, Integer endLineOffset) {
    super();
    this.key = key;
    this.componentKey = componentKey;
    this.message = message;
    this.ruleKey = ruleKey;
    this.ruleName = ruleName;
    this.status = status;
    this.resolution = resolution;
    this.isNew = isNew;
    this.assigneeLogin = assigneeLogin;
    this.assigneeName = assigneeName;
    this.severity = severity;
    this.startLine = startLine;
    this.startLineOffset = startLineOffset;
    this.endLine = endLine;
    this.endLineOffset = endLineOffset;
  }

  public static class Builder {
    private String key;
    private String componentKey;
    private String message;
    private String ruleKey;
    private String ruleName;
    private String status;
    private String resolution;
    private boolean isNew;
    private String assigneeLogin;
    private String assigneeName;
    private String severity;
    private Integer startLine;
    private Integer startLineOffset;
    private Integer endLine;
    private Integer endLineOffset;

    public Integer getStartLine() {
      return startLine;
    }

    public Builder setStartLine(Integer startLine) {
      this.startLine = startLine;
      return this;
    }

    public Integer getStartLineOffset() {
      return startLineOffset;
    }

    public Builder setStartLineOffset(Integer startLineOffset) {
      this.startLineOffset = startLineOffset;
      return this;
    }

    public Integer getEndLine() {
      return endLine;
    }

    public Builder setEndLine(Integer endLine) {
      this.endLine = endLine;
      return this;
    }

    public Integer getEndLineOffset() {
      return endLineOffset;
    }

    public Builder setEndLineOffset(Integer endLineOffset) {
      this.endLineOffset = endLineOffset;
      return this;
    }

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

    public String getMessage() {
      return message;
    }

    public Builder setMessage(String message) {
      this.message = message;
      return this;
    }

    public String getRuleKey() {
      return ruleKey;
    }

    public Builder setRuleKey(String ruleKey) {
      this.ruleKey = ruleKey;
      return this;
    }

    public String getRuleName() {
      return ruleKey;
    }

    public Builder setRuleName(String ruleName) {
      this.ruleName = ruleName;
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

    public String getAssigneeLogin() {
      return assigneeLogin;
    }

    public Builder setAssigneeLogin(String assigneeLogin) {
      this.assigneeLogin = assigneeLogin;
      return this;
    }

    public String getAssigneeName() {
      return assigneeName;
    }

    public Builder setAssigneeName(String assigneeName) {
      this.assigneeName = assigneeName;
      return this;
    }

    public String getSeverity() {
      return severity;
    }

    public Builder setSeverity(String severity) {
      this.severity = severity;
      return this;
    }

    public Issue build() {
      return new Issue(key, componentKey, message, ruleKey, ruleName, status, resolution, isNew, assigneeLogin,
        assigneeName, severity, startLine, startLineOffset, endLine, endLineOffset);
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

  public Integer getStartLine() {
    return startLine;
  }

  /**
   * @return <code>null</code> if it isn't supported by the sonar-batch being used (< 5.3).
   */
  public Integer getStartLineOffset() {
    return startLineOffset;
  }

  public Integer getEndLine() {
    return endLine;
  }

  /**
   * @return <code>null</code> if it isn't supported by the sonar-batch being used (< 5.3).
   */
  public Integer getEndLineOffset() {
    return endLineOffset;
  }

  public String getMessage() {
    return message;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  public String getRuleName() {
    return ruleName;
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

  public String getAssigneeLogin() {
    return assigneeLogin;
  }

  public String getAssigneeName() {
    return assigneeName;
  }

  public String getSeverity() {
    return severity;
  }

}
