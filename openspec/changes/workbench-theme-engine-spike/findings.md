# R7 findings — workbench theme engine

RESEARCH ONLY · NO PRODUCTION BEHAVIOR CHANGE

GO / NO-GO DECISION GATE: **NO-GO for shipping the requested programmatically coordinated R8a
under the present supported-public-API-only constraint.** Own declarative themes work on the
tested Windows and GTK3/X11 platforms. However, the exact target exports the selection API
package with `x-friends` restrictions. This is a support-contract blocker, not a failure of CSS
theme contribution. A is the smallest technically viable candidate if that blocker is resolved.
Full R3 is not a prerequisite for R7.

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

## 12. Postconditions

Every retained explicit setTheme action has a POST line: expected ID, actual ID and equality.
Known IDs match. `unknown.theme` in W04 returns without throwing, leaves C active and keeps C
saved: **match=false**. No tested selection produced a null active theme; the API permits null.
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

**NO-GO / resolve support contract before programmatically coordinated R8a.** Contribution,
selection, active verification, restart persistence and useful surfaces are demonstrated.
Unrestricted external selection API support is not: the exact bundle's friend export contradicts
an unconditional supported-public-API claim. The existing public Java interface's presence is
not enough to waive that criterion. No cast/reflection/internal reset is recommended as a workaround.

A remains the smallest viable mechanism if upstream clarifies/promotes its external contract or
the maintainer explicitly revises the compatibility policy to accept bounded provisional coupling.
That latter choice changes the original gate; it is not a GO under the original rules. A purely
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
| API/support blocker | Seek upstream external contract/promotion (delays implementation); explicitly accept version-pinned provisional coupling (relaxes original criterion); manual Appearance-only scope (loses coordinated selector) | Seek upstream clarification/promotion; keep R8a unapproved meanwhile |
| Mechanism after gate | A owns selectable identity; B cannot withdraw from built-in; C adds startup/refresh burden | A; no B/C in MVP |
| Support boundary | Tested 4.33 Windows/GTK3-X11 only; broader versions/platforms require evidence | Start with that explicit verified boundary; no macOS/Wayland/GTK4 claim |
| Palette scope | Two static light/dark families; single fixed palette; per-editor generated matching | Static light/dark families; generation deferred |
| Original absence / missing previous ID | Preserve absence with bounded key restoration; leave explicit prior ID (changes OS inheritance); ask recovery when previous ID is unavailable | Preserve absence; explicit user choice on unavailable/conflicting state, no invented fallback |
| External/transient switch | Force product theme back; preserve external selection and relinquish/report conflict; prompt before restoring | Preserve external choice, report truthful disagreement; never auto-force |
| High contrast | Block takeover; explicit warning/opt-in; override automatically | Block takeover of detected/named high contrast; verify actual OS modes before support claim |
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
