# Tasks

Implementation starts only after the maintainer resolves decisions 1 to 3 in `proposal.md`. If an answer differs from the recommendation,
update the specs, design and the tasks marked *(D1)*, *(D2)* and *(D3)* first. Also requires `dynamic-workbench-theming` to be archived.

## 1. Gate and baseline

- [ ] 1.1 Record the maintainer's answers to decisions 1–3 in `proposal.md` and adjust artifacts if needed.
- [ ] 1.2 Reproduce the defect on 4.40: with a dark coordinated theme applied, capture a hover, a Javadoc hover, Java content assist
  with its parameter hint, and a hyperlink. Save the stored values of the 15 table keys (all expected absent) as the baseline in
  `evidence/runtime/`.

## 2. Pure logic (`core/workbench`, no SWT/JFace)

- [ ] 2.1 Add `PerceptualColor.mix` and `PopupColors.derive(WorkbenchPalette)` per design §2. Light returns an empty map *(D1)*.
- [ ] 2.2 Add the `PopupColorKeys` table with provenance (design §1), and the `node|key|value` record codec that tolerates
  malformed input (an unparsable record becomes an empty record).

## 3. Eclipse integration (`eclipse/workbench`)

- [ ] 3.1 `PopupColorWriter.sync` per design §4: installed-bundle check, the four ownership branches, flushing foreign nodes before
  saving the record, per-node failure handling, and a summary result. Only `put`/`remove`/`flush` on named keys.
- [ ] 3.2 `WorkbenchCoordinator`: call `sync(derive(palette))` after the postcondition (including the unchanged-palette path);
  call `sync(empty)` on relinquish *(D3)*; no sync on a postcondition failure.
- [ ] 3.3 `EclipseWorkbenchThemeEngine` registers the `themeid` listener that calls `sync(empty)` when the active theme is not ours
  *(D2)*; `WorkbenchStartup` registers it, removes it on shutdown, and calls `sync(empty)` on reconcile when our theme is not active.
- [ ] 3.4 `PreferenceKeys.POPUP_COLORS_OWNED`; popup summary in `WorkbenchOutcome` (DEGRADED on failure); page message for
  user-held keys; per-key lines in `WorkbenchDiagnostics`.

## 4. Focused tests

- [ ] 4.1 Derivation: dark mapping, light is empty, deterministic, active link at least 4.5:1 on background/surface/raised, fallback to
  foreground.
- [ ] 4.2 Writer with in-memory preference nodes:
  - absent key → write and record; present key → skipped and reported;
  - owned and unchanged → rewritten; owned and changed → released and never rewritten;
  - owned and absent (mirror removed it) → released;
  - restore removes only unchanged keys;
  - unrelated keys in both nodes keep their values, and no node is cleared;
  - JDT not installed;
  - a flush failure records nothing for that node;
  - a record save failure leaves orphans that are treated as user values;
  - a malformed record removes nothing.
- [ ] 4.3 Coordinator: sync only after the postcondition holds; no sync on failure; relinquish releases; dark→light releases;
  unchanged-palette path still syncs; the listener path never calls the derivation.
- [ ] 4.4 Consistency: every table key and node string matches the provenance list; the record codec round-trips.

## 5. Runtime verification (4.40 and 4.41, Windows)

- [ ] 5.1 After Apply, all popups from 1.2 and the hyperlinks show the derived colors without a restart; repeat after a restart.
- [ ] 5.2 Set a user hover background in Colors and Fonts first: it is kept and diagnostics show it as held by the user. Change an owned
  key after Apply: the next Apply and disabling coordination both leave it unchanged.
- [ ] 5.3 Disable coordination: unchanged keys are absent again. Switch to a light coordinated theme: same result.
- [ ] 5.4 Choose stock Light, then stock Dark, on the Appearance page: the plugin's values are removed without a restart; stock Dark's
  values are applied and are not recorded as owned; record the order of the stock writes and the listener (design, Risks).
- [ ] 5.5 Save stored-key dumps and screenshots under `evidence/runtime/` with a README.

## 6. Docs and close-out

- [ ] 6.1 `docs/WORKBENCH_THEMING_COMPATIBILITY.md`: popup and hyperlink rows, the ownership rule, the uninstall recovery note.
- [ ] 6.2 Run `openspec validate plugin-color-coordination --strict` and `openspec validate --all --strict`, then the focused tests,
  then `mvn clean verify -B`.
- [ ] 6.3 Proportionality and full-diff review; no unrelated churn; commit locally.
