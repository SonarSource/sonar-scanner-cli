# Claude Commands for SonarScanner CLI

## Update JREs

Help updating all bundled JREs to the latest available JRE 17 version, or to a specific version, by automatically querying the Adoptium API.

**Command:** `/update-jres`

**Usage:**
```
/update-jres           # Uses latest version (default)
/update-jres latest    # Same as above
/update-jres 17.0.15+6 # Uses specific version
```

**What it does:**
1. **Get JREs metadata**: Queries Adoptium API to get JREs metadata
2. **Updates version properties**: Updates architecture-specific dirname properties in pom.xml
3. **Updates download URLs**: Updates platform-specific download URLs
4. **Updates SHA256 hashes**: Updates SHA256 hashes for each platform
