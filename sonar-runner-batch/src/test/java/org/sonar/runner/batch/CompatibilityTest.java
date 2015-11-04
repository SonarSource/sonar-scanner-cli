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

import static org.mockito.Mockito.times;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CompatibilityTest {
  private IssueListener issueListener;

  @Before
  public void setUp() {
    issueListener = mock(IssueListener.class);
  }

  @Test
  public void test() {
    org.sonar.batch.bootstrapper.IssueListener.Issue batchIssue = new org.sonar.batch.bootstrapper.IssueListener.Issue();
    setIssue(batchIssue);

    org.sonar.batch.bootstrapper.IssueListener adaptedIssueListener = Compatibility.getBatchIssueListener(issueListener, false);

    adaptedIssueListener.handle(batchIssue);

    ArgumentCaptor<IssueListener.Issue> arg = ArgumentCaptor.forClass(IssueListener.Issue.class);
    verify(issueListener).handle(arg.capture());
    assertIssue(arg.getValue(), false);
  }

  @Test
  public void testPrecise() {
    org.sonar.batch.bootstrapper.IssueListener.Issue batchIssue = new org.sonar.batch.bootstrapper.IssueListener.Issue();
    setIssue(batchIssue);

    org.sonar.batch.bootstrapper.IssueListener adaptedIssueListener = Compatibility.getBatchIssueListener(issueListener, true);

    adaptedIssueListener.handle(batchIssue);

    ArgumentCaptor<IssueListener.Issue> arg = ArgumentCaptor.forClass(IssueListener.Issue.class);
    verify(issueListener).handle(arg.capture());
    assertIssue(arg.getValue(), true);
  }

  @Test
  public void preciseIssueLocationCompatibility() {
    org.sonar.batch.bootstrapper.IssueListener.Issue batchIssue = mock(org.sonar.batch.bootstrapper.IssueListener.Issue.class);

    org.sonar.batch.bootstrapper.IssueListener adaptedIssueListener = Compatibility.getBatchIssueListener(issueListener, false);
    adaptedIssueListener.handle(batchIssue);

    verify(batchIssue, times(0)).getEndLine();
    verify(batchIssue, times(0)).getStartLine();
    verify(batchIssue, times(0)).getStartLineOffset();
    verify(batchIssue, times(0)).getEndLineOffset();
  }

  private static void setIssue(org.sonar.batch.bootstrapper.IssueListener.Issue issue) {
    issue.setAssigneeName("name");
    issue.setRuleName("rule");
    issue.setRuleKey("key");
    issue.setMessage("msg");
    issue.setAssigneeLogin("login");
    issue.setLine(10);
    issue.setStartLine(5);
    issue.setEndLine(6);
    issue.setStartLineOffset(1);
    issue.setEndLineOffset(2);
    issue.setComponentKey("component");
    issue.setSeverity("severity");
    issue.setNew(true);
    issue.setStatus("status");
  }

  private static void assertIssue(IssueListener.Issue issue, boolean precise) {
    assertThat(issue.getAssigneeName()).isEqualTo("name");
    assertThat(issue.getRuleName()).isEqualTo("rule");
    assertThat(issue.getRuleKey()).isEqualTo("key");
    assertThat(issue.getMessage()).isEqualTo("msg");
    assertThat(issue.getAssigneeLogin()).isEqualTo("login");
    assertThat(issue.getComponentKey()).isEqualTo("component");
    assertThat(issue.getSeverity()).isEqualTo("severity");
    assertThat(issue.isNew()).isEqualTo(true);
    assertThat(issue.getStatus()).isEqualTo("status");

    if (precise) {
      assertThat(issue.getStartLine()).isEqualTo(5);
      assertThat(issue.getEndLine()).isEqualTo(6);
      assertThat(issue.getStartLineOffset()).isEqualTo(1);
      assertThat(issue.getEndLineOffset()).isEqualTo(2);
    } else {
      assertThat(issue.getStartLine()).isEqualTo(10);
      assertThat(issue.getEndLine()).isEqualTo(10);
      assertThat(issue.getStartLineOffset()).isEqualTo(null);
      assertThat(issue.getEndLineOffset()).isEqualTo(null);
    }
  }
}
