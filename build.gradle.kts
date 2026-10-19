import com.github.jk1.license.render.TextReportRenderer
import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.Verify
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.apache.tools.ant.filters.FixCrLfFilter
import org.cyclonedx.model.Component
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.sonarsource.scanner.cli.gradle.Jre
import org.sonarsource.scanner.cli.gradle.MavenShadeServicesTransformer
import org.sonarsource.scanner.cli.gradle.ShadeMinimization
import org.sonarsource.scanner.cli.gradle.readRemovableClasses
import org.sonarsource.scanner.cli.gradle.UpdateJresMetadata
import org.sonarsource.scanner.cli.gradle.ZipSymlinks

plugins {
  java
  jacoco
  `jvm-test-suite`
  `maven-publish`
  // Version provided by buildSrc
  id("com.gradleup.shadow")
  alias(libs.plugins.cyclonedx.gradle.plugin)
  alias(libs.plugins.download.gradle.plugin)
  alias(libs.plugins.license.gradle.plugin)
  alias(libs.plugins.license.report.gradle.plugin)
  alias(libs.plugins.sonar.gradle.plugin)
  id("scanner.artifactory-conventions")
}

dependencies {
  implementation(libs.sonar.scanner.java.library)
  implementation(libs.slf4j.api)
  implementation(libs.logback.classic)
  compileOnly(libs.jsr305)

  testCompileOnly(libs.jsr305)
}

testing {
  suites {
    named<JvmTestSuite>("test") {
      useJUnitJupiter(libs.versions.junit)
      dependencies {
        implementation(libs.assertj.core)
        implementation(libs.mockito.core)
      }
    }

    // The integration tests run the distributions against a SonarQube server started by Orchestrator
    register<JvmTestSuite>("integrationTest") {
      useJUnit(libs.versions.junit4)
      dependencies {
        implementation(libs.orchestrator.junit4)
        implementation(libs.sonar.ws)
        implementation(libs.assertj.core)
        implementation(libs.jetty.server)
        implementation(libs.jetty.servlet)
        implementation(libs.jetty.proxy)
        implementation(libs.wiremock.standalone)
        compileOnly(libs.jsr305)
      }
    }
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.encoding = "UTF-8"
  options.release = 11
}

// Orchestrator requires Java 17
tasks.named<JavaCompile>("compileIntegrationTestJava") {
  options.release = 17
}

tasks.processResources {
  // A local value, as the configuration cache can't serialize references to the build script
  val version = project.version.toString()
  inputs.property("version", version)
  filesMatching("version.txt") {
    filter { it.replace("\${project.version}", version) }
  }
}

// ---------------------------------------------------------------------------------------------------------------------
// Fat JAR
// ---------------------------------------------------------------------------------------------------------------------

tasks.jar {
  manifest {
    attributes("Main-Class" to "org.sonarsource.scanner.cli.Main")
  }
}

val shadeMinimization = tasks.register<ShadeMinimization>("shadeMinimization") {
  description = "Computes the classes of the dependencies to remove from the fat JAR."
  group = BasePlugin.BUILD_GROUP
  projectJar.from(tasks.jar)
  dependencies.from(configurations.runtimeClasspath)
  // Loaded by reflection
  keptDependencies.from(configurations.runtimeClasspath.map { classpath ->
    classpath.filter { it.name.startsWith("logback-classic-") || it.name.startsWith("bcprov-jdk18on-") }
  })
  removableClasses = layout.buildDirectory.file("shade-minimization/removable-classes.txt")
}

tasks.shadowJar {
  manifest {
    // Required for Bouncycastle
    attributes("Multi-Release" to "true")
  }
  // The first file wins, except for the service files which are merged
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  filesMatching("META-INF/services/**") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
  }
  transform(MavenShadeServicesTransformer())
  // Shadow's minimize() keeps fewer classes than the Maven Shade plugin did
  val removableClassesFile = shadeMinimization.flatMap { it.removableClasses }
  inputs.file(removableClassesFile).withPathSensitivity(PathSensitivity.NONE).withPropertyName("removableClasses")
  val removableClasses by lazy { readRemovableClasses(removableClassesFile.get().asFile) }
  exclude { it.path in removableClasses }
  exclude(
    "META-INF/*.SF",
    "META-INF/*.DSA",
    "META-INF/*.RSA",
    "META-INF/LICENSE.txt",
    "META-INF/NOTICE.txt",
    "**/module-info.class",
  )
}

