# regression-verification Specification

## Purpose
Define reproducible repository verification for R1 safety contracts, distinguishing deterministic behavior tests from evidence that requires real Eclipse consumers.

## Requirements

### Requirement: Verification levels have documented entry points

The repository SHALL document focused development tests, the complete automated regression suite, full reactor verification, and separately invoked real Eclipse verification. Each level SHALL state prerequisites, execution location, outputs, scope proved, and when it is required. Build-JVM requirements SHALL be distinguished from JavaSE-17 product output.

#### Scenario: Contributor selects a verification level
- **GIVEN** a contributor has a clean checkout and the documented prerequisites
- **WHEN** they follow the testing instructions
- **THEN** they can select and execute the appropriate level without developer-specific paths or prior ignored artifacts
- **AND** the instructions distinguish ordinary tests from packaging and runtime evidence

#### Scenario: Runtime setup is unnecessary for a logical change
- **GIVEN** a change affects only deterministic parsing or model rules without altering OSGi provider discovery or editor consumption
- **WHEN** focused and complete automated verification are selected
- **THEN** those levels do not require an Eclipse installation or graphical workbench session
- **AND** applicable full-reactor verification remains a distinct requirement

### Requirement: Automated test selection is explicit and fails truthfully

The normal automated suite SHALL execute all deterministic regression tests without packaging or launching a workbench. Focused selection SHALL identify the tests executed. Assertion failures, execution errors, an empty complete suite, and an unmatched explicit selector SHALL produce failure rather than a successful verification result.

#### Scenario: Complete suite execution
- **WHEN** the documented complete automated command is run
- **THEN** all registered deterministic regression classes execute and produce counts and reports
- **AND** runtime probes are not silently counted as executed

#### Scenario: Mistyped focused selector
- **GIVEN** an explicitly requested test selector matches no test
- **WHEN** focused verification runs
- **THEN** the command fails and identifies the unmatched selection

### Requirement: R1 regressions retain readable behavioral contracts

R1 regression coverage SHALL retain readable assertions for XML security, legacy mapping/fallback/alias behavior, exact adapter mutations, and bounded reset/failure behavior. Digest or fingerprint comparisons SHALL be secondary broad-change detectors; failure SHALL expose a readable projection, and updating expectations SHALL require review of the explained behavioral differences.

#### Scenario: Broad fingerprint changes
- **GIVEN** a corpus or adapter fingerprint differs
- **WHEN** the regression fails
- **THEN** the contributor can inspect the ordered token or mutation projection behind the result
- **AND** a changed digest alone does not authorize replacement of the expected value

#### Scenario: Sparse application followed by bounded reset
- **GIVEN** rich and sparse themes, unrelated explicit preferences, absent keys, and injected persistence failures
- **WHEN** adapter and reset contract tests execute
- **THEN** they assert managed values and absence, preserve unrelated state, reject whole-node clearing, and verify failure diagnostics and reset success gating
- **AND** they do not claim recovery of unknown pre-theme values or transactional application rollback

### Requirement: Deterministic fixtures remain isolated and test-owned

Ordinary tests SHALL use controlled stores and temporary inputs rather than a contributor's Eclipse workspace. Fixture lifecycle SHALL restore modified global test state and close owned files, sockets, and threads on success and failure. Test helpers SHALL remain outside shipped product artifacts and SHALL NOT require new public product APIs.

#### Scenario: Parser warnings or security canaries are exercised
- **WHEN** tests install a logging stand-in, inject a provider fault, or monitor external XML resolution
- **THEN** the original global state is restored and monitors terminate within bounded time
- **AND** fixtures use harmless temporary files and loopback endpoints
- **AND** failure to establish the monitor or its calibration cannot be reported as proof of no external access

### Requirement: Real-runtime verification owns complete reproducible assets

The repository SHALL maintain the sources, test-only launch metadata, and preparation/run instructions needed for the bounded R1 runtime checks. Preparation SHALL create disposable installations and workspaces from explicit inputs. Execution SHALL use the built product and SHALL NOT depend on archived binary probes, hidden classpaths, or manually edited local runtime registrations.

#### Scenario: Runtime verification from a clean checkout
- **GIVEN** the documented caller-supplied p2-enabled Eclipse bootstrap and JDK/tool paths are available
- **WHEN** preparation and runtime verification are invoked
- **THEN** generated probe artifacts, runtime registrations, configurations, and storage are created from maintained repository assets
- **AND** the run identifies the source revision, built product, and resolved runtime/tool versions
- **AND** Windows PowerShell 5.1-compatible runtime entry points remain separate from ordinary Maven testing

