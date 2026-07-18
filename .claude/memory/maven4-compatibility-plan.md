# Maven 4 Compatibility — Failing Integration Tests: Analysis & Fix Plan

Branch: `OC-309-maven4-api-migration`
Tested with: **Apache Maven 4.0.0-rc-5** (JDK 21, Temurin), plugin `5.0.1-SNAPSHOT`.
Command to reproduce all ITs: `mvn -Pintegration-tests clean verify` (invoker plugin).
Run a single IT: `mvn invoker:run -Pintegration-tests -Dinvoker.test=<name>` (after `mvn install -DskipTests`).

## Summary

4 integration tests fail on Maven 4 (pass on Maven 3.9.x):

| IT | Failing goal | Root cause |
|----|--------------|------------|
| `xdoclet` | `verifier:verify` (verify phase) | **Cause A** — build-directory redirect invisible downstream |
| `groovy-eclipse-plugin` | `install` (forked lifecycle) | **Cause B** — reclassified main artifact rejected by install |
| `groovy-eclipse-plugin-src-main-java` | `install` (forked lifecycle) | **Cause B** |
| `groovy-eclipse-plugin-src-main-groovy` | `install` (forked lifecycle) | **Cause B** |

Both causes share one theme: **Maven 4 treats the project model as effectively immutable at
execution time.** The plugin's Maven-3 strategy of *mutating* `MavenProject` at runtime inside a
forked lifecycle (redirecting the build directory; swapping/reclassifying the main artifact) no
longer works the way it did.

---

## Cause A — `xdoclet`: build-directory redirection is not visible to later plugins

### Symptom
The IT test writes `test.clover` into `${project.build.directory}`; in the forked `clover`
lifecycle that should be `target/clover`, and `src/test/verifier/verifications.xml` asserts
`target/clover/test.clover` exists. Under Maven 4 the file lands in `target/test.clover`, so the
verifier fails.

### Mechanism (evidence from runtime probe)
`CloverInstrumentInternalMojo.redirectOutputDirectories()` (line ~447) calls:
```java
getProject().getBuild().setDirectory(this.cloverOutputDirectory);      // target/clover
getProject().getBuild().setOutputDirectory(.../clover/classes);
getProject().getBuild().setTestOutputDirectory(.../clover/test-classes);
```
A temporary `getLog().warn("[CLOVER-PROBE] ...")` around this call proved:
- **after** `setDirectory`, re-reading `getProject().getBuild().getDirectory()` returns
  `.../target/clover` — the legacy mutation *does* stick on this `MavenProject` instance.
- but the very next plugin (`maven-antrun-plugin` `echoproperties`) logs
  `Setting project property: project.build.directory -> .../target` (the **original** value), and
  surefire then reports `Build directory=.../target`.

So the mutation is **local to the mojo's `MavenProject` view and does not propagate** to the value
other plugins interpolate for `${project.build.directory}`.

### Why (Maven 4 internals — decompiled from `maven-*-4.0.0-rc-5.jar`)
Maven 4's compat `org.apache.maven.project.MavenProject` is a thin adapter over the **immutable**
`org.apache.maven.api.model.Model` (via `org.apache.maven.model.Model` → `getDelegate()`):
- `MavenProject.getBuild()` → `getModelBuild()` → `getModel().getBuild()`.
- `org.apache.maven.model.Model.getBuild()` constructs a **new** `Build` wrapper on every call,
  parent-linked to the `Model` wrapper via `BaseObject.childrenTracking`.
- `BuildBase.setDirectory(x)` does `update(getDelegate().withDirectory(x))`; `withDirectory`
  returns a **new immutable** `api.model.BuildBase`, and `BaseObject.update()` replaces the delegate
  in the parent via `childrenTracking.replace(...)`.

This means the legacy `MavenProject`/`Model` wrapper tree is updated correctly — which is why the
re-read works — **but** the values consumed elsewhere in Maven 4 (the immutable
`org.apache.maven.api.Project` snapshot held by the session, and/or the per-execution `MavenProject`
adapter handed to each mojo) are **not** derived from that same mutable wrapper. Runtime mutation of
the project model is therefore not a supported contract in Maven 4 (see "What's New in Maven 4":
project/plugin model is immutable; plugins should not mutate the reactor project).

