/*
 * SonarQube Scanner
 * Copyright (C) 2011-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.scanner.cli;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropertyResolver {
  private static final Pattern placeholderPattern = Pattern.compile("\\$\\{([\\w\\.]+)\\}");
  private final Properties props;
  private final Properties resolved;
  private final List<String> queue;
  private Map<String, String> env;

  public PropertyResolver(Properties props, Map<String, String> env) {
    this.props = props;
    this.env = env;
    this.resolved = new Properties();
    this.queue = new LinkedList<>();
  }

  public Properties resolve() {
    for (Map.Entry<Object, Object> e : props.entrySet()) {
      if (resolved.containsKey(e.getKey())) {
        continue;
      }
      resolveProperty((String) e.getKey());
    }

    return resolved;
  }

  private String getValue(String key) {
    String propValue;

    if (key.startsWith("env.")) {
      String envKey = key.substring(4);
      propValue = env.get(envKey);
    } else {
      propValue = props.getProperty(key);
    }

    return propValue != null ? propValue : "";
  }

  private String resolveProperty(String propKey) {
    String propValue = getValue(propKey);
    if (propValue.isEmpty()) {
      resolved.setProperty(propKey, propValue);
      return propValue;
    }

    Matcher m = placeholderPattern.matcher(propValue);
    StringBuffer sb = new StringBuffer();

    while (m.find()) {
      String varName = (null == m.group(1)) ? m.group(2) : m.group(1);
      if (queue.contains(varName)) {
        throw new IllegalArgumentException("Found a loop resolving place holders in properties, for variable: " + varName);
      }

      String placeholderValue = resolveVar(varName);
      m.appendReplacement(sb, Matcher.quoteReplacement(placeholderValue));
    }
    m.appendTail(sb);

    String resolvedPropValue = sb.toString();
    resolved.setProperty(propKey, resolvedPropValue);
    return resolvedPropValue;
  }

  private String resolveVar(String varName) {
    String placeholderValue;
    if (resolved.containsKey(varName)) {
      placeholderValue = resolved.getProperty(varName);
    } else {
      queue.add(varName);
      placeholderValue = resolveProperty(varName);
      queue.remove(varName);
    }
    return placeholderValue;

  }
}
