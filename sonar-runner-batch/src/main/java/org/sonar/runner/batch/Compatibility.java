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

import org.sonar.api.issue.Issue;
import org.sonar.batch.bootstrapper.Batch;
import org.sonar.batch.bootstrapper.LogOutput;

public class Compatibility {
  private Compatibility() {
    // Utility class
  }

  static void setLogOutputFor5dot2(Batch.Builder builder, final org.sonar.runner.batch.LogOutput logOutput) {
    builder.setLogOutput(new LogOutput() {

      @Override
      public void log(String formattedMessage, Level level) {
        logOutput.log(formattedMessage, org.sonar.runner.batch.LogOutput.Level.valueOf(level.name()));
      }

    });
  }

  static org.sonar.batch.bootstrapper.IssueListener getBatchIssueListener(IssueListener listener) {
    return new IssueListenerAdapter(listener);
  }

  static class IssueListenerAdapter implements org.sonar.batch.bootstrapper.IssueListener {
    private IssueListener listener;

    public IssueListenerAdapter(IssueListener listener) {
      this.listener = listener;
    }

    @Override
    public void handle(Issue issue) {
      listener.handle(transformIssue(issue));
    }

    private static IssueListener.Issue transformIssue(Issue batchIssue) {
      IssueListener.Issue newIssue = new IssueListener.Issue();
      newIssue.setAssignee(batchIssue.assignee());
      newIssue.setComponentKey(batchIssue.componentKey());
      newIssue.setKey(batchIssue.key());
      newIssue.setResolution(batchIssue.resolution());
      newIssue.setRule(batchIssue.ruleKey().toString());
      newIssue.setMessage(batchIssue.message());
      newIssue.setNew(batchIssue.isNew());
      newIssue.setLine(batchIssue.line());
      
      return newIssue;
    }
  }
}
