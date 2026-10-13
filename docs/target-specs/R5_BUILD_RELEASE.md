# R5 Target Spec — Build, Compatibility, and Release Engineering

Traceability: A3, B1-B11, F5, F6 plus audit release/reproducibility/tooling sections.

## Requirements

### Requirement: Build prerequisites are executable and documented
The repository SHALL provide a pinned/reproducible entry path for the Maven/Tycho build and SHALL document build-JDK requirements separately from bundle runtime Java compatibility.

#### Scenario: Clean contributor clone
- WHEN prerequisites are satisfied
- THEN the documented build command matches the enforced toolchain contract

### Requirement: Target resolution is bounded and reviewable
The target platform SHALL use release-specific/pinned inputs sufficient to avoid accidental drift, with explicit integration targets for supported optional editors.

#### Scenario: CDT compatibility claim
- IF CDT is advertised as supported
- THEN CI/runtime validation includes the declared CDT target rather than relying only on bundle-presence logic

### Requirement: PR validation exercises behavior
Accepted production PRs SHALL run meaningful automated behavior tests and packaging checks appropriate to the changed surface.

#### Scenario: Runtime bundle behavior changes
- THEN CI runs the relevant tests before publishing artifacts

### Requirement: p2 installation is smoke-tested
Release-oriented CI SHALL verify that the generated repository can install into a clean supported Eclipse runtime/profile.

#### Scenario: Generated p2 repository
- WHEN smoke installation runs
- THEN missing metadata/dependencies fail CI before release publication

### Requirement: Stable and snapshot publication are separate
Unreleased main/develop artifacts SHALL not silently replace the stable release channel.

#### Scenario: Push to integration branch
- THEN the stable release repository remains unchanged

### Requirement: Release publication uses one validated artifact lineage
A release SHALL bind tag, commit, versions, tested artifacts, and published p2 contents to one traceable build lineage.

#### Scenario: Published GitHub release
- THEN attached archive and stable p2 repository derive from the same validated build artifacts

### Requirement: Version consistency is checked automatically
Maven, OSGi bundle, feature, tag, and release metadata SHALL be validated for consistency.

#### Scenario: Mismatched release version
- THEN CI blocks publication with a clear diagnostic

### Requirement: Reproducibility is measured
The release process SHALL define qualifier/timestamp/archive inputs and periodically compare rebuild outputs rather than merely claiming reproducibility.

#### Scenario: Two clean builds from identical source/input set
- THEN differences are reported and explained according to policy

### Requirement: Release integrity metadata is published
Release policy SHALL define checksums and artifact/dependency/license inventory, and SHALL explicitly state whether signing is configured.

#### Scenario: User verifies a release archive
- THEN a published checksum/inventory exists for the immutable release artifact

### Requirement: CI follows least privilege and pinned automation policy
Workflow write permissions SHALL be scoped to jobs that need them, accepted PR targets SHALL be validated, and third-party actions SHALL follow the project's pin/update policy.

#### Scenario: Normal PR build
- THEN it does not receive release publication permissions unnecessarily

### Requirement: Cache identity includes material build inputs
CI dependency cache keys SHALL include target/toolchain inputs that can materially alter resolution.

#### Scenario: Target definition changes
- THEN the cache identity changes according to policy

### Requirement: Distributed bundle metadata and licenses are complete
Packaged schema/API docs/licenses/notices required by the project and redistributed assets SHALL be present and verified in CI.

#### Scenario: Release artifact inspection
- THEN required schema/license/notices are present in the produced distribution