#### Scenario: Missing tooling or provisioning failure
- **GIVEN** prerequisites are missing or runtime preparation cannot resolve the declared baseline
- **WHEN** runtime verification is requested
- **THEN** it reports a non-success outcome with the missing input or provisioning cause
- **AND** it does not reuse an unidentified prior workspace or treat an unexecuted check as passing

### Requirement: R1 runtime evidence covers actual consumer boundaries

Runtime verification SHALL cover packaged OSGi parser/provider discovery and catalog loading, real preference-scope reset preservation, extension availability, and the existing debug/JDT/PDE/Ant consumer contracts. Stored preference values alone SHALL NOT establish live listener delivery, and test-injected stores SHALL NOT establish success of the packaged PDE accessor.

#### Scenario: Existing PDE and Ant consumers transition between themes
- **GIVEN** actual consumers attached to their actual editor stores
- **WHEN** explicit XML comments, generic-only fallback, root fallback, new rich styling, and repeated styling are applied
- **THEN** supported colors and bold/italic attributes are verified at the consumers
- **AND** PDE typed true/false delivery, explicit default-valued flags, the packaged accessor, and absence of unsupported suffix notifications are verified
- **AND** unexpected listener exceptions cause failure without suppressing normal logs

#### Scenario: Optional integrations are absent or incompatible
- **GIVEN** an isolated runtime proves JDT/PDE absence or an incompatible PDE access path is exercised
- **WHEN** availability and service behavior are verified
- **THEN** unavailable integrations do not receive mutations and available text behavior continues
- **AND** incompatible access is diagnostic rather than an ineffective substitute-store success

#### Scenario: Parser discovery and preference scopes are exercised
- **WHEN** packaged parser and reset checks run inside the declared runtime
- **THEN** bundled catalog loading and harmless prohibited XML rejection are verified through OSGi loading with provider identity and no external resolution evidence
- **AND** real instance reset removes managed overrides while preserving unrelated explicit values, absent keys, child nodes, and lower-scope defaults

### Requirement: Runtime execution protects contributor state and resources

Runtime checks SHALL confine mutable preference, framework, workspace, catalog, configuration, and user storage to newly generated verification directories. SWT checks SHALL run on the UI thread, release owned resources and listeners on failure as well as success, terminate within a documented bound, and retain unfiltered diagnostic output.

#### Scenario: Probe fails after creating viewers
- **GIVEN** viewers and preference listeners have been created in a disposable runtime
- **WHEN** an assertion or listener fails
- **THEN** owned listeners, viewers, and framework resources are cleaned up and execution returns failure
- **AND** diagnostics remain available and the contributor's normal Eclipse installation/workspace is unchanged

#### Scenario: Caller supplies an unsafe output directory
- **GIVEN** preparation would reuse a contributor workspace, source tree, or bootstrap installation as mutable output
- **WHEN** the paths are validated
- **THEN** preparation fails before mutation rather than deleting or overwriting that state

### Requirement: Verification evidence has an explicit compatibility boundary

The maintained R1 runtime baseline SHALL be Eclipse 2024-09 on Windows x86_64 with a JDK-21 launcher. Runtime evidence SHALL identify exactly what ran and distinguish current results, historical evidence, expected diagnostics, and unexecuted checks. This baseline SHALL NOT imply a new supported-platform promise or replace deferred R5 CI, p2-install acceptance, or OS/version matrices.

#### Scenario: Maintainer reviews test results
- **WHEN** verification is handed off
- **THEN** automated results, reactor results, and runtime results are reported separately with versions and limitations
- **AND** CDT contract tests, JavaSE-17 bytecode, or unopened SWT viewers are not presented as CDT runtime, Java-17 workbench, visual, or cross-OS proof

#### Scenario: A change reaches a runtime boundary
- **GIVEN** a change alters OSGi parser/provider discovery, preference-scope persistence, extension availability, or live consumer delivery
- **WHEN** its R1 verification is completed
- **THEN** applicable real-runtime evidence is required in addition to deterministic assertions
- **AND** contributors may obtain that evidence from a maintainer with the baseline without making Windows runtime tooling mandatory for every local change
