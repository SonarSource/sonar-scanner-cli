---
description: Update bundled JREs to latest JRE 21 version
---

Update all bundled JREs in the sonar-scanner-cli project to the latest available JRE 21 version (or a specific version if provided as an argument) by automatically querying the Adoptium API.

## Arguments
The user may provide:
- No argument or `latest` (default) - automatically finds the latest JRE 21 version for each architecture
- A specific version like `21.0.5+11`

## Your task

Follow these steps:

1. **Determine the version to use**:
   - If the user provided a specific version (e.g., `21.0.5+11`), use that version
   - Otherwise, query the Adoptium API for the latest JRE 21 release

2. **Fetch JRE metadata from Adoptium API**:

   For **latest** version:
   ```bash
   curl -X 'GET' 'https://api.adoptium.net/v3/assets/latest/21/hotspot?image_type=jre&vendor=eclipse' -H 'accept: application/json'
   ```

   For a **specific version** (URL encode the version):
   ```bash
   curl -X 'GET' 'https://api.adoptium.net/v3/assets/version/<version URL encoded>?image_type=jre&page=0&page_size=10&project=jdk&release_type=ga&semver=false&sort_method=DEFAULT&sort_order=DESC&vendor=eclipse' -H 'accept: application/json'
   ```

3. **Extract information from the API response** for each platform:
   - Linux x64: Look for `os: "linux"` and `architecture: "x64"`
   - Linux aarch64: Look for `os: "linux"` and `architecture: "aarch64"`
   - macOS x64: Look for `os: "mac"` and `architecture: "x64"`
   - macOS aarch64: Look for `os: "mac"` and `architecture: "aarch64"`
   - Windows x64: Look for `os: "windows"` and `architecture: "x64"`

   For each platform, extract:
   - Download URL: `binary.package.link`
   - SHA256 checksum: `binary.package.checksum`
   - Version string: `version.openjdk_version` (e.g., "21.0.5+11")

4. **Handle missing architectures**:
   - If an architecture isn't found in the latest release, the release process might be in progress
   - Suggest staying on the previous version for that architecture
   - Ask the user if they want to proceed with partial update

5. **Update pom.xml**:

   a. **Update version properties** (lines 58-62) - all should use the same JRE directory name:
   ```xml
   <jre.dirname.linux.aarch64>jdk-{version}-jre</jre.dirname.linux.aarch64>
   <jre.dirname.linux.x64>jdk-{version}-jre</jre.dirname.linux.x64>
   <jre.dirname.macosx.aarch64>jdk-{version}-jre/Contents/Home</jre.dirname.macosx.aarch64>
   <jre.dirname.macosx.x64>jdk-{version}-jre/Contents/Home</jre.dirname.macosx.x64>
   <jre.dirname.windows>jdk-{version}-jre</jre.dirname.windows>
   ```

   b. **Update download URLs and checksums** for each profile:
   - `dist-linux-x64` profile (around line 294): Update `<url>` and `<sha256>`
   - `dist-linux-aarch64` profile (around line 367): Update `<url>` and `<sha256>`
   - `dist-windows-x64` profile (around line 440): Update `<url>` and `<sha256>`
   - `dist-macosx-x64` profile (around line 513): Update `<url>` and `<sha256>`
   - `dist-macosx-aarch64` profile (around line 586): Update `<url>` and `<sha256>`

6. **Validate the changes**:
   - Show a summary of what was updated (version, URLs, checksums)
   - Run: `mvn clean verify -Pdist-linux-x64,dist-linux-aarch64,dist-windows-x64,dist-macosx-x64,dist-macosx-aarch64`
   - This will download the JREs and verify the checksums match
   - Report any errors to the user

## Expected file naming patterns
- Linux x64: `OpenJDK21U-jre_x64_linux_hotspot_{version}.tar.gz`
- Linux aarch64: `OpenJDK21U-jre_aarch64_linux_hotspot_{version}.tar.gz`
- macOS x64: `OpenJDK21U-jre_x64_mac_hotspot_{version}.tar.gz`
- macOS aarch64: `OpenJDK21U-jre_aarch64_mac_hotspot_{version}.tar.gz`
- Windows x64: `OpenJDK21U-jre_x64_windows_hotspot_{version}.zip`

Note: The version in the filename uses underscores (e.g., `21.0.5_11`) while the directory name uses plus signs (e.g., `21.0.5+11`).