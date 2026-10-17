# Design

## Context

See [proposal](proposal.md) for motivation and the **approved PM1–PM5 resolution (2026-10-18)**, and [evidence](evidence.md) for discovery and all twelve lifecycle traces. Implementation is authorized after the separate planning commit. Identity crosses parsing, catalog storage and preference references in one runtime bundle; it needs a design because migration/ownership are material, not because it needs more modules.

## Goals / Non-Goals

**Goals:** distinguish independently established themes, preserve a logical reference while its current definition changes, and remove the demonstrated ID-only skip. Retain XML compatibility without trusting its identifiers as globally unique.

**Non-Goals:** detect all disk changes, infer upstream equivalence, settle incoming-file replacement targets, make import transactional, provide application success journaling, or freeze the mutable model. Identity is a foundation, not completion of R2/R3. No authoring/versioning UI, history, package schema, inheritance, overrides, gallery or workbench expansion.

## Decisions

### 1. One established logical ID, source claim kept separately

Approved PM1/PM2: an established theme has a nonblank opaque logical ID. Keep raw `ect-id` separately as optional legacy source metadata; absent/blank is not a logical ID. Preserve its nonblank spelling without a new UUID/URL grammar or case folding. Existing parsed source metadata and tokens remain usable before repository admission, without inventing durable identity in a parser.

Bundled IDs can reuse the existing curated values with a fixed bundled domain. Maintainers retain those values when editing or renaming bundled XML. Do not derive them from bundled paths or labels. Installed themes receive local IDs in a disjoint domain; a simple standard-library-generated opaque value is enough. No generator service, public naming scheme, globally trusted author namespace or separate record-ID field is proposed. Concrete prefix/encoding is internal and must distinguish logical references from old raw source strings.

An installed storage record is the association `logical ID ↔ stored file locator`. Identity belongs to the theme represented by that record, not to its filename: a future supported relocation carries the association; changed contents or label do not regenerate it. Re-importing a file and deciding WHICH association to update is a separate operation/policy. We cannot infer that two imports are one logical theme solely from matching XML, hash, basename, name or `ect-id`.

Why not adopt nonblank `ect-id` for every import? It is cheaper only if duplicate/fork/bundled claims are trusted or resolved now. There is no such contract or corpus evidence. Why not put generated IDs in source XML? That changes documents and requires coupling to replacement machinery. Why not hash content or paths? Edits/moves would change identity and defeat the required future update attachment.

### 2. Identity equality and catalog membership

Approved PM2: index admitted themes by logical identity, sort presentation by existing name order then logical ID, and keep two independent records with the same name or source claim distinct. This removes name-based collapse on **loading existing independent records**; it does not decide import overwrite/merge/reject prompts. The legacy basename import path can still destroy a record before loading; that known hazard remains owned by the next change.

Equality/hash use established logical ID, never current content, label, file, source claim or preview lookup. A parser-only candidate without established identity is equal only to itself, not every other unresolved candidate. Avoid changing token equality/model immutability just to support content comparison. Repository import must return/select the admitted catalog object when one exists, rather than relying on a pre-reload object's source-ID equality. A binding failure is not successful identity admission.

Default actions should resolve the two existing curated bundled IDs rather than a possibly duplicate label (`e541c789-7f90-4539-b1fc-3e086dd8e63e` light, `91d7ea64-57a3-433a-b107-3bc1e1873970` dark). This changes no requested default appearance; missing-default/mode fallback behavior beyond identity disambiguation stays as today and is not a new supported-platform rule.

### 3. Revision alternatives and the smallest correction

| Option | Meets actual requirement? | Cost and conclusion |
| --- | --- | --- |
| A. Stable ID + current digest/revision | Yes, if correctly related to applied content | Requires choosing raw bytes versus parsed/effective content, handling fallback/mapper evolution and remembering what actually applied. No current consumer needs an addressable revision. Not recommended now. |
| B. Stable ID + replaceable current content, no explicit revision field | Yes | Can compare definitions in memory, but does not have to. Structural comparison needs a stable projection despite mutable Theme and Token lacking value equality. D is the smallest specialization of B. |
| C. Immutable revision/history store | Yes | No demonstrated history/rollback/versioning requirement. Duplicates future package/history assumptions. Reject for this change. |
| D. Stable ID + replaceable content, no comparison optimization | Yes, for explicit application requests | Always dispatch selected current definition on Apply/OK, even if its ID matches remembered intent/current object. Reuses existing direct-apply behavior. Recommended PM5. |

