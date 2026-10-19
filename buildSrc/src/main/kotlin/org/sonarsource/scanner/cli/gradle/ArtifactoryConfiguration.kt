package org.sonarsource.scanner.cli.gradle

import org.gradle.api.provider.Property

/**
 * Per-project settings of [scanner.artifactory-conventions].
 */
interface ArtifactoryConfiguration {
  /**
   * Comma-separated list of `groupId:artifactId:extension[:classifier]` coordinates, stored in the build info
   * as the `ARTIFACTS_TO_PUBLISH` environment property.
   */
  val artifactsToPublish: Property<String>
}
