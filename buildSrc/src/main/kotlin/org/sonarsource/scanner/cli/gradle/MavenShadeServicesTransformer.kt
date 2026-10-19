package org.sonarsource.scanner.cli.gradle

import com.github.jengelman.gradle.plugins.shadow.transformers.CacheableTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.ResourceTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import java.nio.charset.StandardCharsets
import org.apache.tools.zip.ZipEntry
import org.apache.tools.zip.ZipOutputStream
import org.gradle.api.file.FileTreeElement

/**
 * Merges the `META-INF/services` files like the `ServicesResourceTransformer` of the Maven Shade plugin: unlike the
 * `ServiceFileTransformer` of Shadow, each merged file ends with a line feed.
 */
@CacheableTransformer
class MavenShadeServicesTransformer : ResourceTransformer {

  private val serviceEntries = LinkedHashMap<String, MutableSet<String>>()

  override fun canTransformResource(element: FileTreeElement): Boolean = element.path.startsWith("$SERVICES_PATH/")

  override fun transform(context: TransformerContext) {
    val lines = context.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readLines() }
    serviceEntries.getOrPut(context.path) { LinkedHashSet() }.addAll(lines)
  }

  override fun hasTransformedResource(): Boolean = serviceEntries.isNotEmpty()

  override fun modifyOutputStream(os: ZipOutputStream, preserveFileTimestamps: Boolean) {
    serviceEntries.forEach { (path, lines) ->
      os.putNextEntry(ZipEntry(path))
      os.write((lines.joinToString("\n") + "\n").toByteArray(StandardCharsets.UTF_8))
      os.closeEntry()
    }
  }

  private companion object {
    const val SERVICES_PATH = "META-INF/services"
  }
}
