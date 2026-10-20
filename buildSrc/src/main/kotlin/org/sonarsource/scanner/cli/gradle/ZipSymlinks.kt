package org.sonarsource.scanner.cli.gradle

import java.io.BufferedInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.UnixStat
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

/**
 * Gradle archive tasks follow symbolic links: the files they point to are stored instead of the links. This restores the
 * symbolic links of a tarball in a zip built from its content.
 */
object ZipSymlinks {

  /**
   * Replaces the entries of [zip] that are symbolic links in [tarball] by symbolic link entries.
   *
   * @param sourceDir the directory of the tarball that was copied into the zip, e.g. `jdk-21.0.12.1+1-jre`
   * @param targetDir the directory of the zip it was copied into, e.g. `sonar-scanner-8.3-linux-x64/jre`
   * @return the number of restored symbolic links
   */
  fun restore(zip: File, tarball: File, sourceDir: String, targetDir: String): Int {
    val links = readSymlinks(tarball, "${sourceDir.trimEnd('/')}/")
      .mapKeys { (path, _) -> "${targetDir.trimEnd('/')}/$path" }
    if (links.isEmpty()) {
      return 0
    }
    val tmp = File(zip.parentFile, "${zip.name}.tmp")
    var restored = 0
    ZipFile.builder().setFile(zip).get().use { input ->
      ZipArchiveOutputStream(tmp).use { output ->
        for (entry in input.entries) {
          val target = links[entry.name]
          if (target == null) {
            output.addRawArchiveEntry(entry, input.getRawInputStream(entry))
          } else {
            val link = ZipArchiveEntry(entry.name)
            link.unixMode = UnixStat.LINK_FLAG or UnixStat.DEFAULT_LINK_PERM
            link.time = entry.time
            output.putArchiveEntry(link)
            output.write(target.toByteArray(StandardCharsets.UTF_8))
            output.closeArchiveEntry()
            restored++
          }
        }
      }
    }
    Files.move(tmp.toPath(), zip.toPath(), StandardCopyOption.REPLACE_EXISTING)
    return restored
  }

  /** @return the targets of the symbolic links under [prefix], by path relative to [prefix] */
  private fun readSymlinks(tarball: File, prefix: String): Map<String, String> {
    if (!tarball.name.endsWith(".tar.gz")) {
      return emptyMap()
    }
    val links = LinkedHashMap<String, String>()
    TarArchiveInputStream(GzipCompressorInputStream(BufferedInputStream(tarball.inputStream()))).use { tar ->
      var entry = tar.nextEntry
      while (entry != null) {
        val name = entry.name.removePrefix("./")
        if (entry.isSymbolicLink && name.startsWith(prefix)) {
          links[name.removePrefix(prefix)] = entry.linkName
        }
        entry = tar.nextEntry
      }
    }
    return links
  }
}