// ---------------------------------------------------------------------------------------------------------------------
// Distributions
// ---------------------------------------------------------------------------------------------------------------------

val assemblyDir = layout.projectDirectory.dir("src/main/assembly")
val DIR_PERMISSIONS = "755"
val jreReleaseName = providers.gradleProperty("jreReleaseName").get()
val jresMetadataFile = layout.projectDirectory.file("gradle/jres-metadata.json")
val jres = Jre.readAll(jresMetadataFile.asFile)

tasks.register<UpdateJresMetadata>("updateJresMetadata") {
  description = "Updates the metadata of the embedded JREs from the Adoptium API, for the release set by the jreReleaseName property."
  group = BasePlugin.BUILD_GROUP
  releaseName = jreReleaseName
  metadataFile = jresMetadataFile
}

fun CopySpec.replaceTokens(useEmbeddedJre: Boolean) {
  val version = project.version.toString()
  filter {
    it.replace("\${project.version}", version)
      .replace("\${use_embedded_jre}", useEmbeddedJre.toString())
  }
}

// Set together: when only the file permissions are set, the directories get the permissions of the file system
fun CopySpec.permissions(fileMode: String) {
  filePermissions { unix(fileMode) }
  dirPermissions { unix(DIR_PERMISSIONS) }
}

fun CopySpec.lineEndings(eol: String) {
  filter<FixCrLfFilter>(
    "eol" to FixCrLfFilter.CrLf.newInstance(eol),
    "eof" to FixCrLfFilter.AddAsisRemove.newInstance("asis"),
    "fixlast" to false,
  )
}

// The name of the fat JAR in the distributions, used by the launcher scripts
fun CopySpec.fatJarName() {
  val fileName = "${project.name}-$version.jar"
  rename { fileName }
}

fun Zip.checkSize(minSize: Long, maxSize: Long) {
  val archive = archiveFile
  doLast {
    val size = archive.get().asFile.length()
    if (size !in minSize..maxSize) {
      throw GradleException("Size of ${archive.get().asFile.name} is $size bytes, it should be between $minSize and $maxSize")
    }
  }
}

val distZip = tasks.register<Zip>("distZip") {
  description = "Assembles the distribution without JRE."
  group = BasePlugin.BUILD_GROUP
  archiveBaseName = "sonar-scanner"
  into("sonar-scanner-$version") {
    from(assemblyDir) {
      include("bin/sonar-scanner.bat", "bin/sonar-scanner-debug.bat")
      replaceTokens(useEmbeddedJre = false)
      lineEndings("crlf")
    }
    from(assemblyDir) {
      include("bin/sonar-scanner", "bin/sonar-scanner-debug")
      replaceTokens(useEmbeddedJre = false)
      lineEndings("lf")
      permissions(fileMode = "755")
    }
    from(assemblyDir) {
      include("conf/**")
      lineEndings("crlf")
      permissions(fileMode = "644")
    }
    into("lib") {
      from(tasks.shadowJar)
      fatJarName()
    }
  }
  checkSize(10_200_000, 11_000_000)
}

