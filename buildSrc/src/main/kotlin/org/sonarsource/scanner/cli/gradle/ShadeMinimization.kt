package org.sonarsource.scanner.cli.gradle

import java.io.BufferedReader
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.vafer.jdependency.Clazz
import org.vafer.jdependency.Clazzpath

/**
 * Computes the classes of the dependencies that are not used by the project, like the `minimizeJar` option of the Maven
 * Shade plugin (ported from its `MinijarFilter`), and writes their paths, one per line, to [removableClasses].
 *
 * The classes kept are those used by the project, those of the [keptDependencies] and the implementations of the services
 * used, with everything they use. The `minimize()` option of Shadow keeps fewer classes: the classes of the dependencies
 * it doesn't minimize are not taken into account to find the used classes.
 */
@CacheableTask
abstract class ShadeMinimization : DefaultTask() {

  /** The JAR of the project classes */
  @get:Classpath
  abstract val projectJar: ConfigurableFileCollection

  /** The JARs of the dependencies of the project */
  @get:Classpath
  abstract val dependencies: ConfigurableFileCollection

  /** The JARs of the dependencies to keep whole, typically because their classes are loaded by reflection */
  @get:Classpath
  abstract val keptDependencies: ConfigurableFileCollection

  @get:OutputFile
  abstract val removableClasses: RegularFileProperty

  @TaskAction
  fun compute() {
    val cp = Clazzpath()
    val projectUnit = cp.addClazzpathUnit(projectJar.singleFile)
    dependencies.forEach { cp.addClazzpathUnit(it) }

    val removable = cp.clazzes.toMutableSet()
    removable.remove(Clazz("module-info"))
    keepPackageInfos(removable, projectUnit.clazzes + projectUnit.transitiveDependencies)
    removable.removeAll(projectUnit.clazzes)
    removable.removeAll(projectUnit.transitiveDependencies)

    keptDependencies.forEach { jar ->
      val jarClazzes = Clazzpath().addClazzpathUnit(jar).clazzes
      removable.toSet().filter { it in jarClazzes }.forEach { keep(removable, it) }
    }

    keepServices(cp, removable)

    removableClasses.get().asFile.writeText(
      removable.map { it.name.replace('.', '/') + ".class" }.sorted().joinToString("\n", postfix = "\n")
    )
  }

  private fun keepPackageInfos(removable: MutableSet<Clazz>, clazzes: Set<Clazz>) {
    val packageNames = HashSet<String>()
    for (clazz in clazzes) {
      var name = clazz.name
      while (name.contains('.')) {
        name = name.substringBeforeLast('.')
        if (packageNames.add(name)) {
          removable.remove(Clazz("$name.package-info"))
        }
      }
    }
  }

  /** Keeps the implementations of the services whose interface is kept, until no more are found */
  private fun keepServices(cp: Clazzpath, removable: MutableSet<Clazz>) {
    do {
      val needed = cp.clazzes - removable
      var repeatScan = false
      // The project classes are never removed, so only the services of the dependencies matter
      for (jar in dependencies) {
        var jarRepeatScan = false
        ZipFile(jar).use { zip ->
          zip.entries().asSequence()
            .filter { !it.isDirectory && it.name.startsWith(SERVICES_PATH) }
            .filter { cp.getClazz(it.name.removePrefix(SERVICES_PATH)) in needed }
            .forEach { entry ->
              zip.getInputStream(entry).bufferedReader(StandardCharsets.UTF_8).use {
                // Only the last service file of a JAR counts, like in the Maven Shade plugin
                jarRepeatScan = keepServiceProviders(cp, removable, it)
              }
            }
        }
        repeatScan = repeatScan || jarRepeatScan
      }
    } while (repeatScan)
  }

  private fun keepServiceProviders(cp: Clazzpath, removable: MutableSet<Clazz>, reader: BufferedReader): Boolean {
    var found = false
    reader.lineSequence()
      .map { it.split("#", limit = 2)[0].trim() }
      .filter { it.isNotEmpty() }
      .mapNotNull { cp.getClazz(it) }
      .filter { it in removable }
      .forEach {
        keep(removable, it)
        found = true
      }
    return found
  }

  private fun keep(removable: MutableSet<Clazz>, clazz: Clazz) {
    removable.remove(clazz)
    removable.removeAll(clazz.transitiveDependencies)
  }

  private companion object {
    const val SERVICES_PATH = "META-INF/services/"
  }
}

/** @return the paths of the classes listed in a file written by [ShadeMinimization] */
fun readRemovableClasses(file: File): Set<String> = file.readLines().filter { it.isNotEmpty() }.toSet()
