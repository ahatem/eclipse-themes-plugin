# R7 findings — workbench theme engine

RESEARCH ONLY · NO PRODUCTION BEHAVIOR CHANGE

GO / NO-GO DECISION GATE: **NO-GO for shipping the requested programmatically coordinated R8a
under the R7 contract's supported-public-API-only criterion.** Own declarative themes work on the
tested Windows and GTK3/X11 platforms. However, the exact target exports the selection API
package with `x-friends` restrictions. This is a support-contract blocker, not a failure of CSS
theme contribution. A is the smallest technically viable candidate if that blocker is resolved.
The strict criterion is R7's own contract; repository policy is softer (see §6), so the
maintainer may reasonably consider a bounded compatibility exception (§24, §30). Full R3 is not a
prerequisite for R7.

**P4 CURRENT-RELEASE GO (Eclipse 4.41 GA, Windows only).** The maintainer approved P4 (one
bounded, version-aware `IThemeEngine` compatibility boundary with a manual Appearance fallback).
The bounded 4.41 verification in §36 found the narrow mechanism viable. This does not authorise
production implementation; R8a/R3 remain unstarted. The 4.33 evidence above is unchanged and
remains 4.33-only.

Revision note (independent review): the runtime findings are 4.33-specific. Later upstream
changes alter some of them (§34), and ecosystem practice is recorded in §35 as de facto practice
only. Sections 12, 14, 15, 20 and 30 carry 4.33 qualifications.

The numbered sections answer the maintainer's requested final-report items. Observations refer
to run sections in [the runtime record](obs/runtime-evidence.txt). [Provenance](obs/README.md)
explains instrumentation, extraction, rejected captures and limitations. Recommendations below
are not approved implementation requirements.

## 1. Starting baseline

Authoritative production baseline: `develop` = refreshed `origin/develop` =
`7ebb3433cbf91a173395740464d034dce1d72855`, `feat: make validated theme model immutable (#28)`.

This invocation found an already-started research branch at `326365c`, with untracked R7
proposal/tasks/evidence and ignored local experiments. It preserved and resumed them rather than
resetting the checkout to manufacture a clean start. Reflog shows the research branch was created
from 7ebb343 and the archive commit followed. Fetch succeeded; origin/develop remained 7ebb343.
No claim is made that a second clean-start switch/pull/archive was performed in this invocation.

## 2. R2.4 archive/promotion

Existing archive commit: `326365c0ccb6ced788497a150555cbf6488515f6`,
`chore: archive immutable validated theme model OpenSpec change`.

The archive is `openspec/changes/archive/2026-10-18-immutable-validated-theme-model/`.
Its six accepted deltas were promoted into editor-integrations, preference-ui, theme-application,
theme-catalog, theme-definition and theme-import. The immutable change is no longer active.
The commit contains only archival moves and spec promotion. Strict validation of all accepted
specs succeeds. Normal archival had already completed before this invocation; it was not
rerun, duplicated or reconstructed manually. No archive-only PR was created.

## 3. R7 branch

`research/workbench-theme-engine-spike`. No stale branch was merged, reused or deleted.

## 4. Research-contract commit

`099b238ad2bc7204a7e9796f0048f1d499e2e7d0`,
`docs: define workbench theme engine research contract`.

It contains `.openspec.yaml`, proposal and tasks. `skip_specs: true` is the CLI-supported
declaration for this documentation/evidence-only change. There are no behavioral deltas. A separate
production design would imply scope that is not authorized; research engineering choices are here
and in the evidence provenance instead.

## 5. Exact tested platform and IDs

| Component | Windows primary | Bounded GTK/Linux |
| --- | --- | --- |
| Repository target | Eclipse 2024-09, `https://download.eclipse.org/releases/2024-09` | Same release, official Platform Linux archive |
| Eclipse build | 4.33.0.I20240903-0240 | 4.33.0.I20240903-0240 |
| Platform bundle | 4.33.0.v20240903-0240 | Same |
| CSS theme bundle | `org.eclipse.e4.ui.css.swt.theme` **0.14.400.v20240424-0956** | Same binary SHA256 |
| Workbench | 3.133.0.v20240818-1923 | Same release |
| Stock theme bundle | 1.2.2500.v20240819-1245 | Same release |
| SWT native bundle | 3.127.0.v20240903-0618, win32 x86_64 | 3.127.0.v20240903-0618, gtk linux x86_64 |
| JVM actually launched | Eclipse Adoptium **21.0.12.1+1-LTS** | Ubuntu OpenJDK **21.0.12.1+1-1-26.04.4-Ubuntu** |
| OS | Windows 11 Pro, host build **26300**, OS API version 10.0.26300; Java reports 10.0 | Ubuntu **26.04 LTS**, WSL2 kernel **6.18.33.2-microsoft-standard-WSL2**, x86_64 |
| Windowing | SWT win32 | GTK **3.24.52**, forced GTK3 / **X11**, `DISPLAY=:0`, through WSLg |
| Accessibility flags during runs | `highContrast=false`, `osDark=true` | `highContrast=false`, `osDark=false` |

The probe compiled with JDK 21 `--release 17`; this is **not a JDK 17 runtime test**. The bundle's
JavaSE-17 requirement remains unchanged. Inherited planning/extract statements claiming JDK 17,
Windows build 26100 or unavailable WSL were superseded by actual measurements.

Theme-bundle SHA256 on both platforms:
`4518cc8f68980a3b8c00c9ea5465b8468a24f72a8fc653b7f3fc592a01ef4370`.
Its source reference is eclipse.platform.ui commit
`d6049bd1eb8f9e5dacd40c3f4ae2eeeecc53f69a`, tag `I20240424-1800`.
ThemeEngine/IThemeEngine/schema were read from the matching source JAR. The existing extracted
ThemeEngine source matched its SHA256; ViewsPreferencePage likewise matched the exact 3.133.0
source JAR. Latest online documentation was only supplementary.

Both engines list these IDs (prefix `org.eclipse.e4.ui.css.theme.`):

| Suffix | Label | Stock base stylesheet on tested OS |
| --- | --- | --- |
| `e4_classic` | Classic | `css/e4_classic.css` |
| `e4_dark` | Dark | Windows `css/e4-dark_win.css`; GTK `css/e4-dark_linux.css` |
| `e4_default` | Light | Windows `css/e4_default_win.css`; GTK `css/e4_default_gtk.css` |
| `e4_preview` | Light (Preview) | Windows `css/e4_preview_win.css`; GTK `css/e4_preview_gtk.css` |
| `high-contrast` | High Contrast | `css/high-contrast.css` |

A adds `spike.probe.theme.a`; C registers `spike.probe.c`. Built-in macOS declarations exist,
but their presence is not runtime evidence. The default ID constant on IThemeEngine is not a
substitute for inventorying real installed IDs.

## 6. API and extension-schema findings

The [binary signature inventory](obs/IThemeEngine-javap.txt) verifies:

| Operation | Exact exposed shape / finding |
| --- | --- |
| registerTheme | `(String id, String label, String basestylesheetURI) -> ITheme`; duplicate ID throws IllegalArgumentException |
| registerStylesheet | `(String uri, String... themeIds)`; empty IDs means global, not disabled |
| registerResourceLocator | `(IResourceLocator, String... themeIds)` |
| getThemes | Returns a list; matching source returns an unmodifiable snapshot |
| setTheme | `(String, boolean)` and `(ITheme, boolean)`, both void |
| restore | `(String alternate)` |
| getActiveTheme | Returns ITheme, potentially null |
| applyStyles | `(Object widget, boolean children)` |
| Other exposed methods | getStyle, addCSSEngine, removeCSSEngine; removing a CSS engine is not unregistering a stylesheet |
| unregisterTheme | **Absent** |
| unregisterStylesheet | **Absent** from interface and inspected ThemeEngine implementation |
| resetCurrentTheme | **Absent** from interface; exists only on `internal.theme.ThemeEngine`: INTERNAL / UNSUPPORTED |

