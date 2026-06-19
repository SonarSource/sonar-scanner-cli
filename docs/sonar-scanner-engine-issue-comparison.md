# sonar-scanner-engine issue comparison

This compares the open issues reported for:

- `java:scanner-engine-with-gradle-scan`
- `java:scanner-engine:with-dumped-properties`

Issues were exported with:

```sh
sonar list issues -p java:scanner-engine-with-gradle-scan --statuses OPEN --page-size 500 --page <page> --format json
sonar list issues -p java:scanner-engine:with-dumped-properties --statuses OPEN --page-size 500 --page <page> --format json
```

The raw page exports and merged JSON files are in `target/sonar-issue-comparison/`.

## Method

Sonar issue keys are project-specific, so the comparison uses this normalized identity:

```text
rule + relative component path + line + hash + message
```

The project key prefix was stripped from the component path before comparing. Counts below are multiset counts unless explicitly marked as unique. This matters because both scans contain a small number of duplicate issue identities.

## Summary

| Metric | Gradle scan | Dumped-properties scan |
| --- | ---: | ---: |
| Open issues | 2,118 | 911 |
| Unique normalized issue identities | 1,991 | 883 |
| Files | 1,463 | 1,963 |
| NCLOC | 68,550 | 87,353 |
| Classes | 1,474 | 1,924 |
| Functions | 7,349 | 10,123 |
| Bugs | 18 | 20 |
| Vulnerabilities | 20 | 20 |
| Code smells | 2,080 | 871 |

| Comparison | Count |
| --- | ---: |
| Exact issue instances common to both scans | 718 |
| Gradle-only issue instances | 1,400 |
| Dumped-only issue instances | 193 |
| Unique exact issues common to both scans | 702 |
| Unique Gradle-only exact issues | 1,289 |
| Unique dumped-only exact issues | 181 |

The dumped-properties scan covers more files and more NCLOC, but still reports far fewer issues. The difference is therefore not explained by the dumped-properties scan analyzing less code overall.

Both projects use the same Java quality profile: `Sonar way`, same profile key, with 594 active Java rules. The rule differences are not explained by different quality profiles.

## Main Rule Deltas

| Rule | Name | Gradle total | Dumped total | Gradle-only | Dumped-only |
| --- | --- | ---: | ---: | ---: | ---: |
| `java:S1874` | `@Deprecated` code should not be used | 1,138 | 0 | 1,138 | 0 |
| `java:S1450` | Private fields only used as local variables in methods should become local variables | 0 | 60 | 0 | 60 |
| `java:S1130` | Exceptions in `throws` clauses should not be superfluous | 62 | 18 | 44 | 0 |
| `java:S3740` | Raw types should not be used | 43 | 0 | 43 | 0 |
| `java:S2699` | Tests should include assertions | 33 | 0 | 33 | 0 |
| `java:S4449` | Nullness of parameters should be guaranteed | 34 | 3 | 31 | 0 |
| `java:S6353` | Regular expression quantifiers and character classes should be used concisely | 31 | 51 | 0 | 20 |
| `java:S2143` | `java.time` classes should be used for dates and times | 0 | 19 | 0 | 19 |
| `java:S8714` | Dedicated exception assertions should be used instead of `try-catch` with `fail()` | 0 | 15 | 0 | 15 |
| `java:S6204` | `Stream.toList()` should be used instead of collectors when an unmodifiable list is needed | 31 | 17 | 15 | 1 |
| `java:S8715` | JUnit Jupiter tests should not use JUnit 4 assertions | 0 | 15 | 0 | 15 |
| `java:S5838` | AssertJ assertions should be simplified to the corresponding dedicated assertion | 45 | 38 | 12 | 5 |
| `java:S2589` | Boolean expressions should not be gratuitous | 9 | 0 | 9 | 0 |
| `java:S5778` | Only one method invocation is expected when testing runtime exceptions | 40 | 34 | 8 | 2 |

## Cross-file and Type-inference Pattern

The strongest pattern is semantic Java analysis, especially rules that need resolved symbols, inherited annotations, nullability metadata, generics, method contracts, or project/dependency bytecode.

| Rule | Name | Gradle total | Dumped total | Gradle-only | Dumped-only |
| --- | --- | ---: | ---: | ---: | ---: |
| `java:S1874` | `@Deprecated` code should not be used | 1,138 | 0 | 1,138 | 0 |
| `java:S1130` | Exceptions in `throws` clauses should not be superfluous | 62 | 18 | 44 | 0 |
| `java:S3740` | Raw types should not be used | 43 | 0 | 43 | 0 |
| `java:S4449` | Nullness of parameters should be guaranteed | 34 | 3 | 31 | 0 |
| `java:S2589` | Boolean expressions should not be gratuitous | 9 | 0 | 9 | 0 |
| `java:S2638` | Method overrides should not change contracts | 4 | 0 | 4 | 0 |
| `javabugs:S2259` | Null pointers should not be dereferenced | 0 | 4 | 0 | 4 |
| `java:S6201` | Pattern matching for `instanceof` should be used | 8 | 6 | 4 | 2 |
| `java:S6204` | `Stream.toList()` should be used | 31 | 17 | 15 | 1 |
| `java:S2293` | The diamond operator should be used | 4 | 0 | 4 | 0 |

`java:S1874` dominates the Gradle-only set. It needs symbol resolution to know that the referenced type, method, field, or constructor is deprecated. The dumped-properties scan reports zero `S1874` issues even though the same Java profile is active, which strongly suggests the scanner CLI analysis does not build the same semantic model as the Gradle analysis.

Top Gradle-only deprecated-symbol messages:

| Message | Gradle-only count |
| --- | ---: |
| Remove this use of `ProjectDefinition`; it is deprecated. | 313 |
| Remove this use of `relativePath`; it is deprecated. | 95 |
| Remove this use of `write`; it is deprecated. | 75 |
| Remove this use of `path`; it is deprecated. | 64 |
| Remove this use of `ProjectReactor`; it is deprecated. | 50 |
| Remove this use of `ProjectBuilder`; it is deprecated. | 38 |
| Remove this use of `IssueFilter`; it is deprecated. | 35 |
| Remove this use of `FilterableIssue`; it is deprecated. | 34 |
| Remove this use of `AesECBCipher`; it is deprecated. | 28 |
| Remove this use of `InstantiationStrategy`; it is deprecated. | 27 |
| Remove this use of `ScannerSide`; it is deprecated. | 25 |
| Remove this use of `InputModule`; it is deprecated. | 22 |

The generated `sonar-project.properties` contains:

- 32 modules.
- `sonar.java.binaries` for all 32 modules.
- `sonar.java.test.binaries` for 30 modules.
- `sonar.java.libraries` for all 32 modules.
- `sonar.java.test.libraries` for all 32 modules.
- 690 main library entries and 1,429 test library entries.
- 0 project-root entries in `sonar.java.libraries`.
- 0 project-root entries in `sonar.java.test.libraries`.
- 0 `build/classes` entries in library properties.

That means the dumped-properties scan exports each module's own binaries and external dependencies, but it does not put sibling module outputs on dependent modules' library classpaths. Gradle analysis has access to the project dependency graph and can give the analyzer a richer cross-module classpath. This is the most likely reason for the missing semantic findings, especially `S1874`, `S3740`, `S4449`, `S1130`, and `S2638`.

Concrete examples of Gradle-only semantic issues:

| Rule | Path | Line | Message |
| --- | --- | ---: | --- |
| `java:S1874` | `server/sonar-scanner-engine/src/test/java/org/sonar/scanner/report/ReportPublisherTest.java` | 98 | Remove this use of `ProjectDefinition`; it is deprecated. |
| `java:S1874` | `shared/plugin-api-scanner-impl/src/test/java/org/sonar/scanner/plugin/api/impl/config/AesECBCipherTest.java` | 24 | Remove this use of `AesECBCipher`; it is deprecated. |
| `java:S1874` | `cloud/sonar-scanner-engine/src/main/java/org/sonar/scanner/scan/ProjectReactorBuilder.java` | 81 | Remove this use of `ProjectDefinition`; it is deprecated. |
| `java:S3740` | multiple files | - | Provide the parametrized type for this generic. |
| `java:S4449` | multiple files | - | Nullness of parameters should be guaranteed. |

## Source-area Pattern

Gradle-only issues are concentrated in scanner engine modules and shared plugin API implementation:

| Area | Gradle-only issues |
| --- | ---: |
| `cloud/sonar-scanner-engine` | 494 |
| `server/sonar-scanner-engine` | 473 |
| `shared/plugin-api-scanner-impl` | 136 |
| `server/plugins` | 75 |
| `cloud/sonar-scanner-engine-light` | 44 |
| `shared/sonar-scanner-extension-framework` | 26 |
| `server/sonar-core` | 22 |
| `cloud/sensor-test-fixtures` | 20 |
| `shared/sonar-duplications` | 20 |
| `server/sonar-ws` | 18 |

Dumped-only issues are much smaller in number and are concentrated in areas that the dumped-properties scan appears to cover more fully:

| Area | Dumped-only issues |
| --- | ---: |
| `cloud/sonar-scanner-report-viewer` | 38 |
| `server/sonar-scanner-protocol` | 36 |
| `cloud/plugins` | 35 |
| `cloud/sonar-scanner-engine` | 30 |
| `server/sonar-scanner-engine` | 22 |
| `server/sonar-plugin-api-impl` | 7 |
| `cloud/sensor-test-fixtures` | 7 |
| `server/sonar-ws` | 5 |

Representative dumped-only issues:

| Rule | Path | Line | Message |
| --- | --- | ---: | --- |
| `java:S1450` | `cloud/sonar-scanner-report-viewer` files | - | Private fields only used as local variables should become local variables. |
| `java:S6353` | `cloud/plugins/sonar-xoo-plugin` files | - | Use concise regular expression syntax. |
| `java:S2143` | `server/sonar-scanner-protocol` and related files | - | Use the `java.time` API for date and time. |
| `java:S8714` | scanner engine tests | - | Use dedicated exception assertions. |
| `java:S8715` | scanner engine tests | - | JUnit Jupiter tests should not use JUnit 4 assertions. |

These dumped-only issues look more like source inclusion differences and ordinary syntactic or local semantic checks. They do not form as strong a cross-file type-inference pattern as the Gradle-only set.

## Conclusion

The Gradle scan reports 1,207 more open issues than the dumped-properties scan, despite analyzing fewer files and less NCLOC. The difference is overwhelmingly caused by semantic Java rules, especially `java:S1874`, which is completely absent from the dumped-properties scan.

The generated properties file does export per-module binaries and external libraries, but it does not export sibling module outputs as library classpath entries. For a multi-module Gradle project, that likely prevents the scanner CLI run from resolving the same cross-module symbols that the Gradle scanner can resolve from the Gradle dependency graph.

The next property-dumper improvement to investigate is adding local project dependency outputs, such as dependent modules' `build/classes/java/main` and possibly test outputs where appropriate, to each module's `sonar.java.libraries` and `sonar.java.test.libraries`. Re-running the dumped-properties scan after that should show whether the missing `S1874`, `S3740`, `S4449`, `S1130`, and similar semantic findings reappear.
