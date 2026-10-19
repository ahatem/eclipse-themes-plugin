# Design

## Target lifecycle

```text
XML bytes -> secure parse + validation -> Theme (immutable visual definition)
  -> repository admission -> ThemeRecord (logicalId + origin + locator + Theme) -> catalog
```

Application: `ThemeRecord -> record.theme() -> ThemeManager/ThemeService -> EclipseThemeAdapter.apply(Theme, prefs)`.
UI: `ThemeRecord` carries logical identity, origin, lifecycle actions; nested `theme` carries visual metadata/preview/display. No intermediary layer.

## Theme final responsibility

Owns: raw/source XML `ect-id` (legacy lookup/preview only), name, author, website, description, `ThemeType`, immutable semantic tokens, token fallback/resolution behavior. Must not own: logical catalog identity, installed/bundled lifecycle identity, managed file/path/locator, repository binding, catalog-record equality semantics.

## ThemeRecord responsibility

One small catalog entry: `logicalId` (existing `bundled:`/`local:` string format unchanged), `ThemeOrigin`, optional managed locator (installed only), `Theme theme`. Equality, if needed, by `logicalId`. No digest/content identity.

## ThemeOrigin placement

Tiny `enum ThemeOrigin { BUNDLED, INSTALLED }` on the record. Preferred home is the existing Eclipse/repository package (`com.github.eclipsethemes.eclipse`) because origin is a catalog/repository concept, not a visual-core concept; `Theme` stays pure core. Final placement is an engineering choice at implementation time only between that package and `core.models`, with the repository-package option preferred for contributor comprehension. No new package merely for these types; no bundle split.

## Parser construction

Parser builds `Map<TokenKey,Token> tokens = new LinkedHashMap<>()` locally (including the existing PM-V7 root seeding at the same ownership point), resolves winners exactly as today, then `return new Theme(..., Map.copyOf(tokens))`. No public builder unless parser complexity genuinely requires one. `Theme` constructor copies defensively; accessors expose the snapshot, never a live view.

## Token invariants

`Theme` snapshots via `Map.copyOf`; `Token` constructor enforces non-null key/color (matching `TokenBuilder` today); `TokenOptions`/`Color` stay immutable records; fallback resolution (`has`/`get` inheritance) unchanged; parser still guarantees root fallbacks per PM-V7.

## TokenKey closed vocabulary

Public `register` becomes private/internal; static initializers register the fixed core set; duplicate declaration throws immediately; `byId()` stays for parsing/lookup. No enum, no contributor registry, no dynamic production registration.

## PM-V7 preservation

The two `Color.ofRgb(30,30,30)`/`(212,212,212)` seeds remain at `XmlThemeParser` lines 98-101 with a comment marking them accepted compatibility behavior. Corpus fingerprint and sparse-default tests must pass byte-identical. Any future change to these values is a separate compatibility decision, never smuggled into R2.4.

## Catalog migration per operation

- Bundled: parse -> require curated non-blank `ect-id` -> `ThemeRecord("bundled:"+id, BUNDLED, none, theme)`.
- Installed reload: parse -> `identityFor(filename)` (lazy) or established binding -> `ThemeRecord(id, INSTALLED, filename, theme)`; unvalidatable files skipped with diagnostics, files/bindings untouched.
- Import: stage -> validate staged bytes -> allocate locator -> move without replacement -> persist binding -> `ThemeRecord(newId, INSTALLED, locator, theme)` -> catalog. Order unchanged.
- Update: resolve target from record locator + binding -> stage -> validate -> atomic replace -> `ThemeRecord(sameId, INSTALLED, sameLocator, newTheme)` -> catalog replace. Identity/locator preserved.
- Removal: resolve via record identity/origin/locator; binding-then-file ordering and restore guarantees unchanged.

## Outcome, selection, UI, adapters

- `ThemeOperationOutcome` carries the committed `ThemeRecord` (not raw `Theme`) plus unchanged diagnostics semantics; no second result abstraction.
- `ThemeSelection` operates on `List<ThemeRecord>`; persistence keys and legacy fallback unchanged; resolution returns records; `remember` takes a record.
- Page uses `record.origin()`, `record.logicalId()`, `record.theme().getName()`; never constructs absolute managed paths; repository operations receive `ThemeRecord`. Visible UX unchanged.
- Adapters unchanged: `apply(Theme, prefs)`; they consume token resolution, theme type, visual metadata only. No `ThemeRecord` in adapter APIs; no schema change.

## Naming/package discipline

Only `Theme`, `ThemeRecord`, `ThemeOrigin`. No aggregate/entity/DTO/descriptor/snapshot/handle/context/port jargon. No bundle split, no broad reorganisation.
