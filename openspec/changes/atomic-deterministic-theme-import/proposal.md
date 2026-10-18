# Proposal

## Why

Merged `develop` refuses imports whose **source basename** maps to an occupied or binding-reserved managed locator (PM6-A). That correctly stopped silent overwrites, but it leaves no supported update path and makes future installs depend on a filename that is not identity. The importer also parses the source once and copies it again, so accepted metadata can differ from the bytes actually stored. Removal deletes a file through `File.delete()` without checking the result and leaves its identity binding reserved. Extension classification is inconsistent: the repository accepts `.XML` while the parser only accepts `.xml`, and `ThemeType.from` is locale-sensitive.

Re-verified against current source (see [evidence](evidence.md)):

- **Q4 (High):** the destructive overwrite itself is gone, but validation is not performed against the persisted bytes; there is no update operation, no staging-directory commit, and an interrupted copy can leave a non-theme file that later blocks the locator.
- **Q5 (High):** the basename-replacement divergence between import and reload is closed, but a repeated import has no defined replace-or-coexist policy and no identity-preserving update path, so a user either accumulates duplicates or must remove and re-import, losing the established identity.
- **Q12 (Medium):** `File::delete` ignores failure and binding cleanup is missing; `mkdirs()` is unchecked. Removal can report success while the file remains, and a successful file deletion leaves a stale locator reservation.
- **Q15 (Low):** `.XML` is listed by the repository and rejected by `ParserFactory`; `ThemeType.from` and `nameCapitalized` use the default locale; page search uses the default locale.

This change defines the missing deterministic operation and failure contract. It deliberately keeps theme identity, parsed content, and storage location separate; it introduces no revision IDs, content digests, or trusted source identifiers.

## What Changes

- **New import never overwrites and never infers an update.** A successful import creates a new installed record with a new logical identity at a repository-allocated locator. Source name, `ect-id`, basename, and identical bytes remain metadata, not continuity.
- **Update is a separate, explicit operation on one selected installed theme.** The target is the selected record's established logical identity, not a filename or content match. Bundled themes are read-only and cannot be update targets.
- **Staged validation:** input is copied to a staging file inside the managed area, and the staged bytes — not the mutable source — are parsed and validated before any managed locator, binding, catalog entry, or selection is changed.
- **Deterministic conflict handling:** same-name, same-source-ID, same-basename, identical-content, imported-vs-bundled, and corrupted-binding cases are classified as coexistence, explicit update, refusal, or repair; none silently replaces another record.
- **Minimal atomic guarantees:** a prior valid file is never removed before replacement input is accepted; a replacement is committed with one same-filesystem atomic move or the update fails with the prior file untouched. New-install and removal commit ordering is explicit, with only attempt-created artifacts cleaned up.
- **Truthful removal:** removal is restricted to one bound installed record, removes its binding atomically and its managed file with checked results, preserves unrelated bindings, and reports success only when both are gone. Failure restores the binding when the file remains.
- **Extension and locale determinism:** supported extension matching is ASCII case-insensitive with `Locale.ROOT`, and theme-type classification uses `Locale.ROOT`.
- **Existing installations are not migrated or renamed.** Existing basename-named files keep their locator and identity; only newly imported files use allocated names. Original XML is never rewritten.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `theme-import`: replace the interim occupied-locator refusal with deterministic new-install, explicit-update, staging, replacement, conflict, and removal behavior.
- `theme-catalog`: repository-allocated installed locators, identity-keyed replacement, and truthful catalog admission/removal.
- `preference-ui`: separate Import and Update actions, accurate import/update/removal outcomes, and no success claim before committed state is verified.

## Decision Gate

**Status: APPROVED — unblocked for implementation.** On 2026-10-18 the maintainer reviewed the complete planning package and explicitly approved **PM-I1 Option B, PM-I2 Option A, PM-I3 Option A, PM-I4 Option A and PM-I5 Option A**, together with the missing-managed-file removal clarification and the required conflict contract below. The same instruction authorizes recording this contract in a separate planning commit and then continuing directly to implementation of this change only. The alternatives and rationale retained in this section are historical decision context, not open questions. No blocking Product / Maintainer decision remains unresolved, and the tasks may be implemented against the approved choices.

