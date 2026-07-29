# Changelog

All notable changes to the Clover Maven Plugin (formerly `maven-clover2-plugin`,
later renamed to `clover-maven-plugin`) are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Versions and dates are taken from Git tags. Issue keys (e.g. `OC-nnn`, `CLOV-nnn`,
`CLMVN-nnn`) refer to the project's issue trackers.

## [Unreleased]
### Changed
### Fixed
### Deprecated

## [5.1.0.1] - 2026-07-29

### Changed
- **OC-323**: When `<jdk>` is not defined, the source level now falls back to the `<release>`
  (`maven.compiler.release`) and `<source>` (`maven.compiler.source`) properties before falling back
  to auto-detection (based on runtime JVM).

## [5.1.0] - 2026-07-26

### Changed
- **OC-309**: Maven 4 compatibility. The `clover:setup` goal is now the recommended (and, on Maven 4,
  the only) way to instrument sources - it runs in the main lifecycle and does not mutate the project
  model.
- **OC-309**: `maven.clover.repositoryPollutionProtection` now defaults to `true` (it used to be
  `false`). Running the `install` or `deploy` phase while instrumentation is active fails the build.
  Set the flag to `false` to restore the previous behaviour.

### Fixed
- **OC-30**: Improved handling of instrumented module dependencies. Prior to the fix, a dependency on
  a module of the same build was resolved to a `-clover` artifact from the local repository, which could
  come from some earlier build and not match current sources. Such a dependency now uses instrumented
  classes produced by that module in the current build - both a plain jar dependency (main classes) and
  a `test-jar` dependency (test classes) are resolved from the reactor.
- **OC-309**: The repository pollution protection never actually ran. `AbstractCloverInstrumentMojo`
  and `CloverInstrumentInternalMojo` both declared a `mavenSession` parameter, so the subclass field
  shadowed the superclass one and the latter stayed `null` - the build lifecycle analysis always failed
  with "Failed to call Maven's internals via reflections" and the protection was silently skipped.
  The duplicate declaration is gone and the underlying exception is now logged with the warning.

### Deprecated
- **OC-309**: The `clover:instrument` and `clover:instrument-test` goals fail on Maven 4. They fork
  a build lifecycle and mutate the project model at runtime; Maven 4 treats the project model as
  immutable at execution time, so these mutations are not visible to the plugins running in the forked
  lifecycle. Both goals keep working on Maven 3.

## [5.0.0] - 2026-07-16

### Changed
- **OC-194**: Renamed the plugin package `com.atlassian.maven.plugin.clover` to
  `org.openclover.maven.plugin`, updated imported classes for OpenClover core 4.6.0+,
  and renamed packages in the code samples. Bumped OpenClover core to 5.0.0.
- **OC-236**: Raised the minimum required Maven version to 3.3.1 and updated the
  `org.apache.maven` dependencies and build plugin versions.
- **OC-234**: Dropped Java 1.7 support; source/target level raised from 1.7 to 1.8.
- **OC-194**: Migrated deployments to the `ossrh-staging-api`. Updated Groovy in the
  test cases to 4.0.32 (with matching groovy-eclipse batch/compiler).

### Removed
- **OC-194**: Removed the `gmaven-plugin` and `gmaven-plugin-no-java` integration
  tests (the plugin has been dead for 14 years).

### Fixed
- **OC-236**: Removed usage of `Strings.nullToEmpty`, which caused a
  `NoClassDefFoundError` because Guava is not on Maven's classpath.
- **OC-213**: The `site:site` goal now generates the "Goals" section with the plugin
  documentation (added `maven-plugin-report-plugin`).
- **OC-229**: Added a test plan with Java 21.

## [4.5.2] - 2024-01-31

### Added
- **OC-123**: Added the `recordTestResults` option for `clover:setup` and
  `clover:instr`, matching the option available in the Ant tasks.

### Changed
- **OC-213**: Updated to OpenClover core 4.5.2.
- **OC-224**: Replaced jMock with Mockito 4.11.0, converting the affected tests from
  JUnit 3 to JUnit 4; updated the `jaxb` integration test to the Jakarta API
  (`jakarta.xml.bind-api` 4.0.1, jaxb impl 4.0.4, `jaxb2-maven-plugin` 3.1.0);
  bumped Maven plugin versions, JUnit to 4.13.2 and Ant to 1.10.13.

## [4.5.1] - 2023-10-29

### Changed
- **OC-212**: Updated Clover core to 4.5.1.

### Fixed
- **OC-212**: Fixed Javadoc generation on Java 11 (removed the forbidden `<tt>` tag
  and disabled checking of Java API links).

