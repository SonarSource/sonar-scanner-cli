package org.sonarsource.scanner.cli.gradle

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.io.File

/**
 * A Temurin JRE embedded in a platform-specific distribution, as described in `gradle/jres-metadata.json`.
 *
 * @property classifier the classifier of the distribution, e.g. `linux-x64`
 * @property os the operating system, as named by the Adoptium API
 * @property arch the architecture, as named by the Adoptium API
 * @property filename the name of the JRE archive
 * @property sha256 the SHA-256 checksum of the JRE archive
 * @property javaHome the path of the Java home inside the JRE archive
 */
data class Jre(
  val classifier: String,
  val os: String,
  val arch: String,
  val filename: String,
  val sha256: String,
  val javaHome: String,
) : java.io.Serializable {

  val isWindows: Boolean get() = os == "windows"

  val isMac: Boolean get() = os == "mac"

  /** Suffix of the task names related to this JRE, e.g. `LinuxX64` */
  val taskSuffix: String get() = classifier.split("-").joinToString("") { it.replaceFirstChar(Char::uppercase) }

  fun toMap(): Map<String, String> = linkedMapOf(
    "classifier" to classifier,
    "os" to os,
    "arch" to arch,
    "filename" to filename,
    "sha256" to sha256,
    "javaHome" to javaHome,
  )

  companion object {
    fun readAll(file: File): List<Jre> {
      @Suppress("UNCHECKED_CAST")
      val entries = JsonSlurper().parse(file) as List<Map<String, String>>
      return entries.map {
        Jre(it.getValue("classifier"), it.getValue("os"), it.getValue("arch"), it.getValue("filename"), it.getValue("sha256"), it.getValue("javaHome"))
      }
    }

    fun writeAll(jres: List<Jre>, file: File) {
      file.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(jres.map { it.toMap() })) + "\n")
    }
  }
}
