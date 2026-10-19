dependencyResolutionManagement {
  // The environment variables ARTIFACTORY_USERNAME and ARTIFACTORY_PASSWORD are used in QA
  // On local box, please add artifactoryUsername and artifactoryPassword to ~/.gradle/gradle.properties
  val artifactoryUsername = System.getenv("ARTIFACTORY_USERNAME") ?: providers.gradleProperty("artifactoryUsername").orNull
  val artifactoryPassword = System.getenv("ARTIFACTORY_PASSWORD") ?: providers.gradleProperty("artifactoryPassword").orNull

  repositories {
    maven {
      url = uri("https://repox.jfrog.io/repox/sonarsource/")
      if (!artifactoryUsername.isNullOrEmpty() && !artifactoryPassword.isNullOrEmpty()) {
        credentials {
          username = artifactoryUsername
          password = artifactoryPassword
        }
      }
    }
  }

  pluginManagement {
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
}
