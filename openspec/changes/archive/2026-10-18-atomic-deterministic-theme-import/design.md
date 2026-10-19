# Design

## Scope and invariants

This change completes the import lifecycle left open by `stable-theme-identity-and-revisions`. It applies only to workspace-installed XML themes. Bundled themes remain read-only. It does not change application, rollback, preview, validation diagnostics, or the definition model.

The design preserves four separate concepts:

1. **Logical identity** is the persisted `local:`/`bundled:` catalog key, owned by repository metadata.
2. **Storage locator** is the direct child filename inside `installed_themes`, also the binding key.
3. **Source metadata** (`ect-id`, display name, author, website) describes content and never establishes continuity.
4. **Content bytes** are untrusted input and are validated only after being copied to an attempt-owned staging file.

No operation may derive an update target, dedupe decision, or replacement from display name, source ID, basename, or content equality. That is the core non-destructive rule.

## Current behavior and defect evidence

`ThemeRepository.importTheme(File)` currently:

1. derives `copied = installed_themes/<source basename>`;
2. refuses if the path is not known absent or the locator has a binding (PM6-A);
3. selects a parser by source filename and parses the source stream, constructing the candidate with the future destination file;
4. reopens the source and copies with `CREATE_NEW`;
5. persists a new binding for the destination basename;
6. inserts into `themes` only after the binding succeeds.

That ordering correctly protects an existing destination, but it leaves the gaps this change addresses:

- the candidate is parsed from one read and the stored file is produced by another read, so the stored bytes are not the validated bytes;
- there is no update/replacement operation after the PM6-A refusal;
- the managed directory is created with an unchecked `mkdirs()` and a colliding basename has no deterministic outcome other than refusal;
- `removeSelectedTheme` calls `File::delete`, ignores `false`, does not remove the binding, resets the current theme before knowing removal succeeded, and then shows an unconditional success dialog;
- `Files.list` filters `.xml` case-insensitively, while `ParserFactory` uses exact lowercase `endsWith(".xml")`;
- `ThemeType.from` and `nameCapitalized` use the default locale, and page search lowercases with the default locale.

`ThemeIdentityBindings` already stages properties in the parent directory, flushes a complete file, and performs an atomic replacement. Its `identityFor` both reads and lazily creates, so update/removal need a way to read an established binding without generating one.

## Operation model

The repository exposes three distinct operations under the approved contract (PM-I1 B, PM-I2 A, PM-I3 A, PM-I4 A, PM-I5 A, approved 2026-10-18):

| Operation | Target selection | Managed write | Binding change | Catalog effect |
| --- | --- | --- | --- | --- |
| New import | None; always a new record | Allocate a new unused locator; move validated staged bytes there | Add one new binding | Add one new record |
| Update | One explicitly selected installed record, identified by its logical ID | Atomically replace that record's existing locator | None; identity and locator retained | Replace that logical-ID record's content |
| Remove | One explicitly selected installed record, identified by its logical ID | Delete that record's locator | Remove only that locator's binding | Remove that record |

Bundled themes can be imported (creating an independent installed copy) but cannot be update or removal targets. A new import is never an update, even when all metadata and bytes match an existing record.

The UI exposes this as “Update Selected Theme…”. This is the approved UI exposure of the R2 lifecycle operation (PM-I1 B) and does not authorize a broader preference-page redesign. The repository contract is independent of button placement: it requires an explicit target object and does not infer one.

## Managed storage and locators

- `installed_themes` remains the only managed directory.
- A staging subdirectory `installed_themes/.staging` holds attempt files whose names end in `.part`. Catalog discovery only lists direct children, and additionally requires a supported `.xml` extension, so staged files can never be admitted.
- New installs allocate `<UUID>.xml` (lowercase extension). Allocation loops over fresh UUIDs, checking both `Files.notExists(locator, NOFOLLOW_LINKS)` and `ThemeIdentityBindings.requireUnused(locator)` before use. No name, source ID, or content influences the name.
- Existing basename locators remain valid and are never renamed. The binding key remains the bare filename.
- Update and removal only accept a catalog object whose file is a direct child of the managed directory and whose parent normalizes to that directory. A path outside it is refused before any I/O mutation.

