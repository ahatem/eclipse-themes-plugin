# Bounded R1 Eclipse runtime verification

These test-only, non-reactor assets verify Eclipse 2024-09 on Windows x86_64 using JDK 21. They are separate from ordinary Maven tests and from product p2 installation acceptance. They do not define a broader product support matrix, visual test suite, or CI policy.

## Inputs and preparation

Supply an existing **p2-enabled Eclipse bootstrap** (its `plugins/` must include the Equinox launcher and p2 director), a **JDK 21** with `java`, `javac` and `jar`, and a **new output directory outside your normal Eclipse workspace, bootstrap, JDK and source tree**. No extra PowerShell modules or PowerShell 7 are required. Scripts run on Windows PowerShell 5.1. Maven prerequisites and commands are in [the test strategy](../../../docs/TEST_STRATEGY.md).

Obtain a p2-enabled Eclipse installation through the usual Eclipse distribution process if you do not already have one. Its version is a bootstrap input, not the version under test. The director resolves the explicit 2024-09 inputs in `baseline.properties` into two new independent profiles: Platform/JDT/PDE with Ant, and platform-only without JDT/PDE. The script reads only the bootstrap's launcher, plugin jars and minimal launch metadata; configuration caches, user preferences and workspaces are created under the new output. Normal Maven tests never invoke these scripts or download a runtime.

Run from the repository root (replace the example input paths with your own):

```powershell
$bootstrap = 'C:\EclipseBootstrap'
$jdk = 'C:\Jdk21'
$output = 'C:\Verification\r1-new-run'
powershell -NoProfile -File tools/runtime-tests/eclipse-2024-09/Prepare-R1Runtime.ps1 `
  -BootstrapEclipse $bootstrap -JdkHome $jdk -OutputDirectory $output
```

Network access to the Eclipse 2024-09 repository and artifact mirrors is required during preparation. Each director process has a 600-second default timeout, adjustable with `-TimeoutSeconds` up to 3600. Cache/network conditions affect preparation time. Root IUs and key consumer versions are checked; transitive artifact reproducibility is reported, not universally promised. Prepared output contains `runtime.json`, independent profiles/bundle pools, relocated bootstrap state and full command/stdout/stderr logs.

All caller paths are resolved and checked before output is created. Existing outputs, source/workspace overlap, bootstrap/JDK overlap and reparse paths are rejected, including new `tmp/` children inside the source tree. Use a directory elsewhere. Scripts do not delete output automatically. A preparation failure leaves identified `failed` state and diagnostics; choose a new directory after addressing the cause. Never relabel a failed directory as prepared or reuse historical `tmp/` installations as passing evidence.

## Build and execute

```powershell
mvn clean verify -B
$product = (Resolve-Path bundles/com.github.eclipsethemes/target/com.github.eclipsethemes-1.1.0-SNAPSHOT.jar).Path
$revision = git rev-parse HEAD
powershell -NoProfile -File tools/runtime-tests/eclipse-2024-09/Verify-R1Runtime.ps1 `
  -RuntimeDirectory $output -JdkHome $jdk -ProductJar $product -SourceRevision $revision
```

Use the actual built jar path for the checkout's version. The verifier checks its bundle identity and JavaSE-17 manifest, records its SHA-256 and caller-supplied revision, and creates a unique run directory. The revision association is supplied with the build record; a manifest/digest alone cannot prove source correspondence. For uncommitted source changes, retain the diff and build log alongside the report.

Each run compiles maintained sources with `javac --release 17`, including only the selected test-owned XML provider/canary helpers, and generates separate present/absent application jars. Product/probe registrations and writable configuration/data/user state are placed in the unique run directory. The prepared profiles remain identifiable read-only launch inputs. There is no manual registration step, product export, extra product dependency or installed update-site acceptance test.

Platform-only may include the JDT **batch compiler** used by Platform/Ant. Absence checks target the actual `org.eclipse.jdt.ui` and `org.eclipse.pde.ui` editor integrations, matching the product's availability contract; a compiler jar is not evidence of an installed Java editor.

The default `All` check runs:

1. `core`: actual packaged activator/catalog/JAXP provider and harmless DTD rejection with zero resolver/connection observations plus positive loopback calibration; repeated reset of dynamic managed-key sets in six real instance scopes, preserving unrelated values/absence, child nodes and lower defaults.
2. `persistence`: a new process/preference lifecycle reads the same disposable core workspace to verify reset removals and unrelated persisted state.
3. `consumers`: unopened SWT viewers on the UI thread verify debug painter RGB, JDT rich/sparse semantic states/new hover, actual PDE/Ant attributes, Boolean delivery/default-valued explicit flags, unsupported-suffix exclusion, the packaged PDE accessor, and existing-service incompatible-access behavior.
4. `absent`: a genuinely platform-only profile proves absent JDT/PDE preconditions, unchanged nodes, real registry/service filtering and continuing text behavior.

`-Check Core`, `-Check Consumers` or `-Check Absent` selects an applicable boundary. Ordinary alias/model changes generally need deterministic tests; see the [boundary table](../../../docs/TEST_STRATEGY.md). Contributors without this Windows baseline can obtain required runtime evidence from a maintainer.

## Outcomes and failure controls

Each launched process has a 120-second default bound (`-TimeoutSeconds`). A timeout terminates only that owned process tree. A passing result requires successful compilation/launch, completed assertions, completion markers and successful process exit. `result.json` lists the exact selection, source label, product digest, baseline and per-check statuses. Full stdout/stderr and workspace logs remain beside it, including on failure. Missing setup, incompatible versions, missing completion and listener exceptions are failures, not skips counted as passes.

For runner maintenance, the consumer probe offers explicit negative controls:

```powershell
powershell -NoProfile -File tools/runtime-tests/eclipse-2024-09/Verify-R1Runtime.ps1 `
  -RuntimeDirectory $output -JdkHome $jdk -ProductJar $product -SourceRevision $revision `
  -Check Consumers -FailureControl Assertion
```

`Assertion` fails after viewer/listener allocation; `Listener` logs a captured exception without suppressing Eclipse logging; `MissingCompletion` exits without the required completion; `Timeout` blocks the disposable probe so a short runner timeout can be checked. Every control must fail. Resource cleanup/display disposal markers distinguish normal/exception cleanup from abrupt process termination. Never report a negative-control run as a passing consumer check.

Failure controls require `-Check Consumers` (or `All`), not `Core`/`Absent`. For the `Timeout` control, use `-TimeoutSeconds 30` and confirm the failed stage is `consumers`; a timeout during compilation does not establish the consumer timeout control.

Run `Test-R1RuntimeScripts.ps1` with the same `-BootstrapEclipse`/`-JdkHome` inputs and another new `-OutputDirectory` to exercise source/workspace/junction rejection, missing tools, incompatible baseline metadata, bounded process termination and failed repository preparation. It changes only owned test fixtures, retains all diagnostics and requires no Pester/module installation. The deliberately unreachable repository exists only in a generated copy of the preparation sources.

SWT resources and listeners created by probes are released on success and assertion failure; borrowed resources remain Eclipse-owned. Runtime consumer internals/reflection are deliberately isolated to this R4_33 test directory. CDT runtime, other OS/releases, Java-17 workbench execution, visual appearance, already-open hover refresh, CI automation, product p2 installation and release acceptance remain outside this evidence.
