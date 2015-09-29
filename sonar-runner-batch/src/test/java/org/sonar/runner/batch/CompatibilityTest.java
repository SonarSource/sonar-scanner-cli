/*
 * SonarQube Runner - Batch
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import static org.assertj.core.api.Assertions.assertThat;

import org.mockito.ArgumentCaptor;
import org.junit.Test;

public class CompatibilityTest {
  @Test
  public void test() {
    IssueListener issueListener = mock(IssueListener.class);

    org.sonar.batch.bootstrapper.IssueListener.Issue issue = new org.sonar.batch.bootstrapper.IssueListener.Issue();
    setIssue(issue);

    org.sonar.batch.bootstrapper.IssueListener adaptedIssueListener = Compatibility.getBatchIssueListener(issueListener);

    adaptedIssueListener.handle(issue);

    ArgumentCaptor<IssueListener.Issue> arg = ArgumentCaptor.forClass(IssueListener.Issue.class);
    verify(issueListener).handle(arg.capture());
    assertIssue(arg.getValue());
  }

  private static void setIssue(org.sonar.batch.bootstrapper.IssueListener.Issue issue) {
    issue.setAssigneeName("name");
    issue.setRuleName("rule");
    issue.setRuleKey("key");
    issue.setMessage("msg");
    issue.setAssigneeLogin("login");
    issue.setLine(10);
    issue.setComponentKey("component");
    issue.setSeverity("severity");
    issue.setNew(true);
    issue.setStatus("status");
  }

  private static void assertIssue(IssueListener.Issue issue) {
    assertThat(issue.getAssigneeName()).isEqualTo("name");
    assertThat(issue.getRuleName()).isEqualTo("rule");
    assertThat(issue.getRuleKey()).isEqualTo("key");
    assertThat(issue.getMessage()).isEqualTo("msg");
    assertThat(issue.getAssigneeLogin()).isEqualTo("login");
    assertThat(issue.getLine()).isEqualTo(10);
    assertThat(issue.getComponentKey()).isEqualTo("component");
    assertThat(issue.getSeverity()).isEqualTo("severity");
    assertThat(issue.isNew()).isEqualTo(true);
    assertThat(issue.getStatus()).isEqualTo("status");
  }
}