The UUID locator means same-basename imports no longer collide. It also avoids Windows reserved names, `:` from `local:` IDs, non-ASCII normalization differences, case-insensitive filesystem collisions, and traversal from untrusted names. Opaque names are an internal storage detail; display metadata remains in the XML/catalog.


## Staged validation

Both install and update use the same preparation:

1. Validate that the source is a readable regular file with a supported extension (see extension policy).
2. Ensure the managed and staging directories exist, checking that creation succeeded and that the staging path is a directory.
3. Create a unique `.staging/<UUID>.part` with `CREATE_NEW`, copy the source bytes once, and close the stream.
4. Select the parser from the supported extension and parse the staged bytes, supplying the intended final `File` so the admitted `Theme` points at its future managed location.
5. On parse or copy failure, delete only the staging file and return failure. No locator, binding, catalog record, or selection has changed.

The parser reads the staged file, not the source. Consequently the bytes that were validated are exactly the bytes later committed, even if the user or another process modifies the source during the operation. Staging also prevents a partially written attempt from ever being a catalog candidate.

No content digest or revision ID is persisted. The staged-byte guarantee is procedural for this operation, not a new identity mechanism.

## New install commit

After staged validation succeeds and a locator is allocated:

1. Move the staging file to the allocated locator with `Files.move(staged, locator)` **without** `REPLACE_EXISTING`. Java's non-atomic move contract fails when the target exists; unlike `ATOMIC_MOVE`, it does not leave target-exists behavior implementation-specific.
2. Persist the new binding with `newIdentityFor(locator)`.
3. Insert `candidate.withLogicalId(identity)` into `themes` and return it.

Failure handling:

- If the move fails, delete the staging file, report failure, and change nothing. An unexpectedly occupied allocated locator is treated as a retryable allocation failure, never an overwrite.
- If binding persistence fails, the managed file is an attempt-created orphan. Delete it best-effort, report failure, and do not insert a catalog record or return an identity. If deletion also fails, log the orphan path; it is unbound and will be lazily admitted with a new identity on a later reload rather than destroying data.
- A crash after the move but before binding persistence has the same orphan result. A crash after binding persistence before catalog insertion is repaired by the next reload, which reads the binding.
- Cleanup deletes only the exact staging path or the exact locator moved by this attempt. It never deletes a pre-existing file.


## Explicit update

### Preconditions

Before staging, update requires all of the following:

- a non-null target whose `getLogicalId()` starts with `local:`;
- a present target file that is an existing regular file;
- that file's parent, normalized, equals the normalized managed directory;
- the bare filename is the binding key, and `ThemeIdentityBindings` has an **established** binding for it equal to the target logical ID. A missing or inconsistent binding is a refusal, not a reason to generate or repair identity;
- a readable source with a supported extension.

Bundled targets, missing targets, no-selection, paths outside managed storage, and inconsistent bindings refuse before any managed byte is written. Update never creates a missing target: the locator must exist at preflight.

### Commit

1. Copy the source once to a `.staging/*.part` file and parse those staged bytes with the final target `File`.
2. Re-check that the target is still an existing regular file at the same locator. If it is absent, fail without moving the staged file; this narrows the external-deletion window but cannot eliminate a concurrent-race window.
3. Atomically replace the target locator:
   `Files.move(staged, target, ATOMIC_MOVE, REPLACE_EXISTING)`.
4. Assign the established logical ID to the parsed candidate and replace the catalog entry under that same ID.

The binding file is not rewritten; the identity and locator are unchanged. The original XML is replaced, not versioned, copied aside, or merged. The target's display metadata may change because the whole document is replaced.

### Failure behavior

- Parse, copy, or preflight failure leaves the prior managed file, binding, catalog entry, and selection unchanged; only staging is removed.
- `AtomicMoveNotSupportedException` is a supported truthful failure (approved PM-I2 A). The prior file remains and no fallback replacement occurs.
- Any other move failure preserves the prior file; staging is cleaned best-effort.
- Once the atomic replacement succeeds, the update is committed even if later UI refresh fails. There is no rollback copy.
- Concurrency with external tools editing the managed file is not coordinated by this bounded change; the guarantee applies to the repository's own operation and ordinary filesystem failures.

## Conflict matrix

