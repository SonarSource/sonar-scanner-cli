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
package org.sonar.runner;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bootstrapper used to download everything from the server and create the correct classloader required to execute a Sonar analysis in isolation.
 */
class Bootstrapper {

  static final String VERSION_PATH = "/api/server/version";
  static final String BATCH_PATH = "/batch/";
  static final String BOOTSTRAP_INDEX_PATH = "/batch_bootstrap/index";
  static final int CONNECT_TIMEOUT_MILLISECONDS = 30000;
  static final int READ_TIMEOUT_MILLISECONDS = 60000;
  private static final Pattern CHARSET_PATTERN = Pattern.compile("(?i)\\bcharset=\\s*\"?([^\\s;\"]*)");

  private static final String[] UNSUPPORTED_VERSIONS_FOR_CACHE = {"1", "2", "3.0", "3.1", "3.2", "3.3", "3.4"};

  private File bootDir;
  private String serverUrl;
  private String productToken;
  private String serverVersion;
  private SonarCache cache;

  /**
   * @param productToken part of User-Agent request-header field - see http://tools.ietf.org/html/rfc1945#section-10.15
   */
  Bootstrapper(String productToken, String serverUrl, File workDir, File cacheLocation) {
    this.productToken = productToken;
    this.cache = SonarCache.create().setCacheLocation(cacheLocation).build();
    bootDir = new File(workDir, "batch");
    bootDir.mkdirs();
    if (serverUrl.endsWith("/")) {
      this.serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
    } else {
      this.serverUrl = serverUrl;
    }
  }

  /**
   * @return server url
   */
  String getServerUrl() {
    return serverUrl;
  }

  /**
   * @return server version
   */
  String getServerVersion() {
    if (serverVersion == null) {
      try {
        serverVersion = remoteContent(VERSION_PATH);
      } catch (ConnectException e) {
        Logs.error("Sonar server '" + serverUrl + "' can not be reached");
        throw new RunnerException("Fail to request server version", e);
      } catch (UnknownHostException e) {
        Logs.error("Sonar server '" + serverUrl + "' can not be reached");
        throw new RunnerException("Fail to request server version", e);
      } catch (IOException e) {
        throw new RunnerException("Fail to request server version", e);
      }
    }
    return serverVersion;
  }

  /**
   * Download batch files from server and creates {@link BootstrapClassLoader}.
   * To use this method version of Sonar should be at least 2.6.
   *
   * @param urls             additional URLs for loading classes and resources
   * @param parent           parent ClassLoader
   * @param unmaskedPackages only classes and resources from those packages would be available for loading from parent
   */
  BootstrapClassLoader createClassLoader(URL[] urls, ClassLoader parent, String... unmaskedPackages) {
    BootstrapClassLoader classLoader = new BootstrapClassLoader(parent, unmaskedPackages);
    List<File> files = downloadBatchFiles();
    for (URL url : urls) {
      classLoader.addURL(url);
    }
    for (File file : files) {
      try {
        classLoader.addURL(file.toURI().toURL());
      } catch (MalformedURLException e) {
        throw new IllegalStateException("Fail to create classloader", e);
      }
    }
    return classLoader;
  }

  private void remoteContentToFile(String path, File toFile) {
    InputStream input = null;
    FileOutputStream output = null;
    String fullUrl = serverUrl + path;
    if (Logs.isDebugEnabled()) {
      Logs.debug("Downloading " + fullUrl + " to " + toFile.getAbsolutePath());
    }
    // Don't log for old versions without cache to not pollute logs
    else if (!isUnsupportedVersionForCache(getServerVersion())) {
      Logs.info("Downloading " + path.substring(path.lastIndexOf("/") + 1));
    }
    try {
      HttpURLConnection connection = newHttpConnection(new URL(fullUrl));
      output = new FileOutputStream(toFile, false);
      input = connection.getInputStream();
      IOUtils.copyLarge(input, output);
    } catch (IOException e) {
      IOUtils.closeQuietly(output);
      FileUtils.deleteQuietly(toFile);
      throw new IllegalStateException("Fail to download the file: " + fullUrl, e);
    } finally {
      IOUtils.closeQuietly(input);
      IOUtils.closeQuietly(output);
    }
  }

