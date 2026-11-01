# Contributing to Eclipse Themes

Thank you for your interest in contributing! Bug reports, suggestions, theme submissions and code improvements are all welcome.

## Reporting bugs and suggesting features

[Open an issue](https://github.com/ahatem/eclipse-themes-plugin/issues/new?template=bug_report.md) with steps to reproduce, what you expected vs. what happened, and your Eclipse version and OS. For ideas, use the [feature request template](https://github.com/ahatem/eclipse-themes-plugin/issues/new?template=feature_request.md).

## Building

You need JDK 21 and Maven 3.9.9+. The plug-in itself targets Java 17 (`JavaSE-17`): Tycho 5 needs the newer JDK to build, so don't raise the bundle's execution environment.

```bash
mvn clean verify -B
```

This compiles, runs the tests and creates a local update site in `releng/com.github.eclipsethemes.updatesite/target/repository`, which you can install into a development Eclipse. Tests are plain JUnit and need no Eclipse installation. See [Architecture](docs/ARCHITECTURE.md) for the code layout and where tests live.

## Pull requests

1. Fork and branch from `develop`; PRs target `develop`.
2. One reason per PR: don't mix refactors, formatting, dependency bumps and features.
3. Add a regression test for bug fixes (parser, repository, adapters and apply/reset changes in particular).
4. Make sure `mvn clean verify -B` passes and describe what changed and why.

## Rules that protect users

**Preference safety.** The plug-in writes into preference nodes owned by Eclipse and other plug-ins.
- Never clear a whole foreign node; touch only the keys your operation owns.
- Remember whether a key was originally absent, not just its value.
- Restore a key only if it still holds the value we wrote, so a newer user change is never overwritten.
- Don't mark a theme applied or active until applying actually succeeded.

**XML and import security.** Theme files are untrusted input. Reject DOCTYPE and external entities, fail closed when the parser can't be secured, validate before replacing an installed theme, replace files atomically, and keep theme packages declarative (never execute imported content).

**UI thread.** SWT widgets and preference writes belong on the UI thread; do slow parsing and file I/O elsewhere and marshal results back. Dispose SWT resources you create, never ones you borrow.

**Layering and compatibility.** `core.*` must not depend on SWT, JFace or the activator. Prefer public Eclipse APIs; when an internal key or `x-friends` API is unavoidable, isolate it, say which Eclipse versions it was checked on, and test it. Windows, GTK and macOS render differently, so don't claim equivalence without evidence, and keep high-contrast choices intact.

Details are in [Architecture](docs/ARCHITECTURE.md#behavior-rules).
