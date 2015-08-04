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

      newIssue.setAssigneeLogin(batchIssue.getAssigneeLogin());
      newIssue.setAssigneeName(batchIssue.getAssigneeName());
      newIssue.setComponentKey(batchIssue.getComponentKey());
      newIssue.setKey(batchIssue.getKey());
      newIssue.setResolution(batchIssue.getResolution());
      newIssue.setRuleKey(batchIssue.getRuleKey());
      newIssue.setRuleName(batchIssue.getRuleName());
      newIssue.setMessage(batchIssue.getMessage());
      newIssue.setNew(batchIssue.isNew());
      newIssue.setLine(batchIssue.getLine());
      newIssue.setSeverity(batchIssue.getSeverity());
      newIssue.setStatus(batchIssue.getStatus());

      return newIssue;
    }
  }
}