### Fix directions to evaluate (Cause A)
1. **Stop redirecting the build directory by mutation.** Instead, drive the forked-lifecycle output
   location through configuration the downstream plugins actually read:
   - set the Ant/Maven property `project.build.directory` (and `.outputDirectory`,
     `.testOutputDirectory`) in `getProject().getProperties()` — verify whether antrun/surefire honor
     the property over the model in Maven 4 (they interpolate `${project.build.directory}`; needs a
     test because property vs. model precedence changed).
2. **Configure the instrumented output dir via the `clover` lifecycle overlay**
   (`src/main/resources/META-INF/maven/lifecycle.xml`) rather than mutating the project — e.g. bind
   the compiler/surefire output directories in the forked lifecycle mapping.
3. **Use the new Maven API** (`org.apache.maven.api.*`, JSR-330 injected `Project`/`Session`) to
   express the redirection in a Maven-4-supported way; requires a Maven-4-only code path
   (keep the Maven-3 path for the 3.3.1/3.9.2 matrix entries).
4. Confirm precedence: check `PluginParameterExpressionEvaluator` in Maven 4 to see exactly which
   object `${project.build.directory}` resolves against for a forked-lifecycle mojo execution.

---

## Cause B — `groovy-eclipse-*`: reclassified main artifact rejected by `install`

### Symptom
Project tests pass (7/7). The `-Pwith-clover-instr` fork runs the `instrument` goal, whose
`@Execute(phase = INSTALL, lifecycle = "clover")` forks a lifecycle through the `install` phase.
`maven-install-plugin:3.1.3:install` then fails:
> The packaging plugin for project `clover-sample-groovy-eclipse-plugin` did not assign a main file
> to the project but it has attachments. Change packaging to 'pom'.

### Mechanism
`CloverInstrumentInternalMojo.redirectArtifact()` (lines ~460-476) replaces the project's **main**
artifact with a `clover`-**classified** one:
```java
Artifact newArtifact = repositorySystem.createArtifactWithClassifier(
        groupId, artifactId, version, type, "clover");
getProject().setArtifact(newArtifact);
getProject().getBuild().setFinalName(finalName + (useCloverClassifier ? "-clover" : ""));
```
`useCloverClassifier` defaults to `true` (`AbstractCloverInstrumentMojo:343`), and
`shouldRedirectArtifacts()` returns `true`. The pollution-protection guards
(`failIfInstallPhaseIsPresent`, `AbstractCloverInstrumentMojo:531`) intentionally **allow** install
when `useCloverClassifier && shouldRedirectArtifacts()` — the Maven-3 design assumed the installed
artifact would be harmless because it carries the `clover` classifier.

### Why it fails on Maven 4 (decompiled `maven-install-plugin` `InstallMojo`)
`InstallMojo.processProject(...)` computes a `mainArtifact` and, when
`!isFile(mainArtifact.getFile())` **and** the project has attached artifacts, throws exactly this
message. Under Maven 4 the reclassified project artifact (classifier `clover`) ends up **not** being
the main file — the packaging plugin's produced JAR is registered as an attachment while the "main"
artifact has no file. Maven 3 tolerated a classifier on the project's main artifact through
install; **Maven 4's install validation does not** (a project's main artifact must have no
classifier and must carry the packaging file, otherwise packaging must be `pom`).

Note: the log also shows many `*:jar:clover:*` `ArtifactResolverException` entries from
`swizzleCloverDependencies()` trying to resolve "clovered" dependency variants — these are caught /
non-fatal (the build fails later at install), but they are noisy and worth revisiting under the new
resolver (Maven Resolver 2.0, now hidden behind the Maven API).

### Fix directions to evaluate (Cause B)
1. **Do not reclassify the project's main artifact.** In Maven 4 attach the clovered JAR as a
   *separate attached artifact* via `MavenProjectHelper.attachArtifact(project, type, "clover", file)`
   instead of `project.setArtifact(classifiedArtifact)`. Keeps the main artifact valid for install.
2. **Exclude `install`/`deploy` from the forked `clover` lifecycle** for Maven 4 so instrumented
   artifacts are never installed (the pollution-protection code already has the notion of forbidding
   install; extend it to *strip* install/deploy from the forked lifecycle mapping rather than merely
   erroring when the user runs them). This matches the plugin's stated intent ("run to verify, not
   install").
3. **Gate by Maven version.** Keep the Maven-3 reclassify path; add a Maven-4 path (attach-artifact
   or lifecycle-trim). Detect via runtime Maven version or the presence of the new API.