An explicit revision ID is **unnecessary**. A digest is **sufficient for bounded content-difference detection only after its input semantics are defined**, not for logical identity, provenance, ordering or proving successful apply. It is **unnecessary here**. The existing corpus test hash is a whole-test fingerprint including names/paths and must not become a production theme revision.

There is no demonstrated need for history, a monotonic counter, user-visible versions or persisted applied-content fingerprints. 'Revision' in R2 can mean the current changed definition; R9's declared revision/digest is a future package decision, not a present dependency. Later import validation can parse a candidate and preserve previous bytes on failure without inventing a history system or comparing revisions.

PM5 deliberately trades repeated preference writes on unchanged OK for a smaller correct dispatch rule. This does not fix swallowed adapter failures, premature active-state commit, Cancel/default semantics or existing external edits; R3/R4 own those. If repeated writes prove materially harmful in implementation, return to the gate rather than silently adding fingerprints.

### 4. Minimal identity persistence and compatibility

Approved PM3: one small plugin-owned binding file in the existing workspace state area, containing only each installed file's current relative locator and logical ID. A flat properties-style map is sufficient; the exact serialization is engineering. It is not a database/schema/migration framework. The locator finds existing bytes; it does not determine the value of the ID. Bundled identity needs no writable metadata. A generated ID is not durably established until this binding has successfully persisted.

Lazy loading of existing stored files is the earliest point that can give them stable reload/restart references. 'Only on the next validated import' avoids a write now but leaves existing accepted missing-ID imports broken indefinitely; record that as PM3 option C, not an implicit compatibility answer. Existing files are parsed using today's acceptance/security rules; this is not broad stricter validation. New import records bind only after the existing parser returns an accepted candidate; this does **not** turn today's copy-before-parse into validated atomic import.

Failure ordering for the narrow binding addition:

1. Read existing bindings and parse candidate XML; do not edit that XML.
2. Reuse an existing valid binding; otherwise allocate an ID and save the minimal binding before exposing it as stable.
3. If saving fails, retain old binding bytes/files and old selected intent, report failure through the existing diagnostic boundary, and withhold only the affected new identity admission. Do not generate a different transient ID and claim success. Unaffected bound/bundled themes remain available.
4. An unreadable/inconsistent binding must not be silently regenerated over; diagnose and preserve it. Implementation should prove this small boundary with an isolated failure fixture; needing a recovery engine returns to ED3/PM3.

A temp file and replace for this small metadata write can prevent destroying the previous binding; it is not the next change's theme-file transaction. No guarantee of atomicity across XML, metadata and preferences is claimed. Metadata loss cannot reconstruct previously generated IDs from content; preserve damaged metadata for diagnosis rather than inventing continuity. Manual deletion/recreation of managed files, arbitrary manual renames of storage and cross-workspace ID transport are outside this supported operation boundary.

Approved PM4: use a distinct internal selected-logical-reference key so an arbitrary old XML string cannot masquerade as a new prefixed ID. Read it first when explicitly present; if absent, consider the old `ACTIVE_THEME_ID` raw value. Resolve the latter only if nonblank and matching exactly one loaded candidate's retained source ID. Count candidates before choosing, including formerly hidden duplicate names. Preserve ambiguous/missing raw intent; do not write a guessed new reference or apply a fallback. A missing explicit selection/default means unresolved intent, not a successfully applied default theme. Existing `currentTheme` terminology remains bounded historical selected-intent handling; no successful-applied-state model is introduced.

On page opening, a resolved intent can select that theme; unresolved intent leaves no automatic fallback selection that OK would then apply. User selection is necessary to make that case actionable. Opening/reload alone never reapplies. A subsequent explicit selection/application writes the new reference through the existing selection flow; keep unrelated preferences and their absence untouched. Do not remove old raw intent as a cleanup operation. Stop-management clears this plugin's new selected reference along with existing selected-intent reset after its existing reset-success gate, so it cannot resurrect a stale new selection. No preference migration may trigger foreign preference writes.

Keep source `ect-id` available to the existing remote preview. Generated local IDs are not website references. Existing remote-preview unreliability stays R4; do not add website calls or reinterpret author IDs to make this change look globally integrated.

### 5. Product-direction sanity checks

| Future scenario, not implemented | Result under recommendation |
| --- | --- |
| Dracula receives upstream content | Updating an explicitly chosen established record retains its ID. Future overrides could remain attached to that ID; no update discovery or merge algorithm is promised. |
| Dracula becomes Dracula Official | Label changes independently of ID, so attachment need not migrate. |
| External XML moves to another path | Installed copy and ID stay unchanged. Incoming-file equivalence is still an import decision. A future managed relocation carries its binding instead of hashing its new path. |
| Website and plugin reference themes | Retained source ID and local logical ID can coexist. A future explicit source association can point to an established theme without replacing all local references. This is not globally portable identity or a synchronization guarantee; R9 must decide its own authority/mapping contract. |

