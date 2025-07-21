# Update JREs Command

Updates all bundled JREs in the sonar-scanner-cli project to the latest available JRE 17 version for each architecture by automatically querying the Adoptium API.

## Arguments
- `latest` (default) - automatically finds the latest JRE 17 version for each architecture
- `<version>` - specify exact version like `17.0.15+6`

## Steps performed:
1. **Fetch release metadata**: Query Adoptium/Temurin API to find the releases details
2. **Update version properties**: Update JRE root directory name in the pom.xml `<properties>` section for each architecture
3. **Update download URLs**: Update download URLs and SHA256 hashes for all platform profiles
4. **Validate changes**: Ensure all Maven profiles are properly updated, then run `mvn clean verify -P<list of all architecture-specific profile ids>` to be sure there are no errors

## Platforms supported:
- Linux x64: `OpenJDK17U-jre_x64_linux_hotspot_{version}.tar.gz`
- Linux aarch64: `OpenJDK17U-jre_aarch64_linux_hotspot_{version}.tar.gz`
- macOS x64: `OpenJDK17U-jre_x64_mac_hotspot_{version}.tar.gz`
- macOS aarch64: `OpenJDK17U-jre_aarch64_mac_hotspot_{version}.tar.gz`
- Windows x64: `OpenJDK17U-jre_x64_windows_hotspot_{version}.zip`

## Usage:
```
/update-jres           # Uses latest version (default)
/update-jres latest    # Same as above
/update-jres 17.0.15+6 # Uses specific version
```

## Implementation Details:

Don't use the GitHub API, as there is an Adoptium API:

When using "latest" (default behavior), the command will:

1. **Query the latest release metadata**: Use the Adoptium API: `curl -X 'GET' 'https://api.adoptium.net/v3/assets/latest/17/hotspot?image_type=jre&vendor=eclipse' -H 'accept: application/json'`
2. **Handle missing architectures**: If an architecture isn't found in the latest release, it might be that the release process is in progress. In this case suggest staying on the previous version.
3. **Read metadata from the JSON**: For each architecture/version combination:
   - Read download URL from the JSON metadata (`binary.package.link`)
   - Read SHA256 hash from from the JSON metadata (`binary.package.checksum`)

When using a specific version, the command will:

1. **Query the release metadata**: Use the Adoptium API: `curl -X 'GET' 'https://api.adoptium.net/v3/assets/version/<version URL encoded>?image_type=jre&page=0&page_size=10&project=jdk&release_type=ga&semver=false&sort_method=DEFAULT&sort_order=DESC&vendor=eclipse' -H 'accept: application/json'`
2. **Read metadata from the JSON**: For each architecture/version combination:
  - Read download URL from the JSON metadata (`binaries.package.link`)
  - Read SHA256 hash from from the JSON metadata (`binaries.package.checksum`)
