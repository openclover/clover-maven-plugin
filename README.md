[![GitHub](https://img.shields.io/badge/license-Apache%202.0-silver.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![GitHub commit activity (branch)](https://img.shields.io/github/commit-activity/y/openclover/clover-maven-plugin/master)](https://github.com/openclover/clover-maven-plugin/commits/master)
[![GitHub last commit (branch)](https://img.shields.io/github/last-commit/openclover/clover-maven-plugin/master)](https://github.com/openclover/clover-maven-plugin/commits/master)
[![Generic badge](https://img.shields.io/badge/Website-openclover.org-green.svg)](https://openclover.org/)
[![RSS](https://img.shields.io/badge/rss-F88900?logo=rss&logoColor=white)](https://openclover.org/blog-rss.xml)

[![GitHub Workflow Status (with event)](https://img.shields.io/github/actions/workflow/status/openclover/clover-maven-plugin/A-test-master-jdk8.yml?label=JDK8)](https://github.com/openclover/clover-maven-plugin/actions/workflows/A-test-master-jdk8.yml)
[![GitHub Workflow Status (with event)](https://img.shields.io/github/actions/workflow/status/openclover/clover-maven-plugin/A-test-master-jdk11.yml?label=JDK11)](https://github.com/openclover/clover-maven-plugin/actions/workflows/A-test-master-jdk11.yml)
[![GitHub Workflow Status (with event)](https://img.shields.io/github/actions/workflow/status/openclover/clover-maven-plugin/A-test-master-jdk17.yml?label=JDK17)](https://github.com/openclover/clover-maven-plugin/actions/workflows/A-test-master-jdk17.yml)
[![GitHub Workflow Status (with event)](https://img.shields.io/github/actions/workflow/status/openclover/clover-maven-plugin/A-test-master-jdk21.yml?label=JDK21)](https://github.com/openclover/clover-maven-plugin/actions/workflows/A-test-master-jdk21.yml)


[![GitHub milestone details](https://img.shields.io/github/milestones/progress-percent/openclover/clover/11)](https://github.com/openclover/clover/milestone/11)
[![GitHub milestone details](https://img.shields.io/github/milestones/progress-percent/openclover/clover/14)](https://github.com/openclover/clover/milestone/14)
[![GitHub milestone details](https://img.shields.io/github/milestones/progress-percent/openclover/clover/15)](https://github.com/openclover/clover/milestone/15)
[![GitHub milestone details](https://img.shields.io/github/milestones/progress-percent/openclover/clover/4)](https://github.com/openclover/clover/milestone/4)

# About #

Clover Maven Plugin is an OpenClover integration with Maven 3.x and Maven 4.x.

This project is open-source, based on the Apache License version 2.0.

# Maven 4 compatibility (since 5.1.0) #

* Use the **`clover:setup`** goal to instrument your code. It works on both Maven 3 and Maven 4:

  ```
  mvn clover:setup verify clover:clover
  ```

* The **`clover:instrument`** and **`clover:instrument-test`** goals work on **Maven 3 only**. They fork
  a build lifecycle and mutate the project model at runtime. Maven 4 treats the project model as immutable during
  the build, so these mutations no longer reach the plugins running in the forked lifecycle. Instead of
  silently producing a wrong build, both goals now **fail on Maven 4** with a message pointing to
  `clover:setup`.

* **Repository pollution protection is now enabled by default**
  (`maven.clover.repositoryPollutionProtection` changed from `false` to `true`). Because `clover:setup`
  instruments sources in the main lifecycle, running `install` or `deploy` together with it would put
  instrumented classes into your local `~/.m2` cache or into a binaries repository - such a build now
  fails with an explanatory message. Run your build up to the `verify` phase, or set
  `-Dmaven.clover.repositoryPollutionProtection=false` if installing instrumented artifacts is intentional.

# Documentation #

Documentation: https://openclover.org/documentation

Issue tracker: https://github.com/openclover/clover/issues

OpenClover home page: https://openclover.org

Developer documentation: https://openclover.org/doc/manual/latest/developer-guide.html

===================================================

# Quick setup for developing the plugin

Useful Maven targets:

```
mvn integration-test -Pintegration-tests,integration-tests-maven3
```