val jreDistZips = jres.map { jre ->
  val jreArchive = layout.buildDirectory.file("jres/${jre.filename}")
  val downloadJre = tasks.register<Download>("downloadJre${jre.taskSuffix}") {
    description = "Downloads the JRE for ${jre.classifier}."
    group = BasePlugin.BUILD_GROUP
    src("https://api.adoptium.net/v3/binary/version/${URLEncoder.encode(jreReleaseName, StandardCharsets.UTF_8)}/${jre.os}/${jre.arch}/jre/hotspot/normal/eclipse?project=jdk")
    dest(jreArchive)
    overwrite(false)
  }
  val verifyJre = tasks.register<Verify>("verifyJre${jre.taskSuffix}") {
    description = "Verifies the checksum of the JRE for ${jre.classifier}."
    group = BasePlugin.BUILD_GROUP
    dependsOn(downloadJre)
    src(jreArchive)
    algorithm("SHA-256")
    checksum(jre.sha256)
  }
  val unpackJre = tasks.register<Sync>("unpackJre${jre.taskSuffix}") {
    description = "Unpacks the JRE for ${jre.classifier}."
    group = BasePlugin.BUILD_GROUP
    dependsOn(verifyJre)
    from(if (jre.isWindows) zipTree(jreArchive) else tarTree(jreArchive))
    into(layout.buildDirectory.dir("jres-unpacked/${jre.classifier}"))
  }
  val javaHome = unpackJre.map { it.destinationDir.resolve(jre.javaHome) }

  tasks.register<Zip>("dist${jre.taskSuffix}Zip") {
    description = "Assembles the distribution with the embedded JRE for ${jre.classifier}."
    group = BasePlugin.BUILD_GROUP
    archiveBaseName = "sonar-scanner"
    archiveClassifier = jre.classifier
    val baseDir = "sonar-scanner-$version-${jre.classifier}"
    into(baseDir) {
      into("jre") {
        from(javaHome) {
          // Same file permissions as in the JRE archive
          useFileSystemPermissions()
          dirPermissions { unix(DIR_PERMISSIONS) }
          exclude("bin/**", "man/**")
          when {
            jre.isWindows -> exclude("plugin/**")
            jre.isMac -> exclude("lib/jspawnhelper")
            else -> exclude("lib/jspawnhelper", "lib/jexec", "plugin/**")
          }
        }
        from(javaHome) {
          if (jre.isWindows) {
            include("bin/**")
          } else {
            include("bin/java", "lib/jspawnhelper")
            if (!jre.isMac) {
              include("lib/jexec")
            }
          }
          permissions(fileMode = "755")
        }
      }
      into("lib") {
        from(tasks.shadowJar)
        fatJarName()
        permissions(fileMode = "644")
      }
      from(assemblyDir) {
        include("conf/**")
        lineEndings("lf")
        permissions(fileMode = "644")
      }
      from(assemblyDir) {
        if (jre.isWindows) {
          include("bin/sonar-scanner.bat", "bin/sonar-scanner-debug.bat")
          lineEndings("crlf")
        } else {
          include("bin/sonar-scanner", "bin/sonar-scanner-debug")
          lineEndings("lf")
        }
        replaceTokens(useEmbeddedJre = true)
        permissions(fileMode = "755")
      }
    }
    val archive = archiveFile
    val tarball = jreArchive
    val jreJavaHome = jre.javaHome
    doLast {
      ZipSymlinks.restore(archive.get().asFile, tarball.get().asFile, jreJavaHome, "$baseDir/jre")
    }
    // Should be big enough to confirm the JRE was bundled
    checkSize(50_000_000, 70_000_000)
  }
}

tasks.assemble {
  dependsOn(distZip, jreDistZips)
}

// ---------------------------------------------------------------------------------------------------------------------
// SBOM
// ---------------------------------------------------------------------------------------------------------------------

tasks.cyclonedxDirectBom {
  projectType = Component.Type.APPLICATION
  // The dependencies shipped in the distributions
  includeConfigs = listOf("runtimeClasspath")
  xmlOutput.convention(null as RegularFile?)
}

// ---------------------------------------------------------------------------------------------------------------------
// Publication
// ---------------------------------------------------------------------------------------------------------------------

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      // The fat JAR is the main artifact
      artifact(tasks.shadowJar) {
        classifier = null
      }
      artifact(distZip)
      jreDistZips.forEach { artifact(it) }
      artifact(tasks.cyclonedxDirectBom.flatMap { it.jsonOutput }) {
        classifier = "cyclonedx"
        extension = "json"
      }
      pom {
        // Otherwise "pom", as the fat JAR and the distribution without JRE both have no classifier
        packaging = "jar"
      }
    }
  }
}

