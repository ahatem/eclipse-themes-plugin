# Implementation verification

## Baseline (2026-10-13)

Checkout: `fix/xml-parser-security`, HEAD `3d303ee`. Existing `AGENTS.md`,
`openspec/config.yaml`, and approved planning edits preserved. No remote actions.
Read root/runtime/releng instructions, roadmap, config, all apply context artifacts.
PM1 Option A remains approved. The user explicitly authorized this apply request;
prior-turn planning-only wording in artifacts describes the earlier approval pass.

Tools: Maven 3.9.11 (installed Oracle VS Code distribution), Java 21.0.11+10
(Eclipse Adoptium JustJ), Java 17.0.20.1+1 (Temurin). OpenSpec 1.14.1 requires
unsandboxed schema reads here; no CLI/schema modification was made.

Baseline command (`JAVA_HOME` set to JustJ 21):

```text
mvn test -B -pl releng/com.github.eclipsethemes.target,bundles/com.github.eclipsethemes -am -Dtest=XmlThemeParser*Test -Dsurefire.failIfNoSpecifiedTests=false
```

After correcting a test-only TokenKey enumeration compilation error, this ran
4 tests: intended local-injection rejection failed after asserting that the
production parser returned injected background `#13579b` from a temporary file.
No user files or network resources were used. The corpus fingerprint capture
also intentionally failed against its placeholder. Log: `tmp/xml-baseline.log`.

Captured fingerprint fixed before production changes, then the same Maven
command with `-Dtest=XmlThemeParserCompatibilityTest` passed **3 tests, 0 errors,
0 failures** (`tmp/xml-safe-baseline.log`). Metadata, styles, alias precedence,
sparse fallbacks, references, repeated parses and source metadata were verified.
All 33 bundled themes' metadata, explicit-token presence and resolved colors/styles
across canonical TokenKey fields have SHA-256:
`626074e623e76c11348885c24230f6f4d9aafcc8cc6f0c9a8176542ff902d38e`.

Corpus warnings use a test-only EclipseThemes instance with an ILog proxy,
installed/restored by reflection. Production activator/logging code is unchanged.

## Implementation / focused evidence

All required controls are configured and verified before input consumption;
factory discovery is unchanged and builders are local to each parse. Unsupported
control tests cover all 11 factory settings with setter refusal, getter refusal,
and unsafe readback (33 cases), plus builder validation/XInclude readback failures
and mismatches, creation/installation failures, missing processors, checked
configuration and attribute exceptions. Spies verify zero reads, zero parse calls
and one discovery attempt on setup failure. FactoryConfigurationError is wrapped;
arbitrary JVM errors propagate. All three resolver callbacks throw without
supplying content; error/fatal handlers retain their SAX exception identity.
Malformed ordinary XML and I/O are not mislabeled as DTD attacks.

The secured real builder is monitored through a delegating provider spy. Twelve
DTD fixture variants (local/network general and parameter entities, local/network
external DTDs, PUBLIC, empty/internal declarations/defaults, nested entities) fail
on stream, stream+source and file routes, using both injected monitored and public
discovered construction. UTF-16 DOCTYPE input also fails. Resolution callbacks:
**0**. A bounded loopback listener reports **0 connections**, with a positive
connection calibration test. Temporary local canaries plus zero callbacks and
verified controls establish rejection before file resolution, beyond checking
absence of injected content. XInclude XML/text and both schema hint forms neither
retrieve nor incorporate local/network content; zero callbacks/connections, no
schema attached, no validation or new syntax rejection. Resources are closed.

Repository inspection confirms `loadPackedThemes`, `loadInstalledThemes`, and
`importTheme` all delegate to ParserFactory/ThemeParser. No repository changes.
Safe characterization and the complete corpus fingerprint remain unchanged.
README documents all-DOCTYPE rejection, fail-closed providers, ordinary references,
offline flattening, and Q4's existing overwrite/delete limitation without promising
recovery or custom quotas.

Initial clean Java 21 implementation run passed 12 focused tests; a Tycho
incremental test compile had failed to include a previous test helper, resolved
by clean compilation with no harness/dependency changes. Additional processor
failure/handler identity checks brought the final suite to 13 tests.

Final Java 17 focused command (build JVM Java 21, test fork Java 17):

```text
mvn clean test -B -pl releng/com.github.eclipsethemes.target,bundles/com.github.eclipsethemes -am -Dtest=SecureXmlBuilderTest,XmlThemeParser*Test -Dsurefire.failIfNoSpecifiedTests=false -Djvm=C:/Users/cw/scoop/apps/temurin17-jdk/current/bin/java.exe
```

Result: **13 tests, 0 failures/errors/skips**, Java **17.0.20.1+1**, provider
`com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl`.
Log: `tmp/xml-focused-17.log`. ED3's focused behavioral evidence is satisfied;
ED1's Java 17 evidence is satisfied, pending final build-JVM and OSGi verification.

Java 21 final focused command: same clean Maven command above, omitting `-Djvm`.
Result: **13 tests, 0 failures/errors/skips**, Java **21.0.11+10-LTS**, same
JDK Xerces provider. Log: `tmp/xml-focused-21-final.log`. Both providers established
all controls with verified values. JavaSE-17 compilation/runtime contract unchanged.

## Full reactor

`mvn clean verify -B` on JustJ Java 21.0.11+10-LTS passed all five reactor projects.
All **22 tests** passed (13 parser + 9 existing preference/reset tests). Bundle,
feature and p2 repository built; log: `tmp/xml-reactor-verify.log`.

