plugins {
  `kotlin-dsl`
}

// see https://github.com/gradle/gradle/issues/17963
fun Provider<PluginDependency>.gav(): String {
  val t = get()
  val id = t.pluginId
  val version = t.version
  return "$id:$id.gradle.plugin:$version"
}

dependencies {
  implementation(libs.commons.compress)
  implementation(libs.jdependency)
  implementation(libs.plugins.artifactory.gradle.plugin.gav())
  implementation(libs.plugins.shadow.gradle.plugin.gav())
}