artifactoryConfiguration {
  artifactsToPublish = listOf("zip", "zip:linux-x64", "zip:linux-aarch64", "zip:windows-x64", "zip:macosx-x64", "zip:macosx-aarch64", "json:cyclonedx")
    .joinToString(",") { "${project.group}:${project.name}:$it" }
}

// ---------------------------------------------------------------------------------------------------------------------
// Integration tests
// ---------------------------------------------------------------------------------------------------------------------

// Set on CI, where the integration tests run against the distributions deployed to Artifactory by the build job:
// Orchestrator downloads them, as they are not in its Maven local repository
val deployedScannerVersion = providers.environmentVariable("PROJECT_VERSION")
val integrationTestScannerVersion = deployedScannerVersion.getOrElse(version.toString())

// A Maven local repository with the distributions, where Orchestrator looks for them first
val integrationTestMavenRepository = layout.buildDirectory.dir("integrationTest/maven-repo")
val prepareIntegrationTestScanner = tasks.register<Sync>("prepareIntegrationTestScanner") {
  description = "Prepares the distributions tested by the integration tests."
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  into(integrationTestMavenRepository)
  if (!deployedScannerVersion.isPresent) {
    into("${project.group.toString().replace('.', '/')}/${project.name}/$version") {
      from(distZip, jreDistZips)
      rename("sonar-scanner-(.*)", "${project.name}-$1")
    }
  }
}

// The tests analyze the projects of their working directory, and check the working directories (.scannerwork...) that
// the scanner creates in them: a fresh copy keeps src/ clean, and the results of previous runs out of the assertions
val prepareIntegrationTestProjects = tasks.register<Sync>("prepareIntegrationTestProjects") {
  description = "Prepares the projects analyzed by the integration tests."
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  from("src/integrationTest/projects")
  into(layout.buildDirectory.dir("integrationTest/work/projects"))
}

tasks.named<Test>("integrationTest") {
  inputs.files(prepareIntegrationTestProjects).withPropertyName("projects")
  workingDir(layout.buildDirectory.dir("integrationTest/work"))
  filter {
    includeTestsMatching("com.sonarsource.scanner.it.SonarScannerTestSuite")
  }
  // Some tests are asserting on localized messages or dates
  systemProperty("user.language", "en")
  systemProperty("user.country", "US")
  environment("LANGUAGE", "en_US")
  systemProperty("scanner.version", integrationTestScannerVersion)
  providers.systemProperty("sonar.runtimeVersion").orNull?.let { systemProperty("sonar.runtimeVersion", it) }
  inputs.files(prepareIntegrationTestScanner).withPropertyName("scannerDistributions")
  systemProperty("maven.localRepository", integrationTestMavenRepository.get().asFile.absolutePath)
  // The results depend on the SonarQube version, which can be an alias like DEV
  doNotTrackState("The tests run against a SonarQube version that can be an alias, like DEV")
  testLogging {
    events("passed", "skipped", "failed")
    exceptionFormat = TestExceptionFormat.FULL
    showStandardStreams = true
  }
}

// ---------------------------------------------------------------------------------------------------------------------
// Quality
// ---------------------------------------------------------------------------------------------------------------------

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  reports {
    xml.required = true
    csv.required = false
    html.required = false
  }
}

license {
  header = file("HEADER")
  strictCheck = true
  encoding = "UTF-8"
  mapping("java", "SLASHSTAR_STYLE")
  include("**/*.java")
}

licenseReport {
  configurations = arrayOf("runtimeClasspath")
  renderers = arrayOf(TextReportRenderer())
}

sonar {
  properties {
    property("sonar.projectKey", "org.sonarsource.scanner.cli:sonar-scanner-cli")
    property("sonar.projectName", "SonarScanner CLI")
  }
}
