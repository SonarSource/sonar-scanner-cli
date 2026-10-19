import org.sonarsource.scanner.cli.gradle.ArtifactoryConfiguration

plugins {
  id("com.jfrog.artifactory")
}

// this value is present on CI
val buildNumber: String? = System.getProperty("buildNumber")
val buildName: String? = System.getenv("BUILD_NAME")
val projectVersion = project.version as String

val artifactoryConfiguration = extensions.create<ArtifactoryConfiguration>("artifactoryConfiguration")

// `afterEvaluate` is required to inject configurable properties; see https://github.com/jfrog/artifactory-gradle-plugin/issues/71#issuecomment-1734977528
project.afterEvaluate {
  val isCommercial = project.group.toString().startsWith("com.")
  val repoKeyEnv = if (isCommercial) "ARTIFACTORY_DEPLOY_REPO_PRIVATE" else "ARTIFACTORY_DEPLOY_REPO"
  artifactory {
    setContextUrl(System.getenv("ARTIFACTORY_URL"))
    // Note: `publish` should only be called once: https://github.com/jfrog/artifactory-gradle-plugin/issues/111
    publish {
      repository {
        repoKey = System.getenv(repoKeyEnv)
        username = System.getenv("ARTIFACTORY_DEPLOY_USERNAME")
        password = System.getenv("ARTIFACTORY_DEPLOY_ACCESS_TOKEN")
      }
      defaults {
        publications("mavenJava")
        setProperties(
          mapOf(
            "build.name" to buildName,
            "version" to projectVersion,
            "build.number" to buildNumber,
            "pr.branch.target" to System.getenv("PULL_REQUEST_BRANCH_TARGET"),
            "pr.number" to System.getenv("PULL_REQUEST_NUMBER"),
            "vcs.branch" to System.getenv("GIT_BRANCH"),
            "vcs.revision" to System.getenv("GIT_COMMIT")
          )
        )
        setPublishArtifacts(true)
        setPublishPom(true)
        setPublishIvy(false)
      }
    }

    clientConfig.info.addEnvironmentProperty("PROJECT_VERSION", projectVersion)
    // Read by gh-action_release to select the artifacts published to binaries.sonarsource.com
    if (artifactoryConfiguration.artifactsToPublish.isPresent) {
      clientConfig.info.addEnvironmentProperty("ARTIFACTS_TO_PUBLISH", artifactoryConfiguration.artifactsToPublish.get())
    }
    clientConfig.info.buildName = buildName
    clientConfig.info.buildNumber = buildNumber
    clientConfig.isIncludeEnvVars = true
    clientConfig.envVarsExcludePatterns =
      "*password*,*PASSWORD*,*secret*,*MAVEN_CMD_LINE_ARGS*,sun.java.command," +
        "*token*,*TOKEN*,*LOGIN*,*login*,*key*,*KEY*,*PASSPHRASE*,*signing*"
  }
}