This avoids an obvious name/path/content dead end while explicitly declining speculative global identity infrastructure. Bundled and imported definitions share the meaning of an established logical ID; their read-only versus workspace storage mechanisms need not be identical.

### 6. Adjacent boundaries

| Idea | Classification / owner |
| --- | --- |
| Nonblank established identity, missing/duplicate source-ID distinction, label-independent lookup | **Required now**: Q3 and persisted references; not new validation grammar |
| Durable installed binding and conservative old selection reader | **Required now, PM2–PM4 approved**: restart stability and compatibility; small lazy introduction only |
| Explicit current-definition dispatch despite equal ID | **Required now**: demonstrated OK skip; no revision representation |
| Incoming update/new-install choice, ID claims, basename collisions, content dedup, deterministic replacement and atomicity | **Deferred**: `atomic-deterministic-theme-import`; define target identity before committing, reuse stable binding on an authorized update, then return the canonical admitted record |
| Removal success, file failure diagnostics and metadata cleanup paired with verified removal | **Deferred**: same import/removal change; do not expand this slice into a deletion transaction |
| Required metadata/root, malformed-ID policy, color/style diagnostics | **Deferred**: `strict-theme-validation-diagnostics`; retain legacy source metadata separately |
| Immutable validated snapshot/model | **Deferred**: `immutable-validated-theme-model`; no equality-by-content dependency introduced |
| Actual successful apply marker, transaction/result, rollback/journal | **Deferred**: R3; selected intent remains distinct from proof of applied state |
| Preview/Cancel/accessibility/version UI | **Deferred**: R4; no new diagnostic UI or user-visible revision introduced |
| Package/provenance/semantic roles/contributor API/overrides/inheritance | **Deferred**: R6–R10; no package or public compatibility promise |

## Risks / Trade-offs

- [Legacy ID claims may really denote the same upstream theme] → keep them as metadata; local independence is PM2, not a finding that they are unrelated. Automatic linking and source authority are deferred.
- [Same-name records become visible; import behavior is still unsafe] → explicit PM2 compatibility choice, document interim hazards; do not claim Q4/Q5 solved.
- [New metadata may fail or be lost] → preserve existing state, diagnose, and do not advertise unstable identities. A new migration/recovery requirement returns to the gate.
- [Repeated OK writes and old 'current' markers] → PM5 accepts repeat dispatch; R3 owns success truth. No listener threading change.
- [Downgrade cannot read new reference] → keep XML/locations/raw old intent intact, disclose possible reselection; no automatic color restoration or bidirectional migration framework.

## Migration Plan

After the approved planning commit, introduce source/logical distinction, add lazy bindings, update catalog/reference consumers, then correct explicit application dispatch. Resolve old preferences conservatively. There is no release-time XML conversion, startup appearance application or forced bulk rewrite. Rollback to older code can still load original XML but may recover an older/unresolved selection and require reselection. Do not promise recovery of overwritten imports or original editor settings.

Keep the existing OSGi bundle and JavaSE-17 execution environment. Use public existing state-area/preference/file APIs, not Eclipse internals. No adapter mapping, extension registration, target, exported package or runtime dependency change. Keep preference writes and SWT access on the existing caller/UI thread; no new background preference mutation, SWT resources or browser infrastructure. New metadata I/O remains bounded to this feature; broad asynchronous import work is deferred.

## Verification boundary and proportionality

Approval recording runs strict OpenSpec validations and whitespace checks before its separate commit. Implementation needs focused identity/reload/persistence-failure/old-reference and same-ID-changed-token dispatch regressions using current fixtures; test actual dispatch, not just `equals`. Start with deterministic isolated binding persistence tests. Use an existing bounded runtime probe only if it proves an Eclipse boundary those tests and the existing abstraction cannot; otherwise stop at sufficient deterministic evidence and the normal reactor once. Do not provision a runtime, add a module/harness, or promise OS matrices.

Underengineering check: generated identity must survive restart; duplicate IDs/names must not collapse independent admitted records; ID-only dispatch suppression and old preference ambiguity must be addressed. Overengineering check: no digest/history, public schema, general migration/repository service, global source trust or adjacent import engine is needed. PM1–PM5 are approved; serialization/helper choices alone are engineering. No additional architectural unknown is hidden as an implementation task.
