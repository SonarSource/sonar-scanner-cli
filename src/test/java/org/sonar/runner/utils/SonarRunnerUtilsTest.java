/*
 * Sonar Standalone Runner
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
package org.sonar.runner.utils;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.test.TestUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;

public class SonarRunnerUtilsTest {

  @Test
  public void shouldGetList() {
    Properties props = new Properties();

    props.put("prop", "  foo  ,  bar  , \n\ntoto,tutu");
    assertThat(SonarRunnerUtils.getListFromProperty(props, "prop")).containsOnly("foo", "bar", "toto", "tutu");
  }

  @Test
  public void shouldGetListFromFile() throws IOException {
    String filePath = "shouldGetList/foo.properties";
    Properties props = loadPropsFromFile(filePath);

    assertThat(SonarRunnerUtils.getListFromProperty(props, "prop")).containsOnly("foo", "bar", "toto", "tutu");
  }

  private Properties loadPropsFromFile(String filePath) throws FileNotFoundException, IOException {
    Properties props = new Properties();
    FileInputStream fileInputStream = null;
    try {
      fileInputStream = new FileInputStream(TestUtils.getResource(this.getClass(), filePath));
      props.load(fileInputStream);
    } finally {
      IOUtils.closeQuietly(fileInputStream);
    }
    return props;
  }

}
