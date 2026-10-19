---
description: Update bundled JREs to latest JRE 21 version
---

Update all bundled JREs in the sonar-scanner-cli project to the latest available JRE 21 version (or a specific version if provided as an argument) by automatically querying the Adoptium API.

## Arguments
The user may provide:
- No argument or `latest` (default) - automatically finds the latest JRE 21 release
- A specific version like `21.0.5+11`

## Your task

Follow these steps:

1. **Determine the release name to use** (e.g. `jdk-21.0.5+11`):
   - If the user provided a specific version (e.g., `21.0.5+11`), the release name is `jdk-<version>`
   - Otherwise, query the Adoptium API for the latest JRE 21 release and read `release_name` from the response:
   ```bash
   curl -X 'GET' 'https://api.adoptium.net/v3/assets/latest/21/hotspot?image_type=jre&vendor=eclipse' -H 'accept: application/json'
   ```

2. **Update `jreReleaseName`** in `gradle.properties` with the release name.

3. **Regenerate the JRE metadata** by running `./gradlew updateJresMetadata`. It queries the Adoptium API for the release
   and writes the file name, SHA-256 checksum and Java home of the JRE of each platform to `gradle/jres-metadata.json`:
   - linux-x64, linux-aarch64, windows-x64, macosx-x64, macosx-aarch64

4. **Handle missing architectures**:
   - If the task fails because an architecture isn't found in the release, the release process might be in progress
   - Suggest staying on the previous version, or waiting for the release to be complete
   - Don't edit `gradle/jres-metadata.json` by hand to mix releases: all the JREs use the same release name

5. **Validate the changes**:
   - Show a summary of what was updated (`git diff gradle.properties gradle/jres-metadata.json`)
   - Run: `./gradlew clean build`
   - This will download the JREs, verify their checksums, and check the size of the distributions
   - Report any errors to the user
