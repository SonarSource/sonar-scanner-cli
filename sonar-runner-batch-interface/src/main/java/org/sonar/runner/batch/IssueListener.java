/*
 * SonarQube Runner - Batch Interface
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
package org.sonar.runner.batch;

public interface IssueListener {
  void handle(Issue issue);

  class Issue {
    private String key;
    private String componentKey;
    private Integer line;
    private String message;
    private String rule;
    private String status;
    private String resolution;
    private boolean isNew;
    private String assignee;

    public void setKey(String key) {
      this.key = key;
    }

    public void setComponentKey(String componentKey) {
      this.componentKey = componentKey;
    }

    public void setLine(Integer line) {
      this.line = line;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public void setRule(String rule) {
      this.rule = rule;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public void setResolution(String resolution) {
      this.resolution = resolution;
    }

    public void setNew(boolean isNew) {
      this.isNew = isNew;
    }

    public void setAssignee(String assignee) {
      this.assignee = assignee;
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
}