Important qualification: public Java signatures do not establish an unrestricted external API.
The exact manifest exports `org.eclipse.e4.ui.css.swt.theme` with friends limited to
`org.eclipse.e4.ui.workbench.renderers.swt`, `org.eclipse.e4.ui.workbench.swt`, and
`org.eclipse.ui.workbench`. Our bundle is not a friend. `internal.theme` is separately
`x-internal:=true`. Eclipse's [PDE access-rule documentation](https://help.eclipse.org/latest/topic/org.eclipse.pde.doc.user/guide/tools/editors/manifest_editor/access_rules.htm)
classifies nonfriend references as discouraged. The [interface's published Javadoc](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/api/org/eclipse/e4/ui/css/swt/theme/IThemeEngine.html)
does not erase the historical binary's restriction. Suppressing a warning is not evidence of
support. Whether Eclipse intends this interface as supported external API needs upstream
clarification or promotion; this spike does not silently settle that contradiction.

**Policy basis.** The strict "supported public API only" criterion used for the gate comes from
R7's own research contract, not from repository policy. `AGENTS.md` (line 137, Architecture
rules) says: prefer public Eclipse APIs; if an internal API, implementation preference key or
renderer detail is unavoidable, isolate it, document provenance and supported versions, and test
it. That rule permits isolated, documented, version-scoped, tested internal coupling.

**Precedent (PM6).** On 2026-10-17 the maintainer approved a single isolated internal accessor,
`PDEPlugin.getDefault().getPreferenceStore()`, at the optional PDE integration boundary for the
declared R4_33 target (see `docs/EDITOR_MAPPING_COMPATIBILITY.md` and archived
`2026-10-17-correct-editor-preference-mappings`, proposal PM6 Option A and design). A selection
call through `IThemeEngine` would be a comparable, though not identical, request: that precedent
was a non-exported internal class, whereas `IThemeEngine` is a published-Javadoc interface in an
`x-friends` package.

**Formal status of x-friends.** Under Eclipse's Provisional API Guidelines, a package exported
with `x-friends` is formally not API and carries no binary-compatibility promise, whatever its
Javadoc looks like. Upstream action is therefore a **promotion request**, not a request to
"clarify" an existing contract.

**Resolver note.** The default Equinox resolver wires imports of non-friend bundles; friend
restrictions are chiefly a PDE/compile-time access rule. Strict resolver mode may hide such
packages from non-friends. Strict mode was **not tested**; the runtime experiments used the
default resolver only.

The matching extension schema supports `<theme id label basestylesheeturi os ws os_version>`,
`<stylesheet uri os ws>` with nested `<themeid refid>`, resource locators, and themeAssociation.
The [documented extension mechanism](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/extension-points/org_eclipse_e4_ui_css_swt_theme.html)
supports declarative contribution. Constructor source shows themes and associated sheets are
registered before restore. The schema advertises OS/WS filtering; only win32 and GTK3/X11 were
tested. No stable theme-selection command bypass was found in the inspected workbench command
declarations. Opening Appearance is a user-mediated alternative, not one coordinated selector.

IThemeEngine.Events is present, but its matching source explicitly says its event topic/attributes
are not yet API. Do not turn those constants or internal reset methods into product contracts.

## 7. A — own declarative theme

W01/W02/W07/W08 and G03/G04/G05 establish that A is enumerated, selected by ID, reported active,
shown in Appearance, saved with true, and restored on a new process. W08/G05 show editor/view tabs,
trim, status, tree/table and container colours without explicitly naming an internal renderer.

Switching to Dark changes the active ID and removes A's probe RGB values on measured surfaces.
A remains available in the engine and Appearance; switching away does not unregister its
contribution. Switching back reapplies it. B targets Light only in these runs, so Dark is an
unmodified-by-B comparison. The Java engine calls are callable in this normal runtime, subject
to the export restriction in section 6.

Exact prior appearance is **not proven**: stock CSS can change editor preferences, and Windows
Dark captures contain anomalous tab-label glyphs, also present after a Dark restart (W10).
The cause is unisolated; do not attribute it to this plugin or claim a platform-wide defect.
The observed RGB reset is stronger than a method return and weaker than full visual restoration.

## 8. B — stylesheet on an existing theme

B attaches only to `org.eclipse.e4.ui.css.theme.e4_default` (Light). W02/W03/W04 show Tree/Table
and ToolBar receiving `#80cbc4` while the built-in theme ID remains unchanged. W04 starts in that
same persisted Light ID with B still active. W03 switches to Dark, then Light: B disappears on
Dark and returns on Light. G03 also starts in built-in Light with B's tree/table values.

There is no supported unregister-stylesheet operation. No declarative enabled flag is exposed;
the engine snapshots contributions on construction. Leaving B installed means selecting Light
continues to enable it. A plugin flag that stops future calls cannot withdraw that contribution.
Replacing it with counter-CSS is another override, not restoration. No hot bundle uninstall or
registry-mutation cleanup experiment was performed; neither is proposed as an MVP API contract.
“Stop management” would need an explicitly accepted restart/provisioning boundary, a different
theme selection, or a mechanism outside B. That mismatch makes B a poor reversible R8a owner.

## 9. C — runtime registration

W04 registers C and a C stylesheet, selects it and verifies the active ID. Reusing its ID throws
IllegalArgumentException. Repeating stylesheet registration returns normally; matching source
uses List.add without deduplication. Registration must be guarded by the caller and does not
replace an earlier sheet. There is no public remove operation for themes/sheets/resource locators.

On already-active Light, registration plus applyStyles leaves the old tree colour. Reselecting
the same active ID also leaves it unchanged. Switching Dark -> Light parses the new sheet and
changes Tree to `#ce93d8` (W04). applyStyles applies parsed rules; it does not reload the registry
of stylesheet URLs. Do not build an R8b refresh loop around the dirty stale worktree's assumption.

W05 starts with persisted C but no runtime registration: active is built-in Dark and saved
themeid is still C. Registering C after startup and explicitly calling restore selects C.
Registration therefore must exist before restore for seamless restoration; IStartup is too late
to satisfy that ordering automatically. A second explicit restore works in this probe, but is
not evidence for a supported production startup architecture.

W05 missing-resource selection still reports the bad ID active and persists it, with an Error
Log FileNotFoundException (see [diagnostic](obs/missing-css-diagnostic.txt)). Bad CSS/invalid colour
also becomes active and persisted; Tree becomes black in this probe, without a corresponding
caller exception or a new diagnostic observed in the inspected log. Neither method return nor
active ID proves successful rendering. These C limitations do not invalidate A's declarative path.

## 10. Persistence: true versus false

| Run/operation | Active ID | Instance themeid | Restart implication |
| --- | --- | --- | --- |
| W01 clean startup | Dark | absent | OS-dark inheritance supplies effective selection |
| W01 set A,true | A | A | W02 starts A |
| W02 set Dark,true | Dark | Dark | Saved Dark |
| W02 set Light,false | Light | **Dark** | W03 starts Dark |
| W03 set Light,true | Light | Light | W04 starts Light, including B |
| W04 set C,true | C | C | W05 starts fallback Dark because C was not registered |

The exact selection key is `/instance/org.eclipse.e4.ui.css.swt.theme/themeid`. true puts and
flushes it; false leaves the persisted value unchanged. Source shows preference-flush failures
are logged rather than returned as an outcome. A successful ID check does not prove disk durability
on an I/O failure; restart verification is the evidence for the tested successful writes.

The D2 hypothesis is supported for selection persistence: platform restore plus a previous-value
snapshot avoids a custom A startup reapply hook. It is **not a complete restoration design**.
The snapshot must include previous effective ID, raw value and absence. Returning to the prior
Dark ID with true writes explicit Dark; it does not restore the original absent/OS-inherit state.
Preserving absence would require narrowly removing only the owned key under a conflict check and
verifying future restore, or an explicitly approved change to inheritance semantics. That step
was not implemented. Never clear the foreign node.

Preference effects extend beyond themeid: W01/W02 record exact CSS-driven changes/removals in
`org.eclipse.ui.workbench`, `org.eclipse.ant.ui` and `org.eclipse.debug.ui`, including their
`overriddenByCSS` markers and editor/console colours. They come from stock theme CSS/listeners,
not just the probe's selection-key write. Appearance OK also writes renderer UI options, shown
in W07. Do not describe switching the workbench theme as an isolated single-preference mutation.

## 11. Restart results

A: Windows W01 -> W02, GTK G03 -> G04, restored before probe actions.
B: Windows W03 -> W04, built-in Light identity and contribution restored.
C: Windows W04 -> W05, registrations absent, stale saved C, fallback Dark; re-registration plus
explicit restore selects C. Existing user customization is not a substitute for engine registration.
The older local missing-contribution experiment E5d is not needed to establish this C result.