## Bounded Eclipse runtime smoke / ED1 resolution

Ran the packaged product under Eclipse **2024-09** target dependencies in
Equinox **3.21.0.v20240717-2103**, Windows x86_64, JustJ Java **21.0.11+10-LTS**.
Real product activator started; ThemeRepository.reload loaded **33 bundled themes**.
Product parser and helper were loaded through the resolved bundle classloader,
with its OSGi context classloader for JAXP discovery. Provider:
`com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl`; builder:
`com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderImpl`. Every control/readback
succeeded through the packaged helper. Core runtime **3.31.100.v20240524-2010**;
UI workbench **3.133.0.v20240818-1923**.

Three harmless DTD fixtures (local general entity, loopback external DTD, empty
DOCTYPE) failed with retained SAX causes using both public production construction
and the monitored delegating provider seam: **6 failures, no theme returned,
0 resolver callbacks, 0 loopback connections**. Local canary removed. All framework,
workspace, catalog and user storage is under ignored `tmp/xml-runtime-smoke`;
no existing installed theme was replaced. Framework stopped cleanly. This is a
headless OSGi parser/catalog smoke, with no UI or rendering equivalence claim.
No manifest/extensions/target changes require p2 installation or an OS matrix.

Reproduction commands after the reactor (Java 21 tools):

```text
# xml-runtime.classpath is java.class.path from SecureXmlBuilderTest's Surefire XML.
javac -cp <Eclipse-2024-09-org.eclipse.osgi-jar> -d tmp/xml-smoke-classes tmp/RuntimeXmlSmoke.java
java -cp <osgi-jar>;tmp/xml-smoke-classes;bundles/com.github.eclipsethemes/target/test-classes RuntimeXmlSmoke tmp/xml-runtime.classpath bundles/com.github.eclipsethemes/target/com.github.eclipsethemes-1.1.0-SNAPSHOT.jar tmp/xml-runtime-smoke
```

Probe source: `tmp/RuntimeXmlSmoke.java`; output: `tmp/xml-runtime-smoke.log`.
ED1 is **resolved for the planned Java 17/build-JVM/Eclipse provider checks**;
ED3 is **resolved by the monitored security, setup-failure and compatibility tests**.
ED2's unavailable non-bundled corpus remains acknowledged, as approved; no support
boundary or security fallback changed.

## Final verification and diff review

Final review tightened only test evidence: the loopback monitor now drains queued
connections until a quiet timeout even if shutdown precedes listener scheduling;
its calibration drains the response. Builder-failure tests explicitly assert the
original checked exception identity and creation count. Production code did not
change after the passing OSGi smoke.

After those test changes, repeated the documented focused commands, in order:
Java 17 **13/13 passed** (`tmp/xml-reviewed-17.log`), then Java 21 **13/13 passed**
(`tmp/xml-reviewed-21.log`). Both emitted the same provider names and unchanged
bundled-corpus fingerprint. Final `mvn clean verify -B` on Java 21 passed all
five projects and **22 tests, no failures/errors/skips** (`tmp/xml-reactor-reviewed.log`).

Executable paths used (the command arguments above are literal):

```powershell
$env:JAVA_HOME = 'C:/Users/cw/.local/share/apps/eclipse-rcp/eclipse/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_21.0.11.v20260515-1531/jre'
# mvn in the commands above resolves to this installed Maven 3.9.11 executable:
& 'C:/Users/cw/.vscode/extensions/oracle.oracle-java-25.1.0/nbcode/java/maven/bin/mvn.cmd' clean verify -B
# openspec resolves to Node running this installed OpenSpec 1.14.1 entry point:
node C:/Users/cw/.local/share/mise/installs/npm-fission-ai-openspec/1.14.1/node_modules/@fission-ai/openspec/bin/openspec.js validate secure-xml-theme-parsing --strict
node C:/Users/cw/.local/share/mise/installs/npm-fission-ai-openspec/1.14.1/node_modules/@fission-ai/openspec/bin/openspec.js validate --all --strict
```

Strict validation passed the change and **all 9 items** (8 accepted specs + change).
Reviewed tracked diffs and every new production/test/planning file against proposal,
delta, design and tasks. `git diff --check` and no-index whitespace checks for new
files found no whitespace errors (only Git's normal LF-to-CRLF notices).

- Runtime changes are limited to secure builder setup and SAX/configuration
  diagnostics. Theme interpretation code is unchanged; security-safe corpus,
  aliases, styles, defaults and references retain their baseline behavior.
- No preference/repository/import semantics, schema/root/identity/color validation,
  activator cleanup, custom quotas, dependencies, manifests, exports, extension
  registration or target definitions changed. README discloses retained Q4 risk.
- Packaged jar contains SecureXmlBuilder and excludes JUnit, test classes, provider
  spies and canaries. `javap` confirms helper class major version **61 (Java 17)**;
  the manifest still requires JavaSE-17.
- Existing maintainer changes in AGENTS.md/config.yaml are preserved; temporary
  probes/logs/runtime storage remain ignored. No unrelated source churn.
- PM1 remains Option A; ED1/ED3 are resolved within the planned evidence boundary.
  Non-bundled DTD compatibility impact remains unknown/accepted, without a broader
  compatibility or resource-quota claim.

All requested work is ready for maintainer review. The change remains active;
no archive, spec sync, commit, push, PR, release action or new change was performed.
