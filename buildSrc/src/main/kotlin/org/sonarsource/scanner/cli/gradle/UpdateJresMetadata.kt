package org.sonarsource.scanner.cli.gradle

import groovy.json.JsonSlurper
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Queries the Adoptium API for the JREs of the given release, and writes their metadata for all the supported platforms.
 * Run it only to upgrade the embedded JREs, and review the generated file before committing it.
 */
@DisableCachingByDefault(because = "Queries a remote API")
abstract class UpdateJresMetadata : DefaultTask() {

  /** The Temurin release name, e.g. `jdk-21.0.12.1+1` */
  @get:Input
  abstract val releaseName: Property<String>

  @get:OutputFile
  abstract val metadataFile: RegularFileProperty

  init {
    // The result depends on the remote API, it can't be up-to-date
    outputs.upToDateWhen { false }
  }

  @TaskAction
  fun update() {
    val release = releaseName.get()
    val url = "https://api.adoptium.net/v3/assets/release_name/eclipse/${URLEncoder.encode(release, StandardCharsets.UTF_8)}" +
      "?heap_size=normal&image_type=jre&project=jdk"
    @Suppress("UNCHECKED_CAST")
    val binaries = (JsonSlurper().parse(URI(url).toURL()) as Map<String, Any>)["binaries"] as List<Map<String, Any>>

    val jres = SUPPORTED_PLATFORMS.map { (classifier, os, arch) ->
      val candidates = binaries.filter { it["os"] == os && it["architecture"] == arch }
      if (candidates.size != 1) {
        throw GradleException("Expected one JRE package for $os $arch in release $release but got ${candidates.size}")
      }
      @Suppress("UNCHECKED_CAST")
      val pkg = candidates.single()["package"] as Map<String, Any>
      val javaHome = "$release-jre" + if (os == "mac") "/Contents/Home" else ""
      Jre(classifier, os, arch, pkg["name"] as String, pkg["checksum"] as String, javaHome)
    }
    Jre.writeAll(jres, metadataFile.get().asFile)
  }

  private companion object {
    val SUPPORTED_PLATFORMS = listOf(
      Triple("linux-x64", "linux", "x64"),
      Triple("linux-aarch64", "linux", "aarch64"),
      Triple("windows-x64", "windows", "x64"),
      Triple("macosx-x64", "mac", "x64"),
      Triple("macosx-aarch64", "mac", "aarch64"),
    )
  }
}