## [4.5.0] - 2023-10-04

### Added
- **OC-117**: Added a `clover-runtime` integration test and a `site:site` generation
  step; added GitHub Actions workflows for building and releasing.

### Changed
- **OC-117**: Set the `cloverVersion` property (version of `org.openclover:clover`
  used by the plugin) to match the released plugin version; updated
  `maven-surefire-plugin` to 3.1.2 with the `surefire-junit47` runner; switched
  groovy-eclipse batch/compiler to versions available on Maven Central (Bintray was
  shut down). Now tested against JDK 11 and JDK 17 (beta support).
- **OC-128**: Upgraded Guava to 29.0-android (JDK 7 compatible) to fix CVE-2018-10237.
- **OC-101**: Recommended Maven version raised to the latest (Java 7 now required).
- **OC-98**: Migrated the source repository from Mercurial to Git on GitHub and
  updated the SCM URLs and documentation links.
- Bumped Ant (1.9.4 → 1.10.11) and JUnit (4.12 → 4.13.1) across dependencies and
  integration tests (Dependabot).

### Removed
- **OC-117**: Removed Guava and the obsolete `license` integration test project.

### Fixed
- **OC-65**: Use `SourceLevel` to validate the source level passed to the
  instrumenter (logs a warning instead of throwing a build error).
- **NONE**: Removed log4j from the `reportLogPassMatch` integration test (replaced
  with slf4j) to address Dependabot vulnerability warnings.

## [4.4.1] - 2019-10-12

### Changed
- **NONE**: Use OpenClover core 4.4.1.

### Fixed
- **OC-103**: Fixed a `NullPointerException` when `ArtifactResolver` resolves an
  artifact with a `clover` classifier but the artifact file is still null; the
  `clover` artifact is no longer looked up in remote repositories, and the scope is
  copied from the original artifact (a resolved artifact has a null scope).
- **OC-107**: Added the missing `@Mojo` annotation for the `clover:instrument-test` goal.

## [4.4.0] - 2019-09-26

### Added
- **OC-84**: Added a `<testSources>` configuration option for `clover:setup` /
  `clover:instr`, allowing custom detection of test sources (converted to `-tsr`
  arguments for `CloverInstr`).
- **OC-19**: Added a `<methodWithMetricsContext>` configuration option for
  `clover:setup` and `clover:instr`.
- **OC-82**, **OC-86**: Added `includeFailedTestCoverage` for `clover:check` and
  `clover:clover`, and `showUniqueCoverage` for `clover:clover`.

### Changed
- **OC-18**: Upgraded the plugin to Maven 3 — removed Maven 2 support and the
  `Maven2LifecycleAnalyzer`, replaced javadoc tags with mojo annotations, switched
  to `ArtifactResolver` / `maven-artifact-transfer`, `maven-reporting-api` 3.0,
  and `@Component`; removed the deprecated `reportStyle` property.
- **OC-83**: Switched dependencies to OpenClover 4.4.0.
- **OC-90**: No longer loads or registers any license key.
- **NONE**: Configured Bitbucket Pipelines for the project.

### Fixed
- **OC-18**: Reduced the "not finding an artifact with the 'clover' classifier"
  message from warning to debug (it is a common, normal case).

## [4.3.1] - 2018-09-22

### Changed
- **OC-76**: Bumped Clover core to 4.3.1.

### Fixed
- **OC-77**: Allow `1.9` and `9` as source levels.

## [4.3.0] - 2018-08-04

### Added
- **OC-61**: Basic support for Java 9 (source/target bumped to 1.6, refreshed GWT,
  JAXB and groovy-eclipse integration tests, htmlunit with IE11).

### Changed
- **NONE**: Switched to clover-core 4.3.0.
- **OC-45**: Improved the Maven configuration.
- **OC-46**: Improved the `maven-invoker` configuration.
- **OC-48**: Simplified the `maven-surefire-plugin` configuration.

### Fixed
- **OC-42**: Generate the historical report only when the `generateHistorical` flag
  is set (not merely when history files are present).
- **OC-40**: Warn about a missing history directory or files only when
  `generateHistorical` is true.

## [4.2.1] - 2017-11-22

### Changed
- **OC-27**: Use Clover Core 4.2.1.
- **OC-32**: Updated documentation links.
- **OC-25**: Replaced `com.atlassian.clover` with `org.openclover` in the integration
  tests.

### Fixed
- **OC-24**: Downgraded `maven-plugin-plugin` to 3.2, because newer versions produced
  a plugin incompatible with Maven 2.x.

## [4.2.0] - 2017-05-12