| Situation | Classification | Determined outcome |
| --- | --- | --- |
| New source has the same display name as an installed or bundled theme | Coexistence | New independent `local:` record; both remain selectable. |
| New source has the same `ect-id` as another record | Coexistence; metadata only | New independent record; no source-ID trust or replacement. |
| New source has the same basename as an existing basename-named file | Coexistence | New UUID locator; existing file and binding untouched. |
| New source bytes are identical to an existing record | Coexistence | New independent record; no silent dedupe. |
| New source duplicates a bundled theme | Coexistence | New installed copy; bundled record remains read-only. |
| New import names a file already in managed storage | Coexistence through staging | Source is read into staging; it is never opened for writing; existing managed file unchanged. |
| Update with source metadata matching a different record | Irrelevant to targeting | Only the explicitly selected logical ID is replaced. |
| Update target is bundled | Refusal | No managed write; bundled content unchanged. |
| Update target's file disappeared before preflight | Refusal | Do not recreate it under the old identity; report failure. |
| Update/removal target binding is missing or inconsistent | Refusal | No managed write; diagnostic; no fresh ID is generated over it. |
| Managed file exists with no binding | Lazy admission, not a conflict | On reload it receives one new binding and identity; it is never treated as an update target before admission. |
| Binding reserves a locator whose file is missing | Inconsistent prior state | No new import targets that locator (new installs use UUIDs); affected record is not silently repaired. |
| Allocated new locator unexpectedly occupied | Allocation retry/failure | Choose another UUID or fail; never overwrite. |
| Existing basename file conflicts case-insensitively on a case-insensitive filesystem | Legacy coexistence constraint | No rename; a new install uses a UUID locator, so it does not collide. |

## Removal

Removal has one target: an explicitly selected `local:` catalog record whose file is a direct child of managed storage and whose established binding equals its logical ID. Bundled and unbound/foreign paths are refused before mutation. The confirmation states that the installed theme file is permanently deleted and cannot be restored.

Ordering (PM-I5 option A):

1. Re-read bindings and remove only the target locator key using the same staged-write + atomic replacement used for admission. Refuse if the key is missing or maps to a different identity. Unrelated keys remain semantically unchanged.
2. Delete the managed file with `Files.delete` and observe success. If the locator is already absent, the file-removal condition is already satisfied (approved PM-I5 clarification): confirm absence with `Files.notExists(locator, NOFOLLOW_LINKS)` and continue to step 3. This is explicit user-requested cleanup, not silent repair: the file is never recreated and identity is never lazily re-established for it. If the managed path exists but is not the expected safe target type/state (for example a symbolic link, a directory, or an unexpected file kind), fail closed instead of treating it as absent. This absent-means-satisfied rule applies only to Remove; for Update, a missing target is a failure (see above).
3. Remove the target logical ID from `themes`.
4. Only after steps 1–3 succeed, report removal success and refresh the catalog/selection.

Failure handling:

- If binding removal fails, nothing else changes and removal fails.
- If the managed file was already absent at step 2 and the exact binding was removed, steps 3–4 proceed and removal reports success; the catalog record and binding are gone and no bytes were deleted.
- If binding removal succeeds but file deletion fails, restore the exact binding (staged atomic replacement). If restoration succeeds, the record remains usable and removal reports failure. If restoration fails, report the composite failure; the file is still present and unbound, so a later reload may lazily re-admit it with a new identity.
- If both binding and file removal succeed, catalog removal is in-memory and cannot partially persist; next reload cannot admit the deleted file.
- A crash after binding removal before file deletion leaves the file unbound. On next load it is re-admitted with a new identity: no bytes are lost, but the old identity/selection is not preserved. This is a documented residual, not transactional removal.
- Unrelated files, bindings, catalog records, and a selected theme that is not the target are never touched.

## Binding operations required

`ThemeIdentityBindings` gains narrow established-only operations:

- `establishedIdentityFor(locator)` reads bindings and returns the existing valid identity without generating one.
- `remove(locator, expectedIdentity)` verifies the expected binding, deletes only that key, and atomically replaces the properties file; it must not rewrite or remove other keys.

Both reuse the existing `readBindings` consistency checks and atomic `replace` method. No lock file, journal, database, or cross-process coordination is added. Existing lazy `identityFor` remains for catalog admission; import/update/removal do not use lazy creation to mask missing metadata.

