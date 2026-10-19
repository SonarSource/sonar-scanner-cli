pluginManagement {
  // Resolves the plugins of the block below. This block is evaluated before the rest of the script, so it has to be
  // self-contained: the pluginManagement repositories of buildSrc/repositories.settings.gradle.kts are applied too
  // late for the plugins of this file.
  // The environment variables ARTIFACTORY_USERNAME and ARTIFACTORY_PASSWORD are used in QA
  // On local box, please add artifactoryUsername and artifactoryPassword to ~/.gradle/gradle.properties
  val artifactoryUsername = System.getenv("ARTIFACTORY_USERNAME") ?: providers.gradleProperty("artifactoryUsername").orNull
  val artifactoryPassword = System.getenv("ARTIFACTORY_PASSWORD") ?: providers.gradleProperty("artifactoryPassword").orNull
  repositories {
    maven {
      url = uri("https://repox.jfrog.io/repox/plugins.gradle.org/")
      if (!artifactoryUsername.isNullOrEmpty() && !artifactoryPassword.isNullOrEmpty()) {
        credentials {
          username = artifactoryUsername
          password = artifactoryPassword
        }
      }
    }
  }
}

rootProject.name = "sonar-scanner-cli"

apply(from = "./buildSrc/repositories.settings.gradle.kts")