4. Revisit `BuildLifecycleAnalyzer` — only `Maven3LifecycleAnalyzer` exists
   (`internal/lifecycle/`). A Maven-4 analyzer may be needed; confirm
   `LifecycleExecutor.calculateExecutionPlan(...)` still behaves the same on Maven 4.

---

## Cause A+B — Radical option: drop the forked-lifecycle `clover:instrument`, standardise on `clover:setup`

Both A and B exist **only because `clover:instrument` forks a lifecycle and mutates the project**
(build directory + main artifact) inside that fork. A more decisive fix is to **eliminate the
forked-lifecycle instrumentation entirely** and let the non-forking `clover:setup` goal carry all
instrumentation.

### Why this is attractive under Maven 4
- `clover:setup` (`CloverSetupMojo`) runs in the **main** lifecycle at `process-sources`, **does not
  fork**, and already returns `shouldRedirectArtifacts() = false`. It instruments sources into
  `${build.directory}/clover/src-instrumented` (+ test) and points the project's source roots there,
  which are then compiled normally. It sidesteps the immutable-project problems because it never
  needs to redirect the build directory of a *forked* run, nor reclassify the artifact.
- Removing the fork removes: build-directory redirection (Cause A), main-artifact reclassification
  and the forked `install`/`deploy` that Maven 4 rejects (Cause B), the `clover` lifecycle overlay,
  and the `BuildLifecycleAnalyzer`/`@Execute(lifecycle="clover")` machinery.
- The instrument-in-place-then-install risk (why pollution protection exists) largely goes away
  because we stop producing instrumented main artifacts for install/deploy.

### What "instrumentation taken over by `clover:setup`" means
- Users switch from `clover:instrument` (fork) to a `clover:setup` + normal `test`/`verify` flow
  (which the existing `groovy-eclipse` ITs already exercise via `clover:setup verify clover:clover`).
- `clover:clover` (report), `clover:check`, `clover:log`, `clover:aggregate` etc. stay as-is.
- **Repository pollution protection becomes ENABLED by default** (`repositoryPollutionProtection`
  currently defaults to `false` at `AbstractCloverInstrumentMojo:242`). With `setup`-based
  instrumentation the plugin must **refuse to `install`/`deploy` artifacts that contain instrumented
  code** — fail fast (or skip attaching/installing) if an `install`/`deploy` phase is reached while
  instrumentation is active.

### Two sub-variants
1. **Keep `clover:instrument` for Maven 3, hard-fail on Maven 4.**
   - Detect the running Maven version (or the presence of the new Maven 4 API) at the start of the
     forked-lifecycle mojos (`CloverInstrumentMojo` / `CloverInstrumentTestMojo` /
     `CloverInstrumentInternalMojo` when invoked via the fork).
   - On Maven 4: throw a clear `MojoExecutionException` explaining that `clover:instrument`'s
     forked lifecycle is unsupported on Maven 4 and pointing users to `clover:setup`.
   - On Maven 3: behaviour unchanged.
   - Pros: no behaviour change for existing Maven 3 users; smaller blast radius.
   - Cons: two code paths to maintain; Maven 3 keeps the fragile mutation approach.
