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

class IssueListenerAdapter implements org.sonar.runner.batch.IssueListener {
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

    issueBuilder.setAssigneeLogin(batchIssue.getAssigneeLogin());
    issueBuilder.setAssigneeName(batchIssue.getAssigneeName());
    issueBuilder.setComponentKey(batchIssue.getComponentKey());
    issueBuilder.setKey(batchIssue.getKey());
    issueBuilder.setLine(batchIssue.getLine());
    issueBuilder.setMessage(batchIssue.getMessage());
    issueBuilder.setNew(batchIssue.isNew());
    issueBuilder.setResolution(batchIssue.getResolution());
    issueBuilder.setRuleKey(batchIssue.getRuleKey());
    issueBuilder.setRuleName(batchIssue.getRuleName());
    issueBuilder.setSeverity(batchIssue.getSeverity());
    issueBuilder.setStatus(batchIssue.getStatus());

    return issueBuilder.build();
  }
}
