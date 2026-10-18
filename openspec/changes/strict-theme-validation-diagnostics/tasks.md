# Tasks

**Status: APPROVED. Execute in order.** The Decision Gate in `proposal.md` records an explicit resolution for
PM-V1 … PM-V7, including the `type` refinement (absent → no finding, present blank/unrecognised → warning) and the
`http`/`https` website allow-list. Implement only the approved option for each gate and do not substitute a
different behavior. If implementation evidence contradicts an approved assumption, stop and return to the Decision
Gate rather than choosing a different behavior silently.

## 0. Reconfirm the approved baseline

- [ ] 0.1 Re-read `proposal.md`'s Decision Gate and confirm every PM-V1…PM-V7 resolution is recorded, including the
  website allowed-scheme list and the exact ERROR/WARNING severities. Stop and ask if any is unresolved.
- [ ] 0.2 Re-verify the planning baseline against current source (`git status`, branch, HEAD, and the evidence
  anchors in `evidence.md`). If any cited behavior has already changed, return to the Decision Gate instead of
  implementing a stale assumption.
- [ ] 0.3 Re-run the bundled-corpus gate for the approved severities (task 5.3) **before** any production edit. If the
  approved rules fail a bundled asset, stop: bundled asset edits are not authorized by this change.

## 1. Introduce the diagnostic contract

- [ ] 1.1 Add one internal `ThemeDiagnostic` value carrying severity, a stable kind string, a human-readable
  message, the source file when known, and the element/attribute involved where applicable, plus the two
  severities. No diagnostic code registry, no numeric codes table, no localization bundle, no public API, and no
  exported package.
- [ ] 1.2 Widen the existing `XmlThemeParser` diagnostic sink from `Consumer<String>` to
  `Consumer<ThemeDiagnostic>` through `ParserFactory.getParserFor(String, …)`, keeping the parser stateless per
  parse. Do **not** change the `ThemeParser` interface signature or retain findings on the parser.
- [ ] 1.3 Add an additive diagnostics accessor to `ThemeParseException`, preserving its existing constructors and
  checked-exception shape. Do not introduce a new exception hierarchy.
- [ ] 1.4 Preserve the existing mapping-conflict **message text** byte-for-byte so the accepted `theme-parsing`
  mapping-diagnostic contract and `XmlThemeMappingTest` continue to assert the same role/source/occurrence/winner
  content. Do not redesign the canonical-over-alias resolution algorithm.

## 2. Implement structural and metadata validation

- [ ] 2.1 Validate the document root element and reject anything other than `colorTheme` with an error finding
  that names the actual root element.
- [ ] 2.2 Require a present, non-blank `name`, with one error finding that names the `name` attribute.
- [ ] 2.3 Apply the approved `type` policy: emit **no** finding when `type` is absent, warn when present but blank
  or not `dark`/`light` case-insensitively, and keep the documented light classification in both cases. Use
  locale-independent comparison.
- [ ] 2.4 Keep `ect-id` optional and non-identity; keep legacy `id` and `modified` recognised and ignored with no
  diagnostic; keep the recognised root-attribute set silent for anything it does not define. Do not add a source-ID
  grammar, format check or required field, and do not change the curated `bundled:<ect-id>` gate.
- [ ] 2.5 Add a focused table-driven parser test covering: wrong root, missing root name, missing/blank `name`,
  absent `type` (asserting **no** finding), unknown `type`, blank `type`, mixed-case `type`, absent `ect-id`, legacy
  `id`/`modified`, and an unrecognised root attribute. Assert both the verdict and that `ect-id` absence never
  produces a finding.

## 3. Implement token, colour and style diagnostics

- [ ] 3.1 Emit a warning finding — without changing the current outcome — for: unknown element name, known token
  with absent/blank `color`, known token whose `color` is rejected by `Color.ofHex`, a non-boolean
  `bold`/`italic`/`underline`/`strikethrough` value, and nested children under a direct child.
- [ ] 3.2 Leave `Color.ofHex` grammar unchanged. Add a parser-level test documenting the accepted grammar
  (`#RRGGBB` with/without whitespace, either case) and the `#RGB` shorthand that existing compatibility tests rely
  on. Do not widen or narrow the accepted forms.
