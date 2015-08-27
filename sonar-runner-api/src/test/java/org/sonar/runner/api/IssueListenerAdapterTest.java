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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.mockito.ArgumentCaptor;

import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;

public class IssueListenerAdapterTest {
  private IssueListenerAdapter adapter;
  private IssueListener issueListener;

  @Before
  public void setUp() {
    issueListener = mock(IssueListener.class);
    adapter = new IssueListenerAdapter(issueListener);
  }

  @Test
  public void test() {
    org.sonar.runner.batch.IssueListener.Issue issue = new org.sonar.runner.batch.IssueListener.Issue();

    issue.setAssigneeName("dummy");
    adapter.handle(issue);

    ArgumentCaptor<Issue> argument = ArgumentCaptor.forClass(Issue.class);
    verify(issueListener).handle(argument.capture());

    assertThat(argument.getValue().getAssigneeName()).isEqualTo("dummy");
  }
}