## Failure and atomicity summary

| Operation | Durable commits, in order | Reported failure guarantee | Crash residual |
| --- | --- | --- | --- |
| New import | staged file → final locator move → new binding → catalog insert | No prior bytes/bindings/catalog change; attempt-owned staging/locator removed best-effort | Unbound final file is lazily admitted; no prior data lost |
| Update | staged file → one atomic target replacement → catalog replacement | Prior target preserved when failure is reported; staging removed | Atomic target is prior or new; binding unchanged |
| Remove | binding-key removal → file deletion or confirmed pre-existing absence → catalog removal | On file-delete failure the binding is restored; no success is shown unless the binding is gone and the locator is confirmed absent | File may remain unbound and be re-admitted with a new identity |


## Extension and locale policy

- `ParserFactory` accepts filenames whose final extension equals `.xml` after `toLowerCase(Locale.ROOT)`. It does not use content sniffing or a default locale.
- Catalog discovery already uses `Locale.ROOT`; it remains the same and now agrees with parser selection. `.XML` and mixed-case variants are therefore admitted consistently.
- `ThemeType.from` lowercases with `Locale.ROOT` so imported theme classification is locale-independent. `nameCapitalized` and catalog search remain presentation concerns and are deferred; this change does not rename their behavior.
- The file chooser offers lowercase and uppercase `.xml` filters. A user selecting an unsupported extension is refused before staging with a clear diagnostic. Matching remains ASCII-case-insensitive, so Turkish dotless-I casing cannot change extension acceptance.

## UI and threading

- Import remains the existing action. On success it reloads, selects by returned logical ID, and only then shows success. It never claims success when the returned record is absent after reload.
- Update is a separate action enabled only for a selected installed theme. It invokes the repository update with that exact object, then reloads and reselects by the preserved logical ID. Bundled or no-selection states disable it. The update confirmation states that the installed XML is replaced while its identity is kept.
- Removal invokes repository removal with the selected logical ID/file, and only after success reloads and chooses the next selection. If the removed record was current, reset is attempted after successful removal; a reset failure is reported separately as “theme removed, reset failed”, not as removal failure.
- No file copy, parse, or atomic move is moved onto a background thread in this bounded change. The existing UI-thread behavior is retained because preference/theme listeners may assume it; operations use local files and bounded XML parsing. Moving this work off the UI thread would be a separate responsiveness change with different failure/selection semantics.
- All cleanup and error paths dispose no borrowed JFace resources and create no SWT resources.

## Migration and compatibility

- Existing bound basename files: unchanged locator, binding key, bytes, and logical ID. No migration pass runs.
- Existing unbound supported files: unchanged bytes/location; lazy admission still establishes one binding on reload.
- New files: UUID locators plus new bindings. They are ordinary XML files for older plugin versions, but older versions do not understand the binding semantics; downgrade interoperability is not promised beyond readability.
- Binding property schema and file name remain unchanged. Only additional operations read/remove existing keys.
- A workspace created by this version remains usable if the change is reverted, because XML formats and existing locators are unchanged; new opaque filenames are still discoverable, though identity behavior reverts to the older implementation.
- No revision metadata, content hashes, or source trust identifiers are added, consistent with the archived identity contract.
- Filesystems without atomic replacement: new installs still work because they never replace; update fails truthfully (approved PM-I2 A) rather than writing non-atomically.

## Alternatives considered

| Alternative | Why not chosen |
| --- | --- |
| Keep basename destinations and ask on collision | Requires filename-based intent, blocks legitimate same-name imports, and cannot provide a deterministic new-install contract. PM-I3 B/C retain this complexity. |
| Parse the source, then copy it as today | Validated metadata can differ from stored bytes; source mutation or truncation creates false admission. |
| Store directly at the final locator while parsing | A bad/partial file can become catalog-visible, and new imports would need an overwrite or cleanup ambiguity. |
| Infer update from `ect-id`, name, or content | Reintroduces the identity inference explicitly rejected by the merged model; can destroy unrelated records. |
| Backup old file, then non-atomic replace | Adds recovery state and crash windows without a bounded recovery owner. PM-I2 B. |
| Journal all three artifacts | A real transaction/recovery subsystem is disproportionate for one local file plus one properties file. |
| Use a content digest or revision ID | Explicitly out of scope and unnecessary for deterministic operation selection. |
| Delete the file before its binding | A binding failure after deletion cannot restore user bytes. PM-I5 B. |
| Quarantine removal | Better deferred cleanup, but requires orphan lifecycle/repair policy outside this change. PM-I5 C. |
| Deduplicate identical imports | Content equality is not identity; silent dedupe hides record selection. PM-I4 B/C. |

