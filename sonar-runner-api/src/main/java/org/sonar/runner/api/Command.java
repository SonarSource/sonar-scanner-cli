/*
 * Sonar Runner - API
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

import java.io.File;
import java.util.*;

class Command {
  private final String executable;
  private final List<String> arguments;
  private final Map<String, String> env;
  private final File directory;

  private Command(Builder builder) {
    this.executable = builder.executable;
    this.arguments = Collections.unmodifiableList(builder.arguments);
    this.env = Collections.unmodifiableMap(builder.env);
    this.directory = builder.directory;
  }

  File directory() {
    return directory;
  }

  String executable() {
    return executable;
  }

  List<String> arguments() {
    return arguments;
  }

  /**
   * Environment variables that are propagated during command execution.
   *
   * @return a non-null and immutable map of variables
   */
  Map<String, String> envVariables() {
    return env;
  }

  String[] toStrings() {
    String[] strings = new String[1 + arguments.size()];
    strings[0] = executable;
    for (int index = 0; index < arguments.size(); index++) {
      strings[index + 1] = arguments.get(index);
    }
    return strings;
  }

  @Override
  public String toString() {
    return Utils.join(toStrings(), " ");
  }

  static Builder builder() {
    return new Builder();
  }

  static class Builder {
    private String executable;
    private final List<String> arguments = new ArrayList<String>();
    private final Map<String, String> env = new HashMap<String, String>();
    private File directory;

    private Builder() {
    }

    Builder setExecutable(String s) {
      this.executable = s;
      return this;
    }

    Builder addArguments(String... args) {
      return addArguments(Arrays.asList(args));
    }

    Builder addArguments(List<String> args) {
      for (String arg : args) {
        if (arg!=null && !"".equals(arg.trim())) {
          arguments.add(arg);
        }
      }
      return this;
    }

    Builder setEnvVariable(String key, String value) {
      env.put(key, value);
      return this;
    }

    Builder addEnvVariables(Map<String, String> map) {
      env.putAll(map);
      return this;
    }

    Builder setDirectory(File d) {
      this.directory = d;
      return this;
    }

    Command build() {
      if (executable == null) {
        throw new IllegalArgumentException("Command executable is not defined");
      }
      return new Command(this);
    }
  }
}
