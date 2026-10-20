rootProject.name = "sonar-scanner-cli-gradle-conventions"

apply(from = "./repositories.settings.gradle.kts")

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}