### Approved resolutions

| ID | Approved | Contract consequence |
| --- | --- | --- |
| **PM-I1** | **B** | Normal **Import** always creates a new independent installed record. Continuity is never inferred from display name, source `ect-id`, source basename/path, equal bytes, or similarity to a bundled theme. A distinct explicit **`Update selected theme…`** operation targets the explicitly selected established installed `local:` record; the replacement source contributes new content only and its metadata never chooses the target. Bundled themes cannot be update targets. This is the minimum UI needed to expose the R2 lifecycle operation and is not authorization for a broader R4 preference-page redesign. |
| **PM-I2** | **A** | An established theme update requires atomic filesystem replacement: one same-filesystem staged candidate and one `ATOMIC_MOVE` + `REPLACE_EXISTING` commit. If atomic replacement is unsupported or fails, the update reports failure and preserves the previous managed file, its binding, its catalog record and selected intent. No non-atomic fallback, no backup/restore replacement machinery, and no delete-then-copy replacement. |
| **PM-I3** | **A** | New installations receive opaque repository-allocated `.xml` managed locators. A new locator is never derived from source basename, display name, source ID, or content. Existing installed files keep their existing locators and bindings: no migration, no bulk rename, no XML rewrite. |
| **PM-I4** | **A** | Every successful normal import creates a new independent record, even when another record has the same display name, the same raw `ect-id`, the same source basename, byte-identical content, equivalent metadata, or content equivalent to a bundled theme. No deduplication and no content hashing/digest infrastructure. Explicit Update is the only replacement path introduced by this change. |
| **PM-I5** | **A** | Removal ordering is: verify exact target/binding → remove exact binding → delete/confirm absence of exact managed file → remove catalog record → report committed success. Binding-removal failure leaves the file untouched and reports failure. File-deletion failure after binding removal restores the exact prior binding and reports failure. Composite failure is reported as such and never claims removal. No journal, quarantine subsystem, undo framework, or transaction manager is authorized. The documented process-crash residual remains accepted and crash-atomic removal is not claimed. |

**PM-I5 clarification — already-absent managed file (approved).** For an explicit Remove request where the selected record is an established `local:` record, its locator is a valid direct managed child, the established binding maps that locator exactly to the selected logical ID, and the managed file is already absent, the file-removal condition is already satisfied. Removal may remove the exact binding, confirm the locator remains absent, remove the catalog record, and report success. This is explicit user-requested cleanup, not silent repair: identity is not lazily regenerated and the file is not recreated. The rule applies only to Remove. For Update, a missing target file is a failure; update must never recreate a missing record under an established identity. If the managed path exists but is not the expected safe target type/state, the operation fails closed rather than treating it as absent. The smallest focused regression locking this distinction is required.

### Required conflict contract (approved)