- [ ] 3.3 Leave unrecognised token attributes ignored without a diagnostic. Leave duplicate/canonical-alias
  resolution and its diagnostic exactly as approved.
- [ ] 3.4 Replace the parser's two direct activator log calls with sink findings, and remove nothing else. Do not
  introduce a logging abstraction in `core`, and do not touch repository, adapter or manager logging.
- [ ] 3.5 Add a table-driven parser test covering each warning kind, asserting the finding's severity, element,
  attribute and value, and that the returned theme still reflects the documented fallback.

## 4. Implement website launch policy and presentation

- [ ] 4.1 Add one small pure launchability check (trim surrounding whitespace, require a syntactically absolute
  URI, compare the scheme case-insensitively against the PM-V3 approved list). Keep it free of SWT so it is
  testable without a display. Do not add a URI/URL library, scheme registry, HTTP client or security framework.
- [ ] 4.2 Emit warnings for a blank, relative, malformed, control-character-bearing or disallowed-scheme
  `website`, and never let a website problem reject a theme.
- [ ] 4.3 In `EclipseThemesPreferencePage`, stop concatenating the raw website value into `"<a>…</a>"`. Render the
  value as escaped inert text, and emit link markup only for a value reported as launchable.
- [ ] 4.4 Make the website selection listener refuse to reach `Program.launch` unless the same launchability check
  passes, so the placeholder and any refused value become inert. Do not add launch-failure UI, a browser, or a
   fetch.
- [ ] 4.5 Add parser-level tests for the launchability check (absent, blank, relative, malformed, control
  characters, `https`, `http`, disallowed schemes, mixed-case schemes, `file:`, script-like schemes) asserting that
  no website condition rejects a theme, and a narrow non-widget preference-page test asserting the rendered text
   is inert for a refused value. Do not add UI widget automation for message strings.

## 5. Unify admission consequences across sources

- [ ] 5.1 Make `loadInstalledThemes` apply the same validity rule as `loadPackedThemes` and import, so a blank-name
  installed record is no longer admitted while an identical bundled resource is skipped. Log the reason; leave the
  managed file, its binding and its logical identity untouched.
- [ ] 5.2 Return a small outcome value from repository/manager import and update, shaped like the existing
  `PreferenceReset.Result` (`successful()`, the committed theme, the findings, and a failure description). Do not
  introduce a repository result framework, a status registry, or a global "last error" field.
- [ ] 5.3 Re-run the bundled-corpus gate for the approved severities across all 33 files. Report the count of
  clean, warning-only and failing files with the exact reason for each. If any bundled file fails, **stop and return
  to the Decision Gate**; do not edit the asset.
- [ ] 5.4 Add repository tests: import refused by a validation error leaves no binding, no managed file and no
  catalog record and returns the finding; update refused by a validation error preserves prior bytes, binding,
  identity and selection; a warning-only candidate commits and carries its warnings; installed reload rejects an
  unvalidatable file without disturbing other records; bundled and installed routes agree for identical content.

## 6. Surface the reason in the UI and finish

- [ ] 6.1 Replace the fixed import and update failure sentences with a message built from the operation outcome's
  actionable finding, naming the failed operation. Keep every existing success, confirmation and reselection
  behavior. Add no diagnostics panel, problems view, warning acknowledgement or conflict wizard.
- [ ] 6.2 Update the existing bundle tests that construct `XmlThemeParser` with a `Consumer<String>` to the new
  diagnostic type, and add the tests listed above. Extend the existing test tree only; add no new harness, module,
  or production test-only seam.
- [ ] 6.3 Run the focused parser/repository tests, then `openspec validate strict-theme-validation-diagnostics --strict`,
  `openspec validate --all --strict`, `git diff --check`, and `mvn clean verify -B` because production sources
  change. Confirm the bundled corpus fingerprint test still passes; if the approved rules alter any resolved value
  for a bundled theme, **stop** and return to the Decision Gate.
- [ ] 6.4 Update `docs/CURRENT_ARCHITECTURE.md` for the validation boundary and the moved parser-logging dependency,
  and note any accepted-for-now limitation from PM-V7. Review the diff for under- and over-proportionality, confirm
  no bundled theme asset, identity binding, preference key, dependency, bundle, exported package or target-platform
  change is present, and stop without starting `immutable-validated-theme-model`, pushing, opening a PR, or
  archiving.