## Test strategy

All tests stay in the existing bundle test tree and use JUnit 4/TemporaryFolder conventions.

1. **Parser/type contract:** `ParserFactory` accepts `.xml`, `.XML`, and mixed-case variants under a Turkish default locale; rejects other extensions. `ThemeType.from` is independent of default locale. These tests fail before the locale/case change.
2. **New install:** temporary repository imports a valid theme twice under the same source basename. Assert two distinct `local:` IDs, two UUID-named files, source bytes preserved, both bindings present, and no use of the source basename.
3. **New-install failure:** existing tests are updated for checked directory creation and opaque locators. Injected binding failure still admits nothing and removes only the attempt-created locator; a retry succeeds. Parse failure leaves no staged, final, binding, or catalog artifact.
4. **Update:** with a seeded installed record and binding, update from changed valid XML. Assert the same logical ID and locator, new bytes, unchanged binding bytes, one catalog record, and no staging residue. Parse/preflight failure leaves prior bytes and binding unchanged. Bundled target and missing/inconsistent binding refuse. If a package-private replacement seam is needed to inject `AtomicMoveNotSupportedException`, it mirrors the existing binding `replace` seam; no general filesystem abstraction is added.
5. **Removal:** removes exactly one managed file and its binding while preserving an unrelated record/file/binding; reload does not re-admit the removed theme; bundled/non-local targets refuse. A controlled delete-failure seam verifies restoration of the prior binding and failure reporting. No success is asserted before both artifacts are gone.
6. **Conflict matrix:** cover same display name, same `ect-id`, same basename, identical bytes, bundled-name duplicate, and an unbound legacy file. These are repository-level assertions, not UI behavior.
7. **UI:** no SWT widget harness is added. Dialog ordering and labels are reviewed directly; available-platform manual smoke covers import/update/remove. This is proportionate because the changed contract is repository state, and page dispatch seams added earlier do not model native file dialogs or confirmation boxes.

Focused command:

```bash
mvn test -B -pl releng/com.github.eclipsethemes.target,bundles/com.github.eclipsethemes -am "-Dtest=ThemeRepositoryImportTest,ThemeIdentityBindingsTest,ThemeIdentityTest,ParserFactoryTest,ThemeTypeTest"
```

Then, for production-impacting changes:

```bash
mvn clean verify -B
```

No p2/install/runtime matrix is required merely for these internal storage semantics unless implementation changes extension registration or packaging. A one-platform manual preference-page smoke is warranted before release because import/update/removal are user-visible destructive operations; it does not establish cross-platform equivalence.

## Risks and limitations

- **Update unavailable on non-atomic filesystems (if PM-I2 A):** documented truthful failure. This is preferable to an unguarded non-atomic replacement.
- **Removal crash window:** file may be re-admitted under a new identity after a crash between binding removal and file deletion. User bytes are preserved; the old identity/selection is not. Accepted residual for PM-I5 A.
- **External concurrent edits:** no locking; an external process can change or delete managed files mid-operation. The atomic replace still protects the repository's own commit, but no cross-process transaction is claimed.
- **UUID readability:** managed filenames are opaque. Users interact through catalog display metadata; locator names are not a supported identity surface.
- **No dedupe/versioning:** duplicate imports accumulate and updates replace without history. This is intentional and consistent with PM-I4 A and the identity model.

## Proportionality review

- Added: one staging subdirectory, two established-only binding operations, one explicit update path, deterministic removal ordering, and two locale/case corrections.
- Not added: journal, locks, database, schema migration, content hashing, revision model, dedupe policy, background executor, new abstraction layer, dependency, or module.
- Every addition maps to a re-verified defect or an approved identity rule. Verification targets the repository/binding boundary and does not duplicate runtime evidence for untouched UI/platform surfaces.
