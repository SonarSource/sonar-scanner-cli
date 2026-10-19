# Claude Commands for SonarScanner CLI

## Update JREs

Help updating all bundled JREs to the latest available JRE 21 version, or to a specific version, by automatically querying the Adoptium API.

**Command:** `/update-jres`

**Usage:**
```
/update-jres           # Uses latest version (default)
/update-jres latest    # Same as above
/update-jres 21.0.5+11 # Uses specific version
```

**What it does:**
1. **Get the JRE release**: Queries Adoptium API to get the latest JRE 21 release name, or uses the given version
2. **Updates the release name**: Updates the `jreReleaseName` property in `gradle.properties`
3. **Updates the JREs metadata**: Runs `./gradlew updateJresMetadata` to regenerate `gradle/jres-metadata.json` (file names and SHA256 hashes of each platform)
4. **Validates**: Runs `./gradlew clean build` to download the JREs and verify their checksums