Live selection does not promise all surfaces update. Appearance itself presents a restart warning;
W07 observed its Theme Changed dialog and dismissed it without restarting. Windows glyph anomalies
in live Dark and W10 persisted Dark remain a bounded visual-quality uncertainty, not a solved
restart requirement. No claim is made that restarting fixes them.

W10's persisted Dark comes from **W09** (workspace `ws-coverage`), which repeats W08 on a fresh
workspace and ends in Dark. W09 is omitted from `runtime-evidence.txt` but its full log is
hashed in `obs/log-hashes.csv`; W10 therefore has no preceding run in the extract.

## 12. Postconditions

Every retained explicit setTheme action has a POST line: expected ID, actual ID and equality.
Known IDs match. `unknown.theme` in W04 returns without throwing, leaves C active and keeps C
saved: **match=false**. No tested selection produced a null active theme; the API permits null.
**4.33-specific:** the silent no-op for an unknown ID is 4.33 behaviour. Since a7e91a0fe0
(PR #3854, 4.40) `setTheme(String unknownId, restore)` falls back to the default Light theme and
persists it when `restore` is true (§34). The postcondition check is still required, but the
expected mismatch outcome differs by release.
W05 demonstrates the additional false-positive boundary: active ID can match with unusable CSS.

Future production must distinguish selection intent, active engine ID, persisted ID and actual
supported surface results. It must not persist its own applied-success state before the operation
and required postconditions complete. No new application-result architecture is implemented here.

## 13. Appearance coexistence

W01/W07/G03 show A in the real Appearance combo. W07 chooses Dark: before OK, active and saved
state remain A. Page OK selects Dark, writes the same themeid and presents the restart prompt.
Users can switch away through the platform UI; our product must not immediately force them back.
The observer calls the actual selected page's performOk, not an internal page constructor/API.

Current product code evidence: ThemeSelection remembers selected editor intent in its own
`SELECTED_THEME_LOGICAL_ID`. EclipseThemesPreferencePage and ThemeService have no workbench-theme
reconciliation listener. Appearance does not update that intent. This is an inference from the
current baseline code plus the platform preference observations, **not an integrated runtime test
of the latest product**; the Windows test install's older plugin is disclosed in provenance.
CSS can still affect foreign editor preference colours independently of the selected intent.

Possible observation: public Eclipse preference change listeners for persisted themeid, combined
with a mandatory active-ID recheck at Apply/disable. Preference listeners miss false/transient
switches; the provisional event topic is not an unconditional public contract. Recommended future
rule: newer external selection wins; stop asserting coherence and conservatively relinquish or
report conflict. Never overwrite a newer active/saved choice just to restore a snapshot.

## 14. themeid semantics

It is a persisted request, not universal active-state truth: absent can mean active Dark (W01),
false can mean active Light/saved Dark (W02), missing runtime registration can mean active Dark/
saved C (W05). B changes appearance without changing the built-in ID. The selected CSS theme and
the separate Colors and Fonts theme are different concepts; one does not prove the other.

**4.33-specific:** the "absent means inherit OS" and "persisted ID need not be the effective ID"
statements describe 4.33. Later upstream changes publish the effective `themeid` into
`DefaultScope` (9adfbf7713, 4.41, issue #4168), add a user/config-scope default theme
(a7e91a0fe0, 4.40) and change restore fallback for a missing saved theme (4e127ef1f1, included
by 4.37). See §34; none of these was tested here.

## 15. Current dark-mode heuristic

`EclipseThemesPreferencePage.isEclipseDarkMode()` at baseline reads raw themeid; a substring
`dark` returns true, otherwise it falls back to OS dark unless inheritance is disabled. It does
not first establish the active theme. Thus A's bright probe appearance on this Windows host
still yields true through the OS fallback. Explicit Light also yields true on this host.
On an OS-light host, a dark own-theme ID without that substring would be misclassified.

B retains the built-in ID: Dark continues to match, but arbitrary overriding CSS need not retain
that palette's meaning; Light still suffers the existing OS-fallback problem. The heuristic does
not become correct merely by choosing B.

Smallest future replacement: an explicit light/dark/high-contrast classification for the owned
static IDs and known built-in IDs, based on verified active selection where available; unknown
third-party themes remain unknown. Only use OS inheritance when there is no explicit effective
selection, rather than as a fallback after an explicit Light/custom theme. The selection API
support issue must be resolved first. No luminance engine or new public theme-author API is needed.

**4.33-specific:** the "classification for known IDs" proposal assumes the 4.33 theme set. The
Classic and High Contrast themes were later removed (§34), and master (4.42, 547df13d85) adds a
declarative `isDarkTheme` attribute and `ITheme.isDark()`. On releases that have it, that is a
better dark signal than ID substring or OS fallback, but it is unavailable in the tested 4.33.

## 16–18. Surface/platform matrix

Windows W08 and GTK G05 use the final A stylesheet; G03/G04 add GTK restart and switch-back
evidence. Values are resolved widget values unless explicitly described as visual. macOS is
**NOT VERIFIED** for every row. These are a coverage boundary, not a compatibility promise.

| Surface | Expected selector/property | Windows | GTK3/X11 | macOS | Observable result / limitation |
| --- | --- | --- | --- | --- | --- |
| Workbench shell/background | Shell background-color | verified | verified | NOT VERIFIED | `#f8bbd0`; reverts to Dark `#515658` |
| Editor tab area | .MPartStack, selected/unselected tab gradients | verified | verified | NOT VERIFIED | EditorStack background `#d1c4e9`; Dark `#292929` |
| View tabs | .MPartStack | verified | verified | NOT VERIFIED | Same background on topLeft/bottomLeft/bottomRight/sticky view stacks |
| Toolbars/trim | .MTrimBar, ToolBar | verified | verified | NOT VERIFIED | `#ffe082`; platform Dark values after switch |
| Trees | Tree | verified | verified | NOT VERIFIED | Probe/Tasks tree `#ffcc80`; Dark `#2f2f2f` |
| Tables | Table | verified | verified | NOT VERIFIED | Constructed SWT Table `#b2ebf2`; Dark `#313538`; not every custom table viewer |
| Status area | #org-eclipse-ui-StatusLine | verified | verified | NOT VERIFIED | `#a5d6a7`; Dark `#515658` |
| Sash/container backgrounds | SashForm, .MPart, .MCompositePart | verified for these controls | verified for these controls | NOT VERIFIED | `#f8bbd0`; no general assertion about every sash/renderer |
| Preferences/dialog shell | Shell | partial, W07 | NOT VERIFIED | NOT VERIFIED | Preferences Shell resolves pink; native buttons/combos remain system white/grey. No usable dialog screenshot retained |
| Menus / title bar | Menu, MenuItem / native frame | outside verified CSS scope | outside verified CSS scope | NOT VERIFIED | Remain visually distinct in included shell captures; no menu CSS override test |
| Native controls | Button/Combo etc. | partial observation only | partial observation only | NOT VERIFIED | Untargeted Button retains system colour, not proof that all native widgets reject CSS |
| Browser HTML | no SWT CSS selector for DOM content | excluded; separate white HTML observed W06 | unavailable WebKit | NOT VERIFIED | Browser control background is not document styling |
| Third-party custom widgets | widget-specific | NOT VERIFIED | NOT VERIFIED | NOT VERIFIED | No third-party appearance-plugin/custom-widget matrix |
| Editor contents/syntax | StyledText/editor preferences | outside A coverage | outside A coverage | NOT VERIFIED | StyledText getter white while parent may paint pink; syntax support remains editor-adapter work |

Screenshots: [Windows A](obs/windows-a.png), [Windows live Dark](obs/windows-live-dark.png),
[Windows Dark restart](obs/windows-dark-restart.png), [GTK A](obs/gtk-a.png),
[GTK Dark](obs/gtk-dark.png). Windows Dark tab glyphs are anomalous; table cells/empty areas may
inherit/paint differently from the widget getter. Do not turn a background-colour observation
into a claim about fonts, icons, contrast, selection/hover states or a whole widget hierarchy.

GTK means exactly GTK3/X11 through WSLg. It is not GTK4, native Wayland, an independent Linux
desktop distribution matrix, or macOS. The Linux archive lacked WebKit GTK bindings; browser
construction failed as logged. Installing extra system packages was unnecessary for the required
workbench-surface experiment.

## 19. Unsupported/unclaimed surfaces

Exclude native title bars, native menus and arbitrary dialogs; embedded HTML/browser documents;
arbitrary icons and third-party custom-drawn widgets; editor syntax/content beyond existing
editor integrations; every font/hover/selection/accessibility state not measured. Other Eclipse
versions, JDK 17 runtime, native Wayland, GTK4 and macOS are unverified. “Unsupported” here means
outside the proposed support boundary, not a proof that no specialized implementation is possible.

## 20. Accessibility/high contrast

Both tested displays report highContrast=false. OS accessibility settings were not changed.
W07 selects Eclipse's named High Contrast theme and then A through the engine; A wins. That is
not an OS high-contrast test. It demonstrates that callers cannot assume the engine will protect
an accessibility selection from their programmatic choice.

Exact Appearance source disables its combo when Display.getHighContrast() is true, avoids
persisting the choice in that mode, and handles GTK visibility differently. That is code evidence,
not a measured high-contrast OS session. Custom shell/tree colours and native controls can produce
mixed contrast. Recommended R8a policy for approval: do not take control in detected high contrast
or when High Contrast is the active selection; leave an explicit accessible explanation. Actual
Windows HC and relevant GTK accessibility validation remain a prerequisite to claiming support.

**4.33-specific:** the named "High Contrast" CSS theme referenced above no longer exists upstream
(removed 2025-09, 4.37, issue #3095). From 38d61c0bbe (4.41) the engine keeps OS high-contrast
colours instead of forcing the default theme, but only on the `String` selection path (§34).
The recommended "block takeover" policy therefore must be re-derived per supported release
rather than keyed to a High Contrast theme ID. Not tested on any release after 4.33.

## 21–22. Stale Claude worktree

Read-only inspection: branch `claude/modest-raman-0643a3`, commit
`a6ab335449b5b7cd18a7695da66a77d4fd20f595`. It is **already dirty**: CssThemeAdapter.java is
modified and `org/` is untracked. Both were preserved. No merge, cleanup, deletion or source edit
was made there.

**USEFUL SELECTOR IDEA:** Shell, workbench tabs, trim/toolbars, tree/table and sash targets are a
reasonable research checklist. Direct `.MPartStack` maps to actual CTabFolder controls in W08/G05.
Preserve that small selector/target list in this record, with evidence, rather than salvaging the
Java architecture. `.MPartStack .CTabFolder` assumes a descendant/class relationship not established
by these widgets; the stack is itself the CTabFolder. The `editorss` ID and CTabItem pseudo-selector
assumptions were not validated and are not promoted into a support contract.

**INVALID API DESIGN at committed a6ab335:** lines 156–158 call unregisterStylesheet (absent),
registerStylesheet with a URL instead of the exposed String argument, and resetCurrentTheme
(implementation-only). The current dirty file removes those calls, so it would be inaccurate to
claim they are still present in its working copy. Its replacement globally registers a sheet on
each apply and calls applyStyles; W04 and matching source show why this neither reloads new CSS
into an already-active theme nor provides unregister/idempotency. Its clear path adds another
empty-file registration rather than an ownership-aware withdrawal. These are not product designs
to merge. It also explicitly suppresses restriction warnings, confirming rather than resolving
the export concern.

Recommendation: selector knowledge is worth preserving as these bounded notes; the branch/code
is not. Deletion remains a separate later maintainer authorization, especially because it is dirty.

## 23. Candidate comparison

Qualitative assessments only; no invented numerical score.

| Criterion | A — own declarative theme | B — stylesheet on built-in | C — runtime/generated theme |
| --- | --- | --- | --- |
| Public API support | Supported contribution; programmatic selection has x-friends blocker | Supported contribution; same selection restriction | Callable methods, same restriction; no removal API |
| Startup/restart | Registry loads before restore; verified Windows/GTK | Reloaded with built-in; verified Windows | Registration lost; startup ordering/re-restore needed |
| Reversibility | Switch away resets measured colours; exact restoration/absence still needs policy | Cannot withdraw while retaining target built-in with B installed | Switch away possible; registrations stay in engine |
| Persistence | Platform true works | Same built-in saved ID | Saves ID, not runtime definition |
| Appearance coexistence | Named selectable entry; external switch works | Indistinguishable ID, built-in remains modified | Can enumerate during session; missing on restart unless recreated |
| Runtime cleanup | No need to unregister to stop selecting A; contribution remains available | No supported stylesheet withdrawal | No theme/sheet removal; accumulation risk |
| Per-theme customization | Fixed curated palettes; additional static IDs possible | Fixed modifications to shared built-in ID | Flexible generated palettes, at substantial lifecycle cost |
| Portability | Bounded Windows/GTK3-X11 evidence | Windows plus bounded GTK application evidence | Windows only for runtime path |
| Complexity | Smallest candidate after support gate | Small code, disproportionate disable semantics | More startup, refresh, generated-file and failure complexity |
| R3 ownership burden | Snapshot selection/absence; conflict guard; CSS/editor overlap | Own effects on foreign theme without withdrawal handle | Registration/generated-state lifecycle plus selection recovery |
| R8a suitability | Preferred technical basis; shipping NO-GO pending gates | Not recommended for reversible coordinated MVP | Not recommended |
| R8b suitability | Useful static fallback | Niche additive install, only with accepted semantics | Investigate only after demonstrated per-theme need/API lifecycle solution |

## 24. GO / NO-GO verdict

**NO-GO under the R7 contract's supported-public-API-only criterion; resolve the support contract
before programmatically coordinated R8a.** Contribution, selection, active verification, restart
persistence and useful surfaces are demonstrated. Unrestricted external selection API support is
not: the exact bundle's friend export means the package is formally not API (§6). The existing
public Java interface's presence is not enough to waive that criterion. No
cast/reflection/internal reset is recommended as a workaround. This verdict follows R7's own
strict criterion, not repository policy: `AGENTS.md` line 137 permits an unavoidable internal
dependency if isolated, provenance- and version-documented, and tested, and the maintainer
approved exactly such an exception for PM6 on 2026-10-17 (§6). An independent review agreed that
no unrestricted supported selector exists and recommends putting a bounded compatibility
exception to the maintainer.

A remains the smallest viable mechanism if upstream promotes the package (a promotion request, not
a clarification) or the maintainer explicitly accepts bounded provisional coupling, isolated and
pinned to verified releases as in PM6. That latter choice relaxes the R7 gate; it is not a GO
under the original criterion, and it is a maintainer decision, not made here. A purely
manual Appearance contribution could avoid our own selection call but reduces the coordinated
product scope. Visual/accessibility/restoration decisions below still need approval and focused
verification before shipping, even after the API issue is resolved.

## 25. Is R3 required before R7?

**No.** R7 is completed research using disposable isolated state. Building journals, rollback,
change-plan classes or startup reconciliation first would have been unnecessary. A later coordinated
product does need a small truthful-application/selection-restoration slice before R8a; that is not
equivalent to implementing the full R3 program before learning which surface to support.

## 26. Exact minimal slice before R8a, conditional on resolving NO-GO

1. Resolve the external selection API contract and approve the precise version/platform boundary.
2. Separate selected editor intent from verified applied state; propagate enough existing editor
   application failure information to avoid a false combined-success result. No general plan framework.
3. Add one workbench-selection boundary with active/saved postconditions and a snapshot of previous
   effective ID plus raw value/absence. Preserve only the operation's owned selection key; never clear
   foreign nodes. Define absent-value and unavailable-previous-theme behavior explicitly.
4. Conservatively check both active and saved state before restoration. External changes win; on
   disagreement, report conflict/require an explicit recovery choice instead of claiming restoration.
5. Declare the supported surfaces; select on Apply/OK only, honor Cancel, avoid live list navigation;
   approve restart/accessibility behavior. Verify the coordinated editor+CSS ordering against measured
   foreign preference overlap. Validate final static CSS on Windows/GTK and investigate the observed
   Windows tab-label anomaly before a visual-quality claim.

These are bounded future acceptance concerns, not implemented APIs/classes or an approved new change.

## 27. Proposed R8a scope

After the gates: own static, named light/dark workbench palettes coordinated with existing editor
ThemeType on Apply/OK. Limit coverage to tested editor/view tab backgrounds, trim/toolbars, status,
and explicitly selected workbench container/tree/table surfaces. Scope CSS to avoid inadvertently
styling editor contents or writing new editor-token preferences. No new theme-author fields,
generated CSS, imported executable themes or global stylesheet additions to built-in IDs.

Expose actual selection/support state, preserve accessibility choices, and make disable conservative.
This proposes coherent static palette families, not exact generated matching for every editor theme
or every IDE pixel. The number/palette policy remains a maintainer decision, not an R7 implementation.

## 28. R8b runtime/generated CSS

Defer it. Require a concrete unmet per-theme palette need, an accepted external API contract,
startup/restore ordering, replacement/idempotency strategy, bounded file/resource ownership and
observable CSS failures. registerStylesheet + applyStyles alone is not that strategy. Do not use
global counter-styles, repeated registrations or internal reset calls to simulate cleanup.

## 29. Roadmap compression recommendation

Keep R7 ahead of full R3. Resolve the support-contract gate, then take the small application-truth /
surface-support / selection-restoration slice, then a bounded declarative R8a. Reconsider R8b only
with demonstrated need; expand R3 ownership/recovery when actual surfaces justify it. Do not collapse
all roadmap work into one architecture change. ROADMAP.md was not edited.

## 30. Remaining maintainer decisions

| Decision | Realistic options / consequences | Recommendation |
| --- | --- | --- |
| API/support blocker | Seek upstream promotion of the x-friends package (a promotion request, since it is formally not API; delays implementation); explicitly accept a bounded, isolated, version-pinned compatibility exception on the PM6 model, consistent with `AGENTS.md` line 137 (relaxes R7's own criterion, not repository policy); manual Appearance-only scope (loses coordinated selector) | Put the bounded compatibility exception to the maintainer; file the promotion request in parallel; keep R8a unapproved until decided. Any exception must be scoped to verified releases (§34 shows 4.33 behaviour is not stable across releases) |
| Mechanism after gate | A owns selectable identity; B cannot withdraw from built-in; C adds startup/refresh burden | A; no B/C in MVP |
| Support boundary | Tested 4.33 Windows/GTK3-X11 only; broader versions/platforms require evidence (post-4.33 semantic changes are listed in §34 and untested) | Start with that explicit verified boundary; no macOS/Wayland/GTK4 claim |
| Palette scope | Two static light/dark families; single fixed palette; per-editor generated matching | Static light/dark families; generation deferred |
| Original absence / missing previous ID | Preserve absence with bounded key restoration; leave explicit prior ID (changes OS inheritance); ask recovery when previous ID is unavailable | Preserve absence; explicit user choice on unavailable/conflicting state, no invented fallback |
| External/transient switch | Force product theme back; preserve external selection and relinquish/report conflict; prompt before restoring | Preserve external choice, report truthful disagreement; never auto-force |
| High contrast | Block takeover; explicit warning/opt-in; override automatically | Block takeover of detected/named high contrast; verify actual OS modes before support claim. 4.33-specific: the named High Contrast theme was removed upstream in 4.37, so re-derive per supported release (§20, §34) |
| Restart/visual quality | Promise live completeness; disclose platform restart boundary; require restart for known incomplete cases | Disclose bounded live coverage, investigate Windows glyph observation, require only evidence-based restart behavior |
| Stale worktree deletion | Preserve; archive/delete later with review of dirty contents | Preserve now; obtain later explicit approval |

D1–D7 remain locked. No future choice in this table is silently approved by the spike.

## 31. Commits

- Existing `326365c0ccb6ced788497a150555cbf6488515f6`: R2.4 archive/promotion.
- `099b238ad2bc7204a7e9796f0048f1d499e2e7d0`: R7 contract.
- Final findings/evidence commit: HEAD at handoff; its SHA is reported in the final response
  (a commit cannot include its own hash).

## 32. Verification and final state

Passed: `openspec validate workbench-theme-engine-spike --strict`,
`openspec validate --all --strict` (**10 passed, 0 failed**) and `git diff --check`.
Full scope review found only the archive/promotion and R7 research record. No Maven reactor run:
committed work is documentation/OpenSpec/evidence only. Exact post-commit git status and the final
evidence commit SHA are reported at handoff. The R7 change stays active
for independent review, not archived as approved implementation. Ignored local probe state is
retained; no unknown worktree, branch or personal workspace was removed.

Proportionality review: all required mechanisms were investigated; unmeasured accessibility and
platform limits are explicit. No production architecture, new module, public adapter, generic
test infrastructure or extra static-analysis tool was introduced. Evidence stops at the research
decision and does not attempt to fix unrelated platform rendering or implement the recommendation.

## 33. Production/remote action confirmation

No production plugin.xml, MANIFEST.MF, source, build or roadmap changes are committed. No probe
implementation is committed. No R3 or R8a implementation, push, PR, tag, merge or release occurred.
Only the existing archive/promotion and the research contract/findings/evidence belong to this branch.

## 34. Post-4.33 upstream changes

Source: `eclipse.platform.ui` master, checked 2026-10-18. Commit existence and titles were
re-confirmed against the GitHub API during this revision; behavioural descriptions are from the
independent review's reading of the source and were **not** reproduced by an experiment. No
experiment in this record ran on any release after 4.33. All R7 runtime observations stay
4.33-specific (§5).

| Change | Commit / reference | Shipped in | Effect on R7 findings |
| --- | --- | --- | --- |
| Bundle export unchanged | master `org.eclipse.e4.ui.css.swt.theme` 0.15.300, JavaSE-21 | master | Package is still `x-friends`; `IThemeEngine.java` is byte-identical to 4.33. The §6 blocker is not removed by upstream. |
| High Contrast CSS theme removed | issue #3095 | 4.37 (2025-09) | The `high-contrast` ID in §5 and the §20 HC-selection experiment do not apply to 4.37+. |
| Restore fallback for a missing saved theme | `4e127ef1f1` | included by 4.37 | Starting with `4e127ef1f1`, `restore()` distinguishes an unavailable saved theme from having no saved theme. For an unavailable saved ID, it uses whether that ID contains `"dark"` to choose Dark versus the supplied alternate theme. OS-dark inheritance remains the fallback only when no saved theme ID exists. The 4.33 W05 result (Dark fallback for an unregistered saved C on an OS-dark host) is therefore version-specific; this does not mean every missing-theme restore becomes Dark. |
| Classic theme removed | `dbc4595ef7` | 4.40 | `e4_classic` in §5 is absent. |
| User/config-scope default theme | `a7e91a0fe0` (PR #3854) | 4.40 | `setTheme(String unknownId, restore)` falls back to the default Light theme and persists it when `restore` is true. It is no longer the silent no-op recorded for `unknown.theme` in W04 (§12). |
| OS high-contrast colours kept | `38d61c0bbe` | 4.41 | The engine keeps OS high-contrast colours instead of forcing the default theme, but only on the `String` path; the `ITheme` overload is not covered. |
| Effective themeid published | `9adfbf7713` (issue #4168) | 4.41 | Starting with `9adfbf7713`, ThemeEngine publishes the effective applied theme ID into `DefaultScope`, allowing normal preference-service lookup to expose inherited/applied state even when no explicit persisted choice exists. The engine keeps two concepts separate: instance/config/user scope carries explicit persisted selection semantics, while `DefaultScope` carries effective runtime theme publication. `themeid` is therefore not guaranteed to equal the active theme, and `DefaultScope` is not persisted selection state. Not tested in the 4.33 runs. |
| Dark attribute | `547df13d85` | master (4.42) | Adds the `isDarkTheme` theme attribute and `ITheme.isDark()`; relevant to the §15 heuristic replacement. |

Consequences for the record:

- The export blocker (§6, §24) is unchanged on master, so waiting for newer Eclipse does not
  resolve it. Upstream promotion would have to be requested.
- Sections 12, 14, 15, 20 and 30 are qualified above as 4.33-specific where these changes alter
  them. Section 5's ID table is the 4.33 inventory only.
- Any bounded compatibility exception (§30) should name the Eclipse releases it covers and add
  verification for each; the 4.33 evidence does not transfer to 4.40+ unseen.

## 35. Ecosystem practice (de facto, not a support contract)

The review found the following external usage of the selection API. It is recorded as **de facto
practice only**: it shows that other plugins depend on the interface despite the `x-friends`
export, not that Eclipse supports or will keep supporting it. Not re-verified in this revision and
not part of the R7 experiments.

- `vogellacompany/eclipse-themes`, `ThemeContributionItem`: obtains the engine with
  `PlatformUI` `getService(IThemeEngine)`, then calls `setTheme(theme, true)`.
- `jeeeyul/eclipse-themes`: uses `@SuppressWarnings("restriction")` for the access.
- DBeaver, Archi, Bonita, Modelio and KNIME call `engine.setTheme`.
- A Platform committer recommended `context.get(IThemeEngine.class)` in eclipse.platform.ui
  issue #4168. That is a committer's recommendation in an issue thread; it does not promote the
  package or create a compatibility promise.

Widespread use is relevant to a maintainer's judgement of break risk and to the strength of a
promotion request. It does not change the formal status in §6 or the R7 gate outcome.

## 36. Eclipse 4.41 measured evidence (P4 current-release verification)

This section is a **separate, later experiment**. Nothing in §1–§35 was run on 4.41, and nothing
here changes the 4.33 observations. Raw evidence: [obs/eclipse-4.41/](obs/eclipse-4.41/)
(`runtime-evidence.txt`, `bundle-evidence.txt`, `log-hashes.csv`, two screenshots). Probe
code stays in ignored `tmp/workbench-spike/v3/`; the probe is disposable and not committed.

### 36.1 Scope and method

Only Candidate A plus the P4 boundary was exercised. B stays rejected and C deferred. The
candidate path used **only** `PlatformUI.getWorkbench().getService(IThemeEngine.class)`,
`getThemes()`, `getActiveTheme()` and `setTheme(ITheme, true)`. The probe was compiled with a plain
`javac --release 17` against the 4.41 plugin jars (no reflection, no `internal.*` import). The
runtime class name of the service (`...internal.theme.ThemeEngine`) was logged as a diagnostic
label only. `setTheme(String, ...)`, `registerTheme`, `registerStylesheet`, `restore`,
`applyStyles`, `getStyle` and `Events` were not called. Resolved values were read with public SWT
getters. Preference scopes were read with `InstanceScope`, `ConfigurationScope`, `DefaultScope`
and `IPreferencesService`. Internal `ThemeEngine` bytecode was read with `javap` for diagnosis
only (non-candidate evidence).

### 36.2 Environment

| Item | Value |
| --- | --- |
| Eclipse | 2026-09 / Platform 4.41.0, build `4.41.0.20260903-0719` (EPP Java package; SHA-512 matches the published value) |
| CSS theme bundle | `org.eclipse.e4.ui.css.swt.theme` 0.15.200.v20260806-0515, JavaSE-21, jar SHA-256 `95d992f4f9c0f08274f3fac6c53b4939e46a04da40eb471bd28cb99f8533b522` (other hashes in `bundle-evidence.txt`) |
| Equinox | `org.eclipse.osgi` 3.24.300.v20260721-1251 |
| SWT | 3.135.0.v20260819-1952 (`SWT.getVersion()` 4974), win32 |
| JDK actually used | launcher JRE `org.eclipse.justj.openjdk.hotspot.jre.full` 25.0.4, runtime `25.0.4.1+1-LTS` (Eclipse Adoptium). The probe was *compiled* with JDK 21 `--release 17` |
| OS / WS | Windows 11 10.0 amd64, `ws=win32`. No GTK, macOS or Wayland run in this pass |
| OS dark mode | `Display.isSystemDarkTheme()` true throughout |
| Runs | R1 selection and round trip, R2 restart plus real Appearance switch, R3strict, R4/R5 visual restart. Disposable configurations and workspaces under `tmp/workbench-spike/v3/` |

The repository target platform was not modified.

### 36.3 Results

1. **Export directive (unchanged).** Exact 4.41 manifest:
   `Export-Package: org.eclipse.e4.ui.css.swt.internal.theme;x-internal:=true,org.eclipse.e4.ui.css.swt.theme;x-friends:="org.eclipse.e4.ui.workbench.renderers.swt,org.eclipse.e4.ui.workbench.swt,org.eclipse.ui.workbench"`.
   Our bundle is not in the friends list and remains a non-friend. This is accepted
   compatibility debt under P4 and must stay documented. It did not prevent compilation,
   resolution or service use at runtime.
2. **Service acquisition.** `getService(IThemeEngine.class)` returned a non-null service in
   every run under ordinary Equinox, loadable and callable through the public interface with no
   reflection and no internal class reference.
3. **Enumeration.** `getThemes()` returned `[e4_dark=Dark, e4_default=Light, spike.p4.theme=P4
   Proof (research)]`. Classic and High Contrast are absent (consistent with §34). Our exact
   `ITheme` was found by ID lookup.
4. **Selection.** `setTheme(ourITheme, true)` selected it. Postcondition: the active theme ID
   equals the expected ID and `getActiveTheme()` is the **same object** that was passed in.
   Repeated for built-in Dark, built-in Light and back to ours: all matched.
5. **Appearance page.** Our label appears in the real Appearance combo (`[Dark, Light, P4 Proof
   (research)]`, selected = ours after restart). See `appearance-page.png`.
6. **Surfaces.** The deliberately garish proof stylesheet changed Shell, view/editor tab folders
   and selected tabs, trim toolbars, Tree, Table, status line and the editor area. Resolved
   colours for the compared widgets equal the 4.33 values (Shell `#f8bbd0`, Tree `#ffcc80`,
   Table `#b2ebf2`, status `#a5d6a7`, selected tab `#d1c4e9`). Not styled by the proof CSS (and
   not claimed): the Project Explorer content area and the native button, which stay white. Only
   these surfaces were tested.
7. **Missing capability.** An exact ID not in `getThemes()` produced `CAPABILITY-FAILURE` with no
   fallback attempted and no state change.

### 36.4 Four distinct preference observations

Fresh workspace, OS dark, **before any selection call**: active `e4_dark`; instance **absent**;
`DefaultScope` `e4_dark` (published effective theme); effective lookup `e4_dark`.
After `setTheme(ours, true)`: active, instance, `DefaultScope` and effective lookup all equal
ours. ConfigurationScope stayed absent. After the real Appearance page chose Light: all four
equal Light.

The baseline row is the evidence for the model: **explicit persisted choice (instance) ≠
effective published theme (`DefaultScope` and lookup) ≠ active engine object**. They agree after
a successful explicit selection only because `restore=true` also writes the instance node
(bytecode: `setTheme(ITheme,boolean,boolean)` reads the previous value, passes it to
`EclipsePreferencesHelper.setPreviousThemeId`, writes `themeid`, flushes, then
`publishEffectiveThemeId()` puts the ID into `DefaultScope`). Calling with `restore=true`
therefore replaces any prior explicit instance choice, so R8a must record the prior state
(including absence) before the call, as the existing R7 decision already requires.
`restore=false` was not tested.

### 36.5 Restart

R1 ended with ours active. R2 started a new process on the same configuration and workspace.
Before any probe selection call the boot state had active, instance, `DefaultScope` and
effective all equal to ours, and the surfaces were ours (`runtime-evidence.txt` R2; the
`Shell.print` capture `restart-print.png` from R5 shows the styled workbench). The declaratively
contributed theme therefore applies at startup without a probe call on 4.41. (An R2 `copyArea`
screenshot captured the desktop instead; resolved values and the R5 capture are the visual
evidence.)

### 36.6 External Appearance switch

With ours active, the real Appearance page (`PreferencesUtil.createPreferenceDialogOn`, page
`org.eclipse.ui.preferencePages.Views`, class `...internal.dialogs.ViewsPreferencePage`) was
opened, **Light** chosen, and **Apply and Close** clicked. Choosing in the combo changed nothing
until applied. On apply, the active theme and instance, default and effective values all became
Light, so the platform can take control away from us as P4 assumes. Nothing was reconciled. The
4.41 page shows a **Theme Changed** dialog with `Use as default theme (only applies to workspaces
without a configured theme)`, `Restart` and `Don't restart`. The probe's button matcher was
case-sensitive and missed the buttons, so the dialog was closed with its window close (not a
restart) and the checkbox was not toggled. The `setTheme(ITheme, true)` API path showed no such
dialog and needed no restart.

### 36.7 Appearance page ID and manual fallback

`PreferencesUtil.createPreferenceDialogOn(shell, "org.eclipse.ui.preferencePages.Views", null,
null)` returned a dialog (not null) and opened the Appearance page, so the ID is valid on 4.41
and the public fallback "open Appearance so the user picks the contributed theme" works. No
product UI was built.

### 36.8 High contrast

`Display.getHighContrast()` was `false` in all runs; the OS accessibility configuration was not
touched. Bytecode: the only `getHighContrast()` call in `ThemeEngine` is inside
`setTheme(String, boolean)` (it logs warnings and falls back to the default theme for an unknown
ID). **`setTheme(ITheme, boolean[, boolean])` contains no high-contrast check**, so the engine
will not stop us. The future boundary must itself refuse coordinated switching when
`Display.getHighContrast()` is true. Real Windows high-contrast behaviour was **not** validated
and is pre-release work.

### 36.9 Strict resolver

`-Dosgi.resolverMode=strict` was passed (and read back by the probe as `strict`). The probe
bundle (`Require-Bundle` on the theme bundle) resolved, the service was available and selection
succeeded with identical results (R3strict). Static check: Equinox 3.24.300's framework classes
contain no `resolverMode` string and no `x-internal` or `x-friends` string; the only occurrences
are in its own `MANIFEST.MF` export headers (`bundle-evidence.txt`). Conclusion with its limits:
the strict property is **not honoured** by this Equinox and the framework does not read the
friend directive at runtime, so friend access is a PDE/compile-time and API-policy matter on
4.41. This is evidence by absence for one resolver build. The `Import-Package` binding variant
and other Equinox versions were not run. "Fail closed so the core plugin still loads" is a
production design requirement and was not built here.

### 36.10 Dark/light semantics on 4.41

`ITheme` exposes only `getId()` and `getLabel()`. **`isDark()` does not exist in 4.41** (it is
master/4.42-only, §34) and must not be assumed. The engine still classifies by ID:
`setTheme(ITheme, ...)` calls `Display.setDarkThemePreferred(id.contains("dark"))`, and
`restore()` uses the same `"dark"` substring for an unavailable saved ID (OS-dark inheritance
only when no saved ID, §34). Hence ID naming **still matters** on 4.41. Our `spike.p4.theme`
contains no `dark`, so it is classed light (the `setDarkThemePreferred` effect itself is not
publicly observable and was not measured). A future dark family needs an ID containing `dark`,
and a light family must not contain it.

### 36.11 Differences from 4.33

- Inventory: Classic and High Contrast themes are gone; only Dark, Light and ours.
- The built-in Dark palette changed (Shell `#48484c`, was `#515658`); our CSS values did not.
- `DefaultScope` now carries the effective ID even with no explicit choice (§34, observed here).
- The Appearance page has a new **Manage default...** button and the Theme Changed dialog with a
  default-theme option. The Eclipse runtime requires Java 25.
- A live switch **back** to ours from a built-in theme left the selected-tab background of three
  tab folders at the previous theme's `#ffffff` (R1 `p4-again`). A fresh process (R2, R5) and
  R3strict's first selection show the correct `#d1c4e9`. Not investigated and not compared with
  4.33; a visual-quality risk for live switching.
- Unknown-ID `setTheme(String, ...)` semantics were read from bytecode only (fallback to the
  default Light theme); not run, and not a candidate path.

### 36.12 Verdict

**P4 CURRENT-RELEASE GO** for Eclipse 4.41 on Windows. Every §17 criterion held: declarative A
works, the theme is an enumerable `ITheme`, `setTheme(ITheme, true)` works with a verifiable
postcondition, the choice survives a full restart, the real Appearance page can displace it, no
internal implementation, reflection or String-fallback path was needed, the access is a small
isolable surface, and failures (missing theme, high contrast, service unavailable) can route to
the manual Appearance fallback without mutating state. The `x-friends` restriction does not fail
the gate under P4. None of the BLOCKED conditions occurred.

Minimal production prerequisites for R8a (planning inputs, not implemented):

1. Declarative own theme(s) with static IDs; a dark family's ID contains `dark`, a light
   family's does not.
2. One isolated compatibility class using only `getService(IThemeEngine.class)`, `getThemes()`,
   `getActiveTheme()` and `setTheme(ITheme, true)`; catch `LinkageError` and runtime failures;
   the core editor plugin must load and work if the capability is absent (optional import or
   separate optional bundle: an engineering choice for the design).
3. Exact-ID lookup in `getThemes()`; absent means capability failure and Appearance fallback,
   never another theme.
4. Own `Display.getHighContrast()` refusal (the `ITheme` overload has none).
5. Postcondition `getActiveTheme()` equals the target before reporting success or recording state.
6. Capture the prior instance-scope `themeid` (including absence) before a `restore=true` call.
7. Record measured releases as confidence metadata (only 4.33, 4.40 and 4.41 are measured; see
   §37.4 for the VERIFIED / EXPERIMENTAL / UNAVAILABLE reading). Measured releases are not a
   hard allow-list.
8. Pre-release: real Windows high contrast, GTK, macOS, and the live switch-back tab-colour issue.

Remaining compatibility risks: friend-package removal or signature change in a future release;
unmeasured releases 4.34–4.40 and 4.42+; `restore=false` behaviour; the Theme Changed
default-theme semantics on the Appearance path; GTK, macOS, Wayland and real high contrast;
live-switch rendering residue; the `"dark"` ID heuristic until an API replaces it.

Decisions still required from the maintainer: the supported Eclipse release range (and whether
4.34–4.40 get verification or are declared unsupported for the coordinated capability); whether
the capability uses `restore=true` (explicit persisted choice) with the consequent conflict
handling; confirmation of the high-contrast refusal policy.

## 37. Eclipse 4.40 measured evidence (floor candidate)

Later, bounded pass: static matrix [obs/static-matrix-4.33-4.41.md](obs/static-matrix-4.33-4.41.md)
(all of 4.33–4.41, no IDE) and one runtime release, 4.40, chosen because its theme-engine and CSS
engine code differ from both 4.39 and 4.41. Raw evidence: [obs/eclipse-4.40/](obs/eclipse-4.40/).
Same disposable probe as §36 (narrow API only, no reflection, `--release 17`), Windows only, OS
dark mode on, `Display.getHighContrast()` false. Nothing here covers 4.34–4.39.

Environment: EPP Java 2026-06 R (SHA-512 matches the published value), Platform
`4.40.0.20260604-0652`, JRE 21.0.11, SWT 4973, theme bundle 0.15.100 (jar hash equals the p2
repository jar), `x-friends` export line identical to 4.41.

### 37.1 Results (all P4 criteria held on 4.40)

1. `getService(IThemeEngine.class)` non-null; `getThemes()` = Dark, Light, ours (Classic absent).
2. `setTheme(ours, true)`, Dark, Light and back to ours: postcondition active ID equals target and
   `getActiveTheme()` is the same object, every time.
3. Missing ID: `CAPABILITY-FAILURE`, no state change.
4. Restart (fresh process, same configuration): ours active at boot with no probe call; surfaces
   match the proof CSS (Shell `#f8bbd0`, Tree `#ffcc80`, Table `#b2ebf2`, status `#a5d6a7`,
   selected tab `#d1c4e9`: identical to 4.33 and 4.41 values).
5. Real Appearance page (`org.eclipse.ui.preferencePages.Views`): ours listed and selected,
   choosing Light and **Apply and Close** replaced the active and persisted theme; the Theme
   Changed dialog (Use as default / Restart / Don't restart) appeared as on 4.41. Light persisted
   across a restart; re-selecting ours via `setTheme(ours, true)` persisted again.
6. Live switch back to ours left the selected-tab background at `#ffffff` (as on 4.41); a fresh
   process shows `#d1c4e9`. The residue is **not 4.41-specific**.

### 37.2 Differences from 4.41 observed

- **No `DefaultScope` publication.** Baseline: active `e4_dark`, instance, config **and default
  absent**, effective lookup absent. After an explicit selection the effective lookup equals the
  instance value. So `getActiveTheme()` is the only source of "what is active" before an explicit
  choice; the P4 boundary must not read `DefaultScope`.
- Runs on JRE 21 (4.41 needs Java 25).
- Cold first start of a fresh configuration exceeded five minutes (m2e etc.); the harness timeout
  killed it once, a warm rerun was normal. Not a theme finding.
- `setDarkThemePreferred` effect is not publicly observable and was not measured.

### 37.3 Classification

4.40: **RUNTIME VERIFIED** (Windows, P4 narrow API). 4.34–4.39 are not run; see the static matrix
for their `STATICALLY EQUIVALENT` / `UNVERIFIED` status. With a floor of 4.40 the verified set is
exactly {4.40, 4.41}, both on Windows only. Still open for the range: restore-time behaviour for an
unavailable saved theme (4.40 and 4.41 are equal), real Windows high contrast, GTK/macOS/Wayland,
4.42+.

### 37.4 Maintainer decision (range for R8a planning)

The maintainer accepted **Eclipse 4.40 and 4.41 on Windows** as the **VERIFIED** set for R8a
planning, with 4.40 as the initial verified floor. "4.40+" is not an unbounded verification claim
and not a hard allow-list: only {win32 × 4.40, 4.41} are measured. Other combinations (4.34–4.39,
4.42+, GTK, macOS, Wayland) are **not verified**; they are not excluded by this record. Under the
maintainer's policy they may be offered as **EXPERIMENTAL** (explicit user opt-in, runtime
capability and postcondition checks still required) or are **UNAVAILABLE** when a required
capability or safety gate fails. The manual Appearance fallback applies whenever the capability
is not usable. Static findings that bear on experimental eligibility (e.g. 4.35–4.39 never call
`setDarkThemePreferred`; CSS engine drift) are in the static matrix. Pre-release items, not yet
verified: real Windows high contrast, GTK, macOS and Wayland, and any release after 4.41. This is
a planning decision, not an authorization to implement R8a.

## 38. R8a dynamic-palette micro-spike (Option D feasibility)

Question: can one declaratively contributed workbench CSS theme consume colours that the plugin
changes at runtime, with no `registerTheme`, `registerStylesheet` or generated CSS? Bounded probe,
Windows only, Eclipse 4.40 (JRE 21) and 4.41 (Java 25). Raw filtered logs, probe CSS and
`plugin.xml`: [obs/r8a-dynamic-palette/](obs/r8a-dynamic-palette/). Probe code is disposable and
ignored. Nothing here authorizes R8a implementation.

### 38.1 Mechanism found (from 4.40 bytecode and the shipped `org.eclipse.ui.themes` CSS)

| Item | Finding |
| --- | --- |
| CSS syntax | A quoted `'#<id>'` value, e.g. `background-color: '#spike-p5-surface'`; Eclipse's own light/dark CSS uses it (`swt-tabBackground-color: '#org-eclipse-ui-workbench-…'`). `CSSSWTColorHelper.getRGBA` treats a string starting with `#` that is not a 6-digit hex as a definition reference |
| Value source | `ColorAndFontProviderImpl.getColor(id)` (ui.workbench) reads `IThemeManager.getCurrentTheme().getColorRegistry().getRGB(id)`; the dots-to-dashes id is normalised back |
| Definition | Public `org.eclipse.ui.themes` extension `colorDefinition id label value` (a CSS `ColorDefinition#id {…}` rule is the CSS-side alternative) |
| Resolution time | Each time the CSS rule is applied (theme parse/apply), not when the registry changes |
| Writing values | Public API: `ColorRegistry.put(id, RGB)` on `IThemeManager.getCurrentTheme().getColorRegistry()` |
| Persistence | **Observed side effect**: `put` also writes `<id>=r,g,b` to the instance `org.eclipse.ui.workbench` preferences (the Colors-and-Fonts customization store, which also holds the user's font keys). After a restart the persisted values are applied at CSS apply with no plugin code |
| Status | `colorDefinition`, `ITheme`/`IThemeManager`, `ColorRegistry`: public. The `'#id'` CSS syntax is the Eclipse theme-CSS convention but its resolver lives in `org.eclipse.e4.ui.internal.css.swt` (internal) and is undocumented as a third-party contract: **implementation-specific**. Persistence-on-`put`: **internal behaviour**, observed only |

### 38.2 Results (identical on 4.40 and 4.41)

Palettes A/B/C were deliberately extreme (red/blue/cyan surface). Values defaulted to
808080/202020/404040 before any `put`.

| Refresh action after `put` | Result |
| --- | --- |
| nothing | unchanged (not D1 LIVE) |
| `engine.applyStyles(shell, true)` | **unchanged** (R1 4.40, S1 4.41) |
| `setTheme(activeTheme, true)` (same ID) | **unchanged** (R3 4.40, S1 4.41) |
| switch to another theme and back (`restore=false` intermediate, then `true`) | **repaints**: A, B, C, A produced distinct surface, tab-selection, tree, table and status colours each time; no stale previous colours on those surfaces; active ID returned to the same object; `getThemes()` count unchanged |
| flip between two own twin themes that share the CSS (`restore=true` each) | **repaints** (B, C on 4.40 and 4.41); the active ID alternates between the twins |

The internal `themeRegistryModified` event path was not run (probe error; internal anyway).
The earlier live switch-back tab residue (§36.11, §37.1) did not reproduce on these
reference-based tab colours. Selected-tab values updated on every switch.

Switch cost: 0.23–2.3 s for hop (two restyles), 0.34–2.8 s for one flip (first call slowest).

### 38.3 Classification

**D3 RESELECT.** Not D1/D2: neither `applyStyles` nor same-ID reselection re-reads the values.
Re-reading needs a real theme change. Restart also works (persisted values apply at boot, 4.41 S2
and 4.40 R5), so D4 is not needed.

### 38.4 What this does and does not show

- Shown: runtime palette values reach an existing declarative theme without runtime theme or
  stylesheet registration, are public-API writable, persist across restart, and survive repeated
  switches, on proof surfaces only, Windows only.
- Not shown: Appearance-page presentation of twin entries; real high contrast; GTK/macOS;
  interaction with a user editing the same colours on the Colors-and-Fonts page; whether
  `colorDefinition isEditable="false"` hides or protects them; native-surface and GTK colour
  support; releases other than 4.40/4.41; contrast of derived palettes.
- Extra dependencies beyond the approved `IThemeEngine` boundary: public `IThemeManager`/`ColorRegistry`
  (no new exception), the `'#id'` CSS convention, and reliance on `put` persistence (internal
  behaviour; a plugin could instead write its own keys, which is more intrusive). No new
  x-friends/internal Java type is required.
- Refresh requires switching themes. Without twins that means a transient switch through another
  theme (full stock-CSS restyle and its foreign preference side effects, flicker); with twins it
  means two plugin-owned entries per CSS family in `getThemes()`/Appearance.

### 38.5 Verdict

**OPTION D GO**, bounded: one static CSS per family reading `'#id'` references, palette published
through `ColorRegistry`, refresh by twin-theme flip, verified on Windows 4.40/4.41 only. See the
report for the Option A comparison and the decisions this leaves to the maintainer.

## 39. Closeout

**Final R7 verdict: P4 GO and OPTION D GO.** Maintainer-approved after the §38 micro-spike.

Measured limits that carry into R8a unchanged:

- Runtime evidence exists for Windows (win32/win32) on Eclipse 4.40 (theme bundle 0.15.100) and 4.41 (0.15.200); GTK3/X11 was
  measured on 4.33 only for P4 selection (§1–§33), and nothing runtime for macOS.
- Dynamic colours work through symbolic `'#id'` CSS references resolved against the current Colors-and-Fonts `ColorRegistry`.
  `ColorRegistry.put(...)` is public API; the `'#id'` resolver is implementation-specific (`org.eclipse.e4.ui.internal.css.swt`).
- A real theme-ID change is required to re-read changed symbolic colours. Same-ID `setTheme(activeTheme, true)` and
  `applyStyles(shell, true)` do not refresh.
- No `registerTheme`, `registerStylesheet` or generated CSS is required.
- Selection (`setTheme(ITheme, true)`) is the only engine call used; the access is non-API (`x-friends`) and accepted under P4.

The R8a production-prep questions (can private colour keys be used; can a refresh twin be hidden) were answered in the production
change `dynamic-workbench-theming` (`design.md`, `evidence/gate-q1-q2-runtime.txt`), not here. This change is closed; nothing
under it is production behaviour.