### Changed
- **OC-15**: Switched the project to OpenClover — changed `groupId` to
  `org.openclover`, removed the Atlassian parent POM and URLs, raised the minimum
  Maven version to 3.0.1, removed the Maven 2 profile, bumped all dependencies,
  removed `wagon-webdav`, and prepared for OSSRH publishing (gpg plugin, distribution
  management). Replaced the deprecated `ArtifactFactory`/`ArtifactResolver`/
  `ArtifactRepository` with `RepositorySystem` where possible.
- **CLOV-1972**: The license file is no longer required or bundled; no system
  property is set unless a license cert/file is defined.
- **CLOV-1972**: Renamed "Clover-for-Maven" / "Maven Clover Plugin" to
  "Clover Maven Plugin", per the Apache Maven plugin naming convention.

## [4.1.2] - 2016-10-11

### Added
- **NONE** (pull request #3, `includesFile`): Support including instrumented files
  from an external file.

### Changed
- **CLOV-1954**: Use Clover Core 4.1.2; upgraded the parent POM to 3.0.105.

### Removed
- **NONE**: Removed the deprecated `reportStyle` from `default-clover-report.xml`
  and marked `reportStyle` in `ReportMojo` as deprecated (it generated deprecation
  warnings).

## [4.1.1] - 2015-12-07

### Added
- **NONE** (pull request #2, `excludesFile`): Excludes can now be provided from an
  external file.

### Changed
- **CLOV-1775**: Renamed `maven-clover2-plugin` to `clover-maven-plugin` — the goal
  prefix changed from `clover2:` to `clover:` throughout goals, documentation, code
  comments, log messages and integration tests.
- **CLOV-1781**: Changed `cloverVersion` to the latest stable Clover version (4.1.1).

## [4.0.7] - 2015-12-01

### Added
- **NONE** (pull request #1): Excludes can now be provided from an external file.

### Changed
- **noissue**: Bumped the minimum required Maven version from 2.0.1 to 2.1.0.
- **CLOV-1772**: Bumped the plugin version.

### Fixed
- **CLOV-1825**: Added more logging for `clover:aggregate` when Clover is unable to
  properly recognize a module's descendants.

## [4.0.6] - 2015-09-18

### Changed
- **CLOV-1772**: Bumped `cloverVersion`.
- **CLOV-1780**: Added a rename/migration notice to the POMs and `index.apt`.

## [4.0.5] - 2015-07-20

### Added
- **CLOV-1596**: Documented the `all_but_reference` lambda instrumentation option.

### Changed
- **CLOV-1765**: Use Clover Core 4.0.5.

### Fixed
- **NONE**: Reduced the severity of the "The reported language of this project is ..."
  message to DEBUG (in most cases it is not a problem).

## [4.0.4] - 2015-04-20

### Added
- **CLOV-1632**: Repository pollution protection — a new `repositoryPollutionProtection`
  parameter for `clover:setup` / `instr` / `instr-test` that can fail a build when
  Clovered artifacts (or a custom classifier used together with the `-clover` one)
  would be installed or deployed. Includes analysis of active Maven 2 and Maven 3
  build life cycles, better diagnostic error messages, and allowance for the
  `javadoc` and `sources` classifiers.

### Changed
- **CLOV-1632**: Use the original Guava library instead of the repacked version from
  `clover.jar`.
- **CLOV-1668**: Warn that `clover:install` does not protect against installation of
  artifacts with classifiers.

## [4.0.3] - 2015-01-29

### Changed
- **NONE**: Bumped Clover core to 4.0.3.
- **CLOV-1605**: Replaced `clover.licenseLocation` with `maven.clover.licenseLocation`
  and pass it to the test projects via the `maven-invoker-plugin`.

### Fixed
- **CLOV-1597**: `instrumentLambda` now defaults to `none`.

## [4.0.2] - 2014-10-13

### Changed
- **NONE**: Use Clover Core 4.0.2.

## [4.0.1] - 2014-09-02

### Changed
- **NONE**: Use Clover Core 4.0.1.

### Fixed
- **CLOV-1478**: Fixed a double-instrumentation error under Maven 2.2.1 and JDK 8
  caused by mismatched map keys (`project.getId()` is not always equal to
  `project.getArtifact().getId()`).
- **CLOV-1470**: Added a test case for a module containing generated sources only.

## [4.0.0] - 2014-07-14

### Added
- **CLOV-1410**: Added a `reportStyle` attribute for the `clover2:clover` mojo.

### Changed
- **CLOV-1467**: Use Clover core 4.0 with the new group id
  (`com.atlassian.clover:clover`); updated the integration test cases for the new
  ADG report layout.
- **CLOV-1369**: Replaced JDK 1.3/1.4/1.5 with 1.6.

### Fixed
- **CLOV-1471**, **CLOV-1469**: `MainInstrumenter` and `TestInstrumenter` now
  distinguish generated source directories so that classes from added test source
  roots (build-helper's `add-test-source`) are not treated as application classes.

## [3.3.0] - 2014-04-01

### Added
- **CLOV-1399**: Added the `instrumentLambda` property for `clover2:setup` and
  `clover2:instr` (passed to `CloverInstr`), enabling Java 8 lambda instrumentation;
  introduced the `AbstractCloverInstrumentMojo` base class.
- **CLOV-1335**: Added the `showInnerFunctions` and `showLambdaFunctions` options for
  the `clover2:clover` mojo.

### Changed
- **NONE**: Increased Clover core to 3.3.0; made the build and integration tests
  compatible with JDK 8.

### Fixed
- **CLOV-1381**: Removed the redundant `clover-report.xml` (the default is bundled in
  `main/resources`) and the self-reference in `<pluginManagement>`.

## [3.2.2] - 2014-02-12

### Changed
- **NONE**: Updated the Atlassian Public/Snapshot repository URLs, the
  `org.sonatype.oss:oss-parent` parent (9), `maven-install-plugin` (2.5.1) and
  `maven-surefire-plugin` (2.16).
- **NONE**: `clover2:check` compares the actual percentage using as many fractional
  digits as set in the property (documented).

## [3.2.0] - 2013-10-23

### Added
- **CLOV-1337**: Ability to set the `1.8` language level.

### Changed
- **CLOV-1202**, **CLOV-1193**: Removed Retrotranslator so that the plugin ships
  JDK 1.5 bytecode; renamed all imports from `com.cenqua.*` to `com.atlassian.*`;
  updated the dependency to Clover core 3.2.0.

## [3.1.12.1] - 2013-10-01

### Changed
- **NONE**: Increased the Clover core version to 3.1.12.1.

## [3.1.12] - 2013-07-02

### Added
- **CLOV-1144**: Support for the `groupId`-based `groovy-eclipse-plugin` layouts —
  correct handling of `src/main/groovy` / `src/test/groovy` and Groovy sources placed
  in Java folders (they are copied to the instrumented tree rather than being treated
  as Java), plus numerous integration tests for gmaven and groovy-eclipse projects.

### Changed
- **NONE**: Migrated the source code to Java 1.5 (generics, foreach, `final`) with
  Retrotranslator for backward compatibility with 1.4; bumped third-party library
  versions (qdox 1.11+).
- **NONE**: Changed the SCM/documentation links from Atlassian Studio to Bitbucket.

### Fixed
- **CLOV-1290**: Fixed Clover history report generation for a multi-module project
  with a relative `historyDir` path.

## [3.1.11] - 2013-03-25

### Changed
- **NONE**: Increased Clover Core to 3.1.11; updated imports as packages were renamed
  in Clover core (`com.cenqua.*` → `com.atlassian.*`).

### Fixed
- **CLOV-1170**: Fixed a bug in `clover:optimizeIntegration` where null was returned
  instead of the variable value.
- **CLOV-1166**: Corrected the version range; added integration tests checking
  Clover for Maven against different groovy-eclipse-plugin / groovy-all versions.
- **CLMVN-142**: Fixed links to JPG images.

## [3.1.10.1] - 2013-01-24

### Changed
- **NONE**: Prepared release 3.1.10.1 with new SCM / issue tracker URLs in the POM
  (based on Clover core 3.1.10).

## [3.1.10] - 2013-01-09

### Added
- **CLOV-1170**: Support for regular expressions and comma-/space-separated paths in
  `<include>` / `<exclude>` tags, matching the maven-surefire-plugin behavior.

### Changed
- **CLOV-1199**: Adapted to the new release process — deploying to OSS Sonatype and
  removing the duplicated gpg plugin (artifacts were being signed twice).
- **CLOV-1109**: Made the integration tests runnable on Maven 2.0.x (lowered
  PMD/Checkstyle versions).

### Fixed
- **CLOV-1109**: `testFailureIgnore` is no longer hardcoded in `lifecycle.xml` — the
  proper properties are added dynamically when `setTestFailureIgnore=true` is set for
  `clover2:setup` or `clover2:instr`.

## [3.1.8] - 2012-11-13

### Added
- **CLOV-1137**: Added the `skipGroverJar` and `groverJar` attributes for
  `clover2:setup` and `clover2:instrumentInternal`.
- **NONE**: Allow setting `codeType` for `CloverCheckMojo` (`APPLICATION` (default),
  `ALL`, `TEST`) to control which part of the application coverage is checked.

## [3.1.7] - 2012-08-31

### Changed
- **NONE**: `maven-clover2-plugin` 3.1.7 now depends on Clover 3.1.7.

### Added
- **CLOV-1082**: Made the webapp sample work — updated Cargo to 1.2.3 and added a
  `MyServletContextListener` so the webapp Clover runtime connects back to the Clover
  server. Added an example combining `maven-surefire-plugin` and
  `maven-failsafe-plugin` for unit and integration tests.

## [3.1.6] - 2012-06-19

### Added
- **CLV-5839**: Example of how to build and test a GWT application with Clover.
- **CLOV-753**: Added the `jaxb` integration test.

### Changed
- **CLOV-1064**: Moved `isModuleOfProject()`, `getModuleProjects()` and
  `getDescendentModuleProjects()` from `CloverAggregateMojo` to `AbstractCloverMojo`;
  `CloverLogMojo` now adds test directories from sub-modules as well.

### Fixed
- **CLOV-1026**: Handle modules with the same `artifactId` by using `getId()`
  (groupId + artifactId + classifier + version) instead of `getArtifactId()`.
- **CLMVN-148**: Change for optimizing integration tests.
- **NONE**: Fixed a bug where `clover2:optimize` with an empty `optimizeExcludes`
  did not read the value from the maven-surefire-plugin `excludes` property.

## [3.1.5] - 2012-04-26

### Changed
- **CLV-5827**: Corrected the issue-tracker and SCM URLs and increased the Clover
  dependency to 3.1.5.

## [3.1.4] - 2012-02-27

### Changed
- **NONE**: Bumped the version for release.

## [3.1.3] - 2012-01-17

### Changed
- **NONE**: Updated `cloverVersion` to 3.1.3; set the snapshot repository to
  Atlassian public.

### Fixed
- **CLMVN-145**: Ensure `DistributedCoverage` is not passed into
  `InstrumentationConfig`, which caused a `ClassNotFoundException` during Groovy
  instrumentation.
- **CLOV-1042**: Released a snapshot with a candidate fix.

## [3.1.2] - 2011-11-07

### Changed
- **NONE**: Upped the Clover version.

## [3.1.1] - 2011-11-06

### Changed
- **NONE**: Upped the Clover version.

## [3.1.0] - 2011-05-31

### Added
- **CLMVN-141**: Support for Java 1.7 (with Clover core 3.1.0).

## [3.0.5] - 2011-04-13

### Fixed
- **CLMVN-136**: Applied a patch (submitted by Jeff Melching) for Maven 3 support.

## [3.0.4] - 2010-12-02

### Fixed
- **CLMVN-136**: Fix for Maven 3 support — use the latest `plexus-resources` to load
  `report-descriptor.xml`.
- **NONE**: Defer saving of history to the last module when a single clover DB is
  specified (refactored out `isLastProjectInReactor`).

## [3.0.2] - 2010-04-13

### Fixed
- **NONE**: Bugfix release for Bamboo XML parsing.

## [3.0.1] - 2010-03-31

### Changed
- **NONE**: Bumped the version number to work around a deployment issue.

## [3.0.0] - 2010-03-30

### Added
- **CLOV-890**: Added support for Groovy instrumentation, with functional tests.
- **CLOV-914**: Added method and conditional percentage checks (and a
  `statementPercentage` target) for the `clover2:check` mojo.

### Changed
- **CLOV-908**: Set the `grover.jar` and grover config artifacts to `scope=SYSTEM`
  to prevent Maven trying to download them from remote repositories.

### Fixed
- **CLMVN-131**: Actually resolve the clover artifact before adding it as a project
  dependency.
- **CLOV-819**: Fixed the documentation of the `clover2:clean` mojo.

## [2.6.3] - 2009-11-20

### Added
- **CLMVN-130**: Added the `maven.clover.alwaysReport` option so a report is produced
  even when there are no unit tests.
- **CLMVN-129**: New `copyExcludedFiles` option.

### Changed
- **CLMVN-128**: Add `clover.jar` as a `provided` dependency instead of `compile`,
  and added a `maven.clover.scope` configuration to modify this.
- **CLOV-753**: Added a sample project that uses the jaxb-plugin.

## [2.6.2-r2] - 2009-10-16

### Fixed
- **NONE**: Depend on Clover core 2.6.2 (not 2.6.1).

## [2.6.2] - 2009-10-15

### Changed
- **NONE**: Release preparation (no functional changes).

## [2.6.1] - 2009-10-02

### Added
- **CLMVN-127**: New mojo (patch from Sumit Shah) that forks the lifecycle but only
  runs to the `test` phase, not the `install` phase as `clover2:instrument` does.

### Changed
- **NONE**: Updated the Clover core dependency to 2.6.1.

## [2.6.0] - 2009-09-09

### Added
- **CLMVN-124**: New `maven.clover.includesList` and `maven.clover.excludesList`
  options to define includes/excludes from a comma-separated list on the command line.
- **CLMVN-123**: New `keepDb` option on `clover2:clean` that preserves the
  `clover.db` file when deleting the `target/clover` directory.
- **CLMVN-119**: Added the `clover2:reset` goal, which resets the output and source
  directories to their originals.

### Changed
- **CLMVN-121**: `clover2:clean` now removes the `target/clover` directory as well as
  the snapshot file.
- **CLMVN-118**: Only create output directories when there is actually source to
  instrument (works around MINSTALL-18).

### Fixed
- **CLMVN-126**: Filter test-class coverage before checking with `clover2:check`.
- **CLMVN-122**: Only fall back to the surefire test file set for optimization if
  neither includes nor excludes are given (patch from Chris Kiehl).
- **RELENG-344**: `maven-plugin-management-plugin` 1.0-atlassian-1 breaks in Maven 2.2.0.

## [2.5.1] - 2009-05-28

### Added
- **CLMVN-116**: New option to omit the `-clover` classifier on the Clovered artifact.
- **CLMVN-115**: New `debug` flag that turns on debugging info for test optimization
  (e.g. prints the list of files Clover detects as modified).
- **CLMVN-114**: Made the fudge-factor a configuration option
  (`maven.clover.cloveredArtifactExpiryInMillis`).

### Changed
- **CLMVN-117**: `clover2:clean` now *deletes* the snapshot file rather than
  preserving it; added a `maven.clover.clean.skip` flag.
- **CLMVN-113**: Save `clover.snapshot` to `${basedir}/.clover/clover.snapshot` by
  default.

## [2.5.0] - 2009-05-11

### Added
- **CLMVN-111**: Load the license for `clover2:merge`.
- **CLMVN-109**: New `forceSnapshot` option, so a snapshot file may be created during
  the execution of any sub-module.
- **CLMVN-95**: New nested `DistributedCoverage` element for configuring distributed
  coverage collection; added method-level-only instrumentation.

### Changed
- **CLMVN-106**: Correctly pass all Maven 2 properties to the custom Clover report
  descriptor (patch from Henri Tremblay).
- **CLMVN-105**: Honor the standard `project.build.sourceEncoding` property.
- **CLMVN-100**: Renamed `src-optimized` to `src-instrumented` (since `clover2:setup`
  is used for full builds as well).

### Fixed
- **CLMVN-97**: Prevent artifacts with the same `artifactId` being instrumented to
  the wrong location.
- **CLMVN-99**: Remove duplicate dependencies.

## [2.4.3] - 2009-03-09

### Added
- **CLMVN-94**: New option to copy resources stored in `src/main/java` to
  `target/clover/src`.
- **CLMVN-95**: New `serverLocation` configuration for test optimization.
- **CLMVN-87**: Use `project.build.finalName` if it is set, otherwise the default.

### Fixed
- **CLMVN-96**, **CLOV-441**: Fixed a bug that caused compilation errors when
  `<includes>` was configured; use Ant's `DirectoryScanner` for finding
  included/excluded files.
- **CLMVN-90**: Use a `StaleSourceScanner` to scan for files to instrument so as not
  to cause unnecessary instrumentation/compilation during test optimization.
- **CLMVN-78**: Bugfix when a directory contains two `pom.xml`s, one a sub-module of
  the other.
- **CLMVN-89**: Compile unit tests with `target=1.5` while source code still targets 1.4.
- **CLMVN-92**: Ensure jMock is scoped to `test`.

## [2.4.2] - 2008-12-01

### Added
- **CLMVN-14**: Added the new `filteredElements` column to the default report columns.

### Changed
- **NONE**: Updated the Clover core dependency to 2.4.2.

### Fixed
- **CLMVN-86**: Create the snapshot directory if it does not exist.

## [2.4.1] - 2008-11-05

### Fixed
- **CLOV-368**: Bumped to the 2.4.1 dependency on `clover.jar`.

## [2.4.0] - 2008-11-03

### Added
- **CLMVN-76**: Test runner optimization — new goals `clover2:setup`,
  `clover2:optimize`, `clover2:checkpoint` (later renamed to `snapshot`) — plus a
  `skip` flag and support for a global/per-project single Clover database via
  `maven.clover.singleCloverDatabase`.
- **CLMVN-80**: Added an `ordering` parameter and an `alwaysRunTests` list to
  `clover2:optimize`.
- **CLMVN-73**: `clover2:clean`, and console dumping of the checkpoint for debugging.

### Changed
- **CLMVN-79**: The default Clover span is now `Integer.MAX_VALUE` seconds (not 0),
  to ensure no coverage is missed.
- **CLMVN-75**: Instrument sources during the `process-sources` phase (not `verify`),
  so generated sources are instrumented correctly.
- **CLMVN-14**: Allow custom contexts to be defined.

### Fixed
- **CLOV-353**, **CLMVN-83**: Use a `LinkedHashSet` when adding Clover to the
  compile-time classpath to preserve order (thanks to Brett Graves).
- **CLMVN-67**: Work around the license being deleted on some Windows platforms
  (license files are resolved to the temp directory and deleted on exit).
- **CLMVN-68**: Correctly specify the test directories for aggregate multi-module
  projects.
- **CLMVN-66**: Use `artifactId` (not `project.getName`) for the report title, and
  set the report title for the historical target too.

## [2.3.3-ATL1] - 2008-09-23

### Added
- **CLMVN-76**: Allow a custom checkpoint file location to be specified;
  default `generateXml` to true.
- **CLMVN-73**: First cut of test runner optimization and `clover2:clean`.

### Changed
- **CLMVN-75**: Instrument source code during `process-sources` (not `verify`) to
  ensure generated sources are instrumented correctly.

## [2.3.2] - 2008-07-16

### Added
- **CLMVN-36**: First version of `CloverMergeMojo`, which merges arbitrary databases
  (not only those in the sub-projects) — patch from Alex B.
- **CLMVN-9**: Allow the encoding of HTML reports to be configured.
- **CLMVN-60**: Added support for `maven.clover.span` in the aggregate task.

### Fixed
- **CLMVN-64**: If the Clover report descriptor is defined but not available as a
  file, resolve it as a resource on the classpath.

## [2.3.1] - 2008-05-27

Initial OpenClover-lineage release of the Maven Clover 2 plugin, imported from the
Apache `maven-clover-plugin` (SVN revision 574419) and repackaged for Clover 2.0.

### Added
- **CLMVN-18**: Got the `maven-plugin-plugin` running over the Clover plugin (patch
  from Marcel May).
- **CLMVN-35**: Added `historyDir` and `historyThreshold` properties to
  `clover-check`, allowing a project to "ratchet up" its coverage and fail the build
  if coverage drops.
- **CLMVN-8**: Allow arbitrary `clover-report.xml` descriptors to be specified, plus
  a `resolveReportDescriptor` option to load the descriptor from a Maven repository.
- **CLMVN-13**: Added a `license` parameter to specify the license inline in the POM;
  fall back to the default Clover license if none is configured.
- **CLMVN-33**: Preliminary `skip` parameter.

### Changed
- **CLMVN-34**: Upgraded the `artifactId` everywhere; added a new option to support
  JDK 1.6.
- **CLMVN-42**, **CLMVN-28**: Allow `maven.clover.encoding` to set the encoding used
  when parsing source files.
- **CLMVN-25** / **CLMVN-46**: Renamed the `generateXXX` system properties to
  `maven.clover.generateXXX`.

### Fixed
- **CLMVN-41**: Fixed the Maven multi-module `generate-sources` issue (generated
  sources appearing twice due to the forked lifecycle).
- **CLMVN-44**: Rolled Ant back to 1.6.5 to avoid a `URISyntaxException`.
- **CLOV-251**: Clover core now ignores missing or invalid test-source directories.

### Removed
- **CLMVN-38**: Removed the `useSurefireTestResults` option.

[5.0.0]: https://github.com/openclover/clover-maven-plugin/compare/clover-maven-plugin-4.5.2...clover-maven-plugin-5.0.0
[4.5.2]: https://github.com/openclover/clover-maven-plugin/compare/clover-maven-plugin-4.5.1...clover-maven-plugin-4.5.2
[4.5.1]: https://github.com/openclover/clover-maven-plugin/compare/clover-maven-plugin-4.5.0...clover-maven-plugin-4.5.1
[4.5.0]: https://github.com/openclover/clover-maven-plugin/compare/clover-maven-plugin-4.4.1...clover-maven-plugin-4.5.0
[4.4.1]: https://github.com/openclover/clover-maven-plugin/compare/clover-maven-plugin-4.4.0...clover-maven-plugin-4.4.1
[4.4.0]: https://github.com/openclover/clover-maven-plugin/compare/clover-maven-plugin-4.3.1...clover-maven-plugin-4.4.0
[4.3.1]: https://github.com/openclover/clover-maven-plugin/compare/clover-maven-plugin-4.3.0...clover-maven-plugin-4.3.1
[4.3.0]: https://github.com/openclover/clover-maven-plugin/compare/clover-maven-plugin-4.2.1...clover-maven-plugin-4.3.0
[4.2.1]: https://github.com/openclover/clover-maven-plugin/compare/clover-maven-plugin-4.2.0...clover-maven-plugin-4.2.1
[4.2.0]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-4.1.2...clover-maven-plugin-4.2.0
[4.1.2]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-4.1.1...maven-clover2-plugin-4.1.2
[4.1.1]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-4.0.7...maven-clover2-plugin-4.1.1
[4.0.7]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-4.0.6...maven-clover2-plugin-4.0.7
[4.0.6]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-4.0.5...maven-clover2-plugin-4.0.6
[4.0.5]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-4.0.4...maven-clover2-plugin-4.0.5
[4.0.4]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-4.0.3...maven-clover2-plugin-4.0.4
[4.0.3]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-4.0.2...maven-clover2-plugin-4.0.3
[4.0.2]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-4.0.1...maven-clover2-plugin-4.0.2
[4.0.1]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-4.0.0...maven-clover2-plugin-4.0.1
[4.0.0]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-3.3.0...maven-clover2-plugin-4.0.0
[3.3.0]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-3.2.2...maven-clover2-plugin-3.3.0
[3.2.2]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-3.2.0...maven-clover2-plugin-3.2.2
[3.2.0]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-3.1.12.1...maven-clover2-plugin-3.2.0
[3.1.12.1]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-3.1.12...maven-clover2-plugin-3.1.12.1
[3.1.12]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-3.1.11...maven-clover2-plugin-3.1.12
[3.1.11]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-3.1.10.1...maven-clover2-plugin-3.1.11
[3.1.10.1]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-3.1.10...maven-clover2-plugin-3.1.10.1
[3.1.10]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-3.1.8...maven-clover2-plugin-3.1.10
[3.1.8]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-3.1.7...maven-clover2-plugin-3.1.8
[3.1.7]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-3.1.6...maven-clover2-plugin-3.1.7
[3.1.6]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-3.1.5...maven-clover2-plugin-3.1.6
[3.1.5]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-3.1.4...maven-clover2-plugin-3.1.5
[3.1.4]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-3.1.3...maven-clover2-plugin-3.1.4
[3.1.3]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-3.1.2...maven-clover2-plugin-3.1.3
[3.1.2]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-3.1.1...maven-clover2-plugin-3.1.2
[3.1.1]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-3.1.0...maven-clover2-plugin-3.1.1
[3.1.0]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-3.0.5...maven-clover2-plugin-3.1.0
[3.0.5]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-3.0.4...maven-clover2-plugin-3.0.5
[3.0.4]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-3.0.2...maven-clover2-plugin-3.0.4
[3.0.2]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-3.0.1...maven-clover2-plugin-3.0.2
[3.0.1]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-3.0.0...maven-clover2-plugin-3.0.1
[3.0.0]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-2.6.3...maven-clover2-plugin-3.0.0
[2.6.3]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-2.6.2-r2...maven-clover2-plugin-2.6.3
[2.6.2-r2]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-2.6.2...maven-clover2-plugin-2.6.2-r2
[2.6.2]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-2.6.1...maven-clover2-plugin-2.6.2
[2.6.1]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-2.6.0...maven-clover2-plugin-2.6.1
[2.6.0]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-2.5.1...maven-clover2-plugin-2.6.0
[2.5.1]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-2.5.0...maven-clover2-plugin-2.5.1
[2.5.0]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-2.4.3...maven-clover2-plugin-2.5.0
[2.4.3]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-2.4.2...maven-clover2-plugin-2.4.3
[2.4.2]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-2.4.1...maven-clover2-plugin-2.4.2
[2.4.1]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-2.4.0...maven-clover2-plugin-2.4.1
[2.4.0]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-2.3.3-ATL1...maven-clover2-plugin-2.4.0
[2.3.3-ATL1]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-2.3.2...maven-clover2-plugin-2.3.3-ATL1
[2.3.2]: https://github.com/openclover/clover-maven-plugin/compare/maven-clover2-plugin-2.3.1...maven-clover2-plugin-2.3.2
[2.3.1]: https://github.com/openclover/clover-maven-plugin/releases/tag/maven-clover2-plugin-2.3.1