The classification below is contractual and is the same matrix maintained in [design](design.md#conflict-matrix); the delta specs assert the observable outcomes. No dialog asks the user to resolve metadata coincidences.

| Incoming situation | Outcome |
| --- | --- |
| Same display name | New independent install |
| Same source `ect-id` | New independent install |
| Same source basename | New independent install using different opaque locator |
| Identical bytes | New independent install |
| Duplicate of bundled theme | New independent installed copy |
| Source is itself an installed file | Copy source into staging; original managed record untouched |
| Explicit update metadata resembles another theme | Only selected target updated |
| Bundled update/removal | Refused |
| Missing update target | Refused |
| Missing/mismatched update/removal binding | Refused |
| Unbound existing legacy XML | Lazy catalog admission only; not inferred as update target |
| Reserved locator with absent XML | Existing inconsistent state; never silently reassigned |

### Evidence-dependent (resolved for planning)

| Question | Finding |
| --- | --- |
| Does current import validate the stored bytes? | No. It parses the source, then opens the source again for `CREATE_NEW` copy. A source change between those reads makes committed metadata differ from stored XML. |
| Are Q4/Q5 overwrite defects still present? | The overwrite is refused before mutation, so the original data-loss path is closed. The remaining Q4/Q5 gaps are staged-byte validation, an explicit update/replacement operation, and a defined repeated-import policy. |
| Is Q12 removal still untruthful? | Yes. `File::delete` is unchecked, the binding is retained, and success is shown unconditionally inside the `try`. |
| Is Q15 reproducible? | Yes. `loadInstalledThemes` lowercases for filtering, `ParserFactory` requires exact `.xml`, and `ThemeType.from`/`nameCapitalized`/search lowercase use the default locale. |
| Can locators remain basename-based? | Not for new installs. Any basename policy requires conflict inference from a value the identity contract explicitly rejects. Existing locators can remain for compatibility. |
| Is an identity-derived filename portable? | `local:` contains `:`, which is invalid on Windows. A direct logical-ID filename is therefore not portable without an encoding scheme. |
| Does the planned update primitive work on the primary local platform? | A Windows/JDK 21 probe confirmed `ATOMIC_MOVE` + `REPLACE_EXISTING` replaces an existing file, while a plain move refuses an existing target. Network/overlay filesystems remain unverified and must fail truthfully. |

### Engineering (decided for the proposal)

- Staging and replacement stay in the existing `installed_themes` directory/filesystem; no new bundle, service, database, journal, or dependency is introduced.
- Files are staged under a non-`.xml` name or a non-catalogued subdirectory so reload cannot admit an in-progress file.
- Existing `ThemeIdentityBindings` atomic metadata replacement is reused; a removal operation is added for one key, preserving unrelated keys and their bytes semantically.
- Catalog and selected intent change only after the file and binding commit steps required by the operation; SWT/preference work stays on the existing UI thread.
- Tests extend the existing `ThemeRepositoryImportTest`, `ThemeIdentityBindingsTest`, parser tests, and narrow pre-widget preference seams. No new harness is justified.

### Product / Maintainer decisions (approved 2026-10-18; alternatives retained as historical context)

**PM-I1 — How update intent is established**

| Option | Consequences / trade-offs |
| --- | --- |
| **A — no update operation:** Import always adds a new record; users remove then import to replace | Smallest and fully deterministic, but loses the established logical identity and any selection/customization attachment; poor fit for an explicit update lifecycle. |
| **B — recommended:** A separate “Update selected theme…” action applies to the currently selected installed record | Explicit user intent, no inference from name/source ID/basename/content; preserves logical ID and locator; adds one UI action and one repository operation. Bundled/no-selection/disappeared targets fail closed. |
| **C — prompt to replace on basename match** | Preserves legacy convenience, but a filename match is not identity and can overwrite an unrelated theme; conflicts with the approved identity model. |
| **D — infer update from `ect-id`, name, or identical content** | Most convenient and least safe: duplicate names/claims and forks are normal, so automatic replacement can destroy an unrelated user record. |

Resolution (approved 2026-10-18): **B**. It is the only option that provides update without re-introducing identity inference. Import remains non-destructive; update is a distinct deliberate operation.

**PM-I2 — Replacement fallback when atomic move is unsupported**

| Option | Consequences / trade-offs |
| --- | --- |
| **A — recommended:** Fail the update before mutation when the filesystem cannot atomically replace; keep the prior file | Strong, simple guarantee on local workspaces; update may be unavailable on some network/overlay filesystems, with a truthful diagnostic. |
| **B — backup plus non-atomic fallback** | Broadens filesystem support but admits crash windows where neither the old nor new file is complete, and needs recovery policy/cleanup outside this bounded change. |
| **C — direct non-atomic replacement** | Simple but can destroy a valid prior theme on interruption; violates the requested non-destructive guarantee. |

Resolution (approved 2026-10-18): **A**. New installs need no replacement fallback because they only commit to an unused allocated locator; only explicit replacement requires this decision.

**PM-I3 — Managed locator naming and migration scope**

| Option | Consequences / trade-offs |
| --- | --- |
| **A — recommended:** Repository-generated opaque `.xml` filename per new install; keep existing filenames/bindings unchanged; no migration | Independent of untrusted names, portable, case-collision-free, and no rewrite/relocation of existing user files. Filenames are internal and may be less human-readable in the workspace state area. |
| **B — sanitized source basename plus disambiguating suffix** | More readable on disk, but requires Windows reserved-name/case/path sanitization, collision allocation, and ongoing basename reasoning. |
| **C — rename all existing files to a new scheme** | Uniform storage, but rewrites/moves user-managed state and expands recovery/migration risk for no user-visible benefit. |

Resolution (approved 2026-10-18): **A**. The locator is an internal storage key; existing files stay valid. No migration is proposed.

**PM-I4 — Duplicate/identical imports**

| Option | Consequences / trade-offs |
| --- | --- |
| **A — recommended:** Admit every successful import as a new independent record; duplicates coexist | Deterministic and non-destructive; user can intentionally keep variants or two copies. Does not deduplicate. |
| **B — silently deduplicate by content/source ID/name** | Fewer duplicates, but content/metadata is not identity, and silent deduplication hides which record was admitted or updated. |
| **C — refuse duplicate-looking imports** | Avoids accidental duplicates but blocks legitimate forks and requires a trust/matching policy that does not exist. |

Resolution (approved 2026-10-18): **A**, with the user able to remove unwanted records. Explicit update (PM-I1 B) is the only replacement path.

**PM-I5 — Removal failure/restart guarantee**

| Option | Consequences / trade-offs |
| --- | --- |
| **A — recommended:** Remove the binding first, then the file, then the catalog entry; on file-delete failure restore the binding; report success only if both are gone | No user bytes are deleted before metadata can support them; ordinary failures preserve usability. A process crash between steps can leave the file present and unbound, so it is re-admitted lazily with a new identity rather than lost; that residual is documented, not claimed transactional. |
| **B — delete file first, then binding, restoring the file on binding failure** | Keeps binding until late, but normal binding failure cannot restore deleted bytes; higher data-loss risk. |
| **C — quarantine move plus binding removal plus deferred cleanup** | Better restart cleanup, but adds a journal/repair lifecycle and orphan-management behavior beyond this bounded change. |

Resolution (approved 2026-10-18): **A**. It is the smallest non-destructive ordering; it does not promise crash-atomic removal.

### Deferred

- Application-result truth, rollback of already-applied colors, and recovery: **R3**.
- Coherent Apply/OK/Cancel/defaults, preview fidelity, and offline preview: **R4**.
- Strict validation/diagnostics and source-ID grammar: **R2 `strict-theme-validation-diagnostics`**.
- Immutable validated snapshots: **R2 `immutable-validated-theme-model`**.
- Cross-workspace deduplication, package/archive formats, theme revisions/history, and public import APIs: **R6/R9**.
- Locale-sensitive catalog search and `nameCapitalized` presentation: UI localization follow-up outside this import contract; only extension matching and `ThemeType.from` are included here because they decide whether/how an imported file is accepted.

## Compatibility and recovery

- Existing installed XML files remain at their current locators with their current bindings; no migration, rename, or XML rewrite occurs.
- New imports are additional files. Older plugin versions can still read them as ordinary `.xml` files, but cannot interpret identity bindings; replacing/removing from an older version can desynchronize metadata created by this version. No downgrade guarantee is added beyond readability.
- Same-name and same-source-ID records remain distinct, consistent with the archived identity contract.
- Removal permanently deletes the selected installed theme file only after binding removal. There is no undo; the confirmation must say so. Bundled files are never removal targets.
- No rollback claim is made for colors already applied by any version.
- Under approved PM-I2 A, unsupported atomic replacement is a supported truthful failure, not a silent non-atomic update.

## Impact

Production work affects `ThemeRepository`, `ThemeIdentityBindings`, `ParserFactory`/`ThemeType`, `ThemeManager`, and the import/remove surface of `EclipseThemesPreferencePage`. Tests in the existing bundle test tree change where they assume basename destinations. No new dependency, exported API, OSGi bundle, target-platform change, schema, migration framework, journal, or revision model is justified. JavaSE-17 and current platform scope remain unchanged.