2. **Remove `clover:instrument` (and the forked lifecycle) completely.**
   - Delete `CloverInstrumentMojo`, `CloverInstrumentTestMojo`, the `clover` lifecycle overlay
     (`META-INF/maven/lifecycle.xml`), `@Execute(lifecycle="clover")` usages, `redirectArtifact()`,
     `redirectOutputDirectories()`, `BuildLifecycleAnalyzer`/`Maven3LifecycleAnalyzer`, and the
     `swizzleCloverDependencies` machinery if it is only needed for the fork.
   - Single instrumentation path (`clover:setup`) for both Maven 3 and Maven 4.
   - Pros: one clean, Maven-4-native path; deletes the most Maven-version-sensitive code; smallest
     long-term maintenance surface.
   - Cons: **breaking change** for users who depend on `clover:instrument`/`instrument-test`; docs,
     changelog and a major-version bump needed; some IT scenarios (the `-Pwith-clover-instr`
     profiles, `xdoclet`'s forked-lifecycle assertion) must be rewritten or removed.

### Impact on the current failing ITs
- The `groovy-eclipse-*` ITs already have a `clover:setup verify clover:clover` variant that passes;
  their failing variant is the `-Pwith-clover-instr` (fork) one — which would be removed/rewritten.
- `xdoclet` specifically asserts forked-lifecycle behaviour (`target/clover/test.clover`); under this
  approach it would be rewritten to assert the `clover:setup` output location, or retired.

### Follow-ups if this option is chosen
- Flip `repositoryPollutionProtection` default to `true`; add explicit guards so `install`/`deploy`
  with active instrumentation fail fast with an actionable message.
- Update documentation/site: deprecate/remove `instrument` & `instrument-test`, present
  `clover:setup` as the canonical flow.
- Decide major-version bump / changelog entries for the removal (sub-variant 2).

---

## Cross-cutting notes / constraints

- **Immutable model is the crux.** Both causes come from mutating `MavenProject` at runtime. The
  durable fix is to stop relying on runtime mutation and instead express redirections through
  lifecycle mapping/configuration or the new Maven API.
- **Keep the 3.x matrix green.** The compatibility matrix runs 3.3.1, 3.9.2 and 4.0.0-rc-5. Any
  Maven-4-specific fix must not regress Maven 3 — prefer version-gated code paths or an approach
  that works on both.
- **Decompilation setup** (for future sessions): jars under
  `~/apache-maven-4.0.0-rc-5/lib/maven-{core,model,impl,api-*}-4.0.0-rc-5.jar`; inspect with
  `javap -p -c`. Key classes: `org.apache.maven.project.MavenProject` (compat adapter),
  `org.apache.maven.model.{Model,Build,BuildBase,BaseObject}` (mutable wrappers over immutable
  `api.model.*`), `org.apache.maven.plugins.install.InstallMojo` (install validation).
- **Docs consulted:** "What's New in Maven 4" (immutable project/plugin model; JSR-330 required;
  Maven Resolver 2.0 hidden behind API; new `before:`/`after:` lifecycle phases; Java 17+ runtime).

## Strategic choice — two mutually exclusive directions

- **Direction 1 — Preserve the fork, fix it for Maven 4** (Cause A + Cause B fix sections above).
  Keeps `clover:instrument`; more Maven-4-specific plumbing (attach-artifact, lifecycle trim,
  property/lifecycle-based dir redirection, possibly a Maven 4 lifecycle analyzer). Higher ongoing
  complexity but no user-facing breaking change.
- **Direction 2 — Drop the fork, standardise on `clover:setup`** (radical option above). Removes the
  root cause instead of working around it; enable pollution protection by default and never
  install/deploy instrumented artifacts. Sub-variant 2a (Maven-3 keeps `instrument`, Maven-4 hard-
  fails) or 2b (remove `instrument` entirely — breaking, cleanest).

Recommendation to discuss: **Direction 2** aligns best with Maven 4's immutable-project model and
minimises long-term maintenance; choose 2a for a gentler transition or 2b for a clean single path.

## Suggested execution order (if Direction 1)
1. Cause B option 1+2 (attach-artifact and/or trim install/deploy from forked lifecycle) — fixes 3
   of 4 ITs and removes a real repo-pollution risk. Validate with the 3 groovy-eclipse ITs on both
   Maven 3.9.2 and 4.0.0-rc-5.
2. Cause A — decide between property-injection vs lifecycle-mapping; validate with `xdoclet` on both
   Maven versions.
3. Re-run the full `-Pintegration-tests` suite on 3.3.1 / 3.9.2 / 4.0.0-rc-5.

## Suggested execution order (if Direction 2)
1. Enable `repositoryPollutionProtection` by default; add fail-fast guards on `install`/`deploy`
   while instrumentation is active.
2. Sub-variant 2a: add Maven-version detection + hard error in the forked-lifecycle mojos on Maven 4.
   Sub-variant 2b: remove `CloverInstrumentMojo`/`CloverInstrumentTestMojo`, the `clover` lifecycle
   overlay, and the redirect/analyzer machinery.
3. Rewrite/retire the fork-specific ITs (`xdoclet`, the `-Pwith-clover-instr` variants) to use
   `clover:setup`.
4. Re-run the full `-Pintegration-tests` suite on 3.3.1 / 3.9.2 / 4.0.0-rc-5; update docs/changelog.

## Status
- [x] Reproduced all 4 failures on 4.0.0-rc-5.
- [x] Root-caused both categories (runtime probe + decompilation + Maven 4 docs).
- [x] Temporary `[CLOVER-PROBE]` logging removed from `CloverInstrumentInternalMojo`.
- [ ] Fixes not yet implemented (awaiting go-ahead on the chosen strategy).
