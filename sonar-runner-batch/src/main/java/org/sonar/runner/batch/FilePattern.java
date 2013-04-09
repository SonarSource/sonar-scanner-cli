/*
 * Sonar Runner - Batch
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
package org.sonar.runner.batch;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.WildcardPattern;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class FilePattern {

  Collection<File> listFiles(File basedir, String pattern) {
    File absoluteBasedir = absoluteBasedir(pattern);
    IOFileFilter filter;
    if (absoluteBasedir != null) {
      filter = new AbsolutePathFilter(WildcardPattern.create(pattern, "/"));
      basedir = absoluteBasedir.isFile() ? absoluteBasedir.getParentFile() : absoluteBasedir;
    } else {
      filter = new RelativePathFilter(basedir, WildcardPattern.create(pattern, "/"));
    }
    if (basedir.isDirectory() && basedir.exists()) {
      return FileUtils.listFiles(basedir, new AndFileFilter(FileFileFilter.FILE, filter), TrueFileFilter.TRUE);
    }
    return Collections.emptyList();
  }


  private File absoluteBasedir(String pattern) {
    File absoluteBasedir = null;
    int wildcard = StringUtils.indexOfAny(pattern, new char[]{'*', '?'});

    if (wildcard == 0) {
      // relative path

    } else if (wildcard == -1) {
      absoluteBasedir = new File(pattern);

    } else {
      int lastSlashBeforeWildcard = pattern.substring(0, wildcard).lastIndexOf("/");
      if (lastSlashBeforeWildcard >= 0) {
        String path = pattern.substring(0, lastSlashBeforeWildcard);
        absoluteBasedir = new File(path);
      }
    }
    if (absoluteBasedir != null && !absoluteBasedir.isAbsolute()) {
      absoluteBasedir = null;
    }
    return absoluteBasedir;
  }

  private static class RelativePathFilter implements IOFileFilter {
    private final File basedir;
    private final WildcardPattern pattern;

    private RelativePathFilter(File basedir, WildcardPattern pattern) {
      this.basedir = basedir;
      this.pattern = pattern;
    }

    public boolean accept(File file) {
      return pattern.match(relativePath(file));
    }

    public boolean accept(File file, String filename) {
      return true;
    }

    String relativePath(File file) {
      List<String> stack = Lists.newArrayList();
      String path = FilenameUtils.normalize(file.getAbsolutePath());
      File cursor = new File(path);
      while (cursor != null) {
        if (containsFile(cursor)) {
          return Joiner.on("/").join(stack);
        }
        stack.add(0, cursor.getName());
        cursor = cursor.getParentFile();
      }
      return null;
    }

    private boolean containsFile(File cursor) {
      return FilenameUtils.equalsNormalizedOnSystem(basedir.getAbsolutePath(), cursor.getAbsolutePath());
    }

  }

  private static class AbsolutePathFilter implements IOFileFilter {
    private final WildcardPattern pattern;

    private AbsolutePathFilter(WildcardPattern pattern) {
      this.pattern = pattern;
    }

    public boolean accept(File file) {
      return pattern.match(FilenameUtils.separatorsToUnix(file.getAbsolutePath()));
    }

    public boolean accept(File file, String filename) {
      return true;
    }
  }
}