  String remoteContent(String path) throws IOException {
    String fullUrl = serverUrl + path;
    HttpURLConnection conn = newHttpConnection(new URL(fullUrl));
    String charset = getCharsetFromContentType(conn.getContentType());
    if (charset == null || "".equals(charset)) {
      charset = "UTF-8";
    }
    Reader reader = new InputStreamReader(conn.getInputStream(), charset);
    try {
      int statusCode = conn.getResponseCode();
      if (statusCode != HttpURLConnection.HTTP_OK) {
        throw new IOException("Status returned by url : '" + fullUrl + "' is invalid : " + statusCode);
      }
      return IOUtils.toString(reader);
    } finally {
      IOUtils.closeQuietly(reader);
      conn.disconnect();
    }
  }

  /**
   * By convention, the product tokens are listed in order of their significance for identifying the application.
   */
  String getUserAgent() {
    return "sonar-bootstrapper/" + Version.getVersion() + " " + productToken;
  }

  HttpURLConnection newHttpConnection(URL url) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setConnectTimeout(CONNECT_TIMEOUT_MILLISECONDS);
    connection.setReadTimeout(READ_TIMEOUT_MILLISECONDS);
    connection.setInstanceFollowRedirects(true);
    connection.setRequestMethod("GET");
    connection.setRequestProperty("User-Agent", getUserAgent());
    return connection;
  }

  private List<File> downloadBatchFiles() {
    try {
      List<File> files = new ArrayList<File>();
      if (isUnsupportedVersionForCache(getServerVersion())) {
        getBootstrapFilesFromOldURL(files);
      }
      else {
        getBootstrapFiles(files);
      }
      return files;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to download libraries from server", e);
    }
  }

  private void getBootstrapFilesFromOldURL(List<File> files) throws IOException {
    String libs = remoteContent(BATCH_PATH);
    for (String lib : libs.split(",")) {
      File file = new File(bootDir, lib);
      remoteContentToFile(BATCH_PATH + lib, file);
      files.add(file);
    }
  }

  private void getBootstrapFiles(List<File> files) throws IOException {
    String libs = remoteContent(BOOTSTRAP_INDEX_PATH);
    String[] lines = libs.split("[\r\n]+");
    for (String line : lines) {
      line = line.trim();
      if ("".equals(line)) {
        continue;
      }
      String[] libAndMd5 = line.split("\\|");
      String libName = libAndMd5[0];
      String remoteMd5 = libAndMd5.length > 0 ? libAndMd5[1] : null;
      Logs.debug("Looking if library " + libName + " with md5 " + remoteMd5 + " is already in cache");
      File libInCache = cache.getFileFromCache(libName, remoteMd5);
      if (libInCache != null) {
        Logs.debug("File is already cached at location " + libInCache.getAbsolutePath());
      }
      else {
        Logs.debug("File is not cached");
        File tmpLocation = cache.getTemporaryFile();
        remoteContentToFile(BATCH_PATH + libName, tmpLocation);
        Logs.debug("Trying to cache file");
        String md5 = cache.cacheFile(tmpLocation, libName);
        libInCache = cache.getFileFromCache(libName, md5);
        if (!md5.equals(remoteMd5)) {
          Logs.warn("INVALID CHECKSUM: File " + libInCache.getAbsolutePath() + " was expected to have checksum " + remoteMd5 + " but was cached with checksum " + md5);
        }
        Logs.debug("File cached at location " + libInCache.getAbsolutePath());
      }
      files.add(libInCache);
    }
  }

  static boolean isUnsupportedVersionForCache(String version) {
    return VersionUtils.isUnsupportedVersion(version, UNSUPPORTED_VERSIONS_FOR_CACHE);
  }

  /**
   * Parse out a charset from a content type header.
   *
   * @param contentType e.g. "text/html; charset=EUC-JP"
   * @return "EUC-JP", or null if not found. Charset is trimmed and uppercased.
   */
  static String getCharsetFromContentType(String contentType) {
    if (contentType == null) {
      return null;
    }

    Matcher m = CHARSET_PATTERN.matcher(contentType);
    if (m.find()) {
      return m.group(1).trim().toUpperCase();
    }
    return null;
  }
}
