# Tasks

The three blocking decisions are resolved in `proposal.md` (1A dark only, 2A release on external change, 3A no editor-only writes).
Slice 1 (15 keys) was proven first; the remaining groups were then added to the same table and ownership model.

## 1. Gate and baseline

- [x] 1.1 Record the maintainer's answers to decisions 1–3 in `proposal.md`.
- [x] 1.2 Baseline of the declared keys before any write: the `baseline` steps in `evidence/runtime/` (`pc*-r1.log`).
- [x] 1.3 Classify every stock-Dark preference key and every leftover stock selector (`evidence/key-classification.md`).

## 2. Pure logic (`core/workbench`, no SWT/JFace)

- [x] 2.1 `PerceptualColor.mix`, `PluginColorSource`, `PluginColors.derive(WorkbenchPalette)` (light returns an empty map).
- [x] 2.2 `PluginColorTable` with provenance, one entry per key (`node, key, required bundle, source`), and the `node|key|value` record
  codec that treats malformed input as an empty record.

## 3. Eclipse integration (`eclipse/workbench`)

- [x] 3.1 `PluginColorWriter.sync` with the installed-bundle check, the ownership branches, foreign flush before record save, per-node
  failure handling and a summary result. Only `put`/`remove`/`flush` on named keys.
- [x] 3.2 `WorkbenchCoordinator`: sync after the postcondition (including the unchanged-palette path); release on relinquish and for
  editor-only applies; no sync on a postcondition failure.
- [x] 3.3 `themeid` listener in `EclipseWorkbenchThemeEngine`, registered and removed by `WorkbenchStartup`; startup reconcile releases
  when our theme is not active.
- [x] 3.4 `PreferenceKeys.PLUGIN_COLORS_OWNED`; summary in `WorkbenchOutcome` (DEGRADED on failure); counts in the page message; key
  names only in `WorkbenchDiagnostics`.
- [x] 3.5 Plug-in widget CSS by palette role in the four static sheets, selector for selector with the stock Dark/Light sheets.
- [x] 3.6 Table order: JDT Javadoc keys before the information keys they default to (found on 4.40: releasing the referenced key first
  leaves the dependent live color stale).

## 4. Focused tests

- [x] 4.1 Derivation: dark mapping, light empty, deterministic, readability and tint rules, distinct tints on two dark palettes.
- [x] 4.2 Writer with in-memory nodes: absent/present/owned/changed/mirror-removed, restore, foreign keys untouched, no node cleared,
  bundle not installed, flush failure, record save failure, malformed record.
- [x] 4.3 Coordinator ordering and the listener path (never derives or writes a value).
- [x] 4.4 Consistency: table disjoint from the editor adapters' keys, no duplicate keys, release order, CSS free of preference rules and
  literals, family files identical.

## 5. Runtime verification (4.40 and 4.41, Windows)

- [x] 5.1 One Apply writes the declared keys; dark→dark updates unchanged keys; restart keeps ownership.
- [x] 5.2 User values survive; a changed owned key is released and kept.
- [x] 5.3 Dark→light, disable and external Appearance choice (Light and stock Dark) release owned keys only; record the order of stock's
  writes and the listener.
- [x] 5.4 Save the logs and a README under `evidence/runtime/`, marking what could not be verified.

## 6. Docs and close-out

- [x] 6.1 `docs/WORKBENCH_THEMING_COMPATIBILITY.md`, `docs/target-specs/R8_COORDINATED_THEME_MVP.md`, `docs/ROADMAP.md`.
- [x] 6.2 Archive `dynamic-workbench-theming`, then this change.
- [x] 6.3 `git diff --check`, `openspec validate --all --strict`, one `mvn clean verify -B`.
- [x] 6.4 Proportionality and full-diff review; commit locally.
