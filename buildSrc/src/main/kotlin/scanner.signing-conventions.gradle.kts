import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask

plugins {
  signing
}

val ghBranch: String? = System.getenv("GITHUB_REF_NAME")
val isMainBranch = ghBranch == "master" || (ghBranch?.matches(Regex("branch-[\\d.]+")) ?: false)

signing {
  val signingKeyId = providers.gradleProperty("signingKeyId").orNull
  val signingKey = providers.gradleProperty("signingKey").orNull
  val signingPassword = providers.gradleProperty("signingPassword").orNull
  useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
  setRequired({ isMainBranch && gradle.taskGraph.hasTask(":artifactoryPublish") })
}

// Deferred to afterEvaluate so that publications created in afterEvaluate are present when sign() snapshots the container.
afterEvaluate {
  plugins.withId("maven-publish") {
    signing {
      sign(the<PublishingExtension>().publications)
    }
  }
}

tasks.withType<Sign>().configureEach {
  onlyIf {
    val artifactoryTask = tasks.findByName("artifactoryPublish") as? ArtifactoryTask
    isMainBranch &&
      gradle.taskGraph.hasTask(":artifactoryPublish") &&
      artifactoryTask?.skip != true
  }
}
