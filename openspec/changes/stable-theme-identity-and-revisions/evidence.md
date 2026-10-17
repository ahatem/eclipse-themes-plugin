# Identity discovery evidence

## Baseline and method

Inspected 2026-10-18. Initial clean branch: `fix/deterministic-corpus-fingerprint-ordering`, `62ed81a`. Fetch confirmed `origin/develop=9f708fabfb3806638c1ee2842d15f1a39d3d45a9` (PR #24), following `7cb92e5` (PR #23). Local `develop` was fast-forwarded. The requested R2 branch already existed with only `93db6ee` (R1 archive hygiene), based on PR #23. That work was preserved and rebased onto PR #24 as `aee20ba`, with the requested commit message. No remote write occurred.

The existing archive at `../archive/2026-10-17-formalize-regression-testing/` was retained with its original date. All 20 tasks are checked. Its verification record includes the final proportionality review and independent service-continuation correction; these are historical results, not fresh executions. Review confirmed seven unchanged file renames and only the new `regression-verification` main spec. Promoted requirement text matches the archived delta exactly and Purpose is preserved. Strict all-spec validation passed (9 items). No redundant archive commit or second archive was created.

Method: read current source/specs and relevant historical boundaries; search identity callers; inspect bundled XML root attributes as text. No Java tests, builds, provisioning, runtime or user-workspace experiments were run. The scenarios below are **direct code traces**, not observed graphical sessions. DOM missing-attribute behavior is reflected in the Q3 trace; existing compatibility and repository fixtures already accept missing-ID XML but do not assert its equality semantics. No external API premise is needed to select this bounded contract.

## Current evidence anchors

Paths below are relative to `bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/` unless linked otherwise. Line references describe merged PR #24 production source, unchanged by the archive.

| Anchor | Concrete source | What it establishes |
| --- | --- | --- |
| E1 | [Theme](../../../bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/core/models/Theme.java), fields/constructor 11–32, accessors 35–64, mutation 67–68, equality/hash 97–105 | One `id`, label/author/website/description/type, optional file and mutable tokens. Equality is self-reference or equal nonnull ID; hash is ID hash or zero. No content/revision comparison. Two constructor-null IDs are unequal unless same object; two parsed empty IDs compare equal. |
| E2 | [XmlThemeParser](../../../bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/core/parser/XmlThemeParser.java):63–80, 131–151 | Reads only root `ect-id` into `id`, with no trimming, namespace, uniqueness or required-ID validation. Missing attribute yields empty string. Legacy root `id` is ignored. Root defaults and mapped tokens become current content; source file passes through. |
| E3 | [ThemeParser](../../../bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/core/parser/ThemeParser.java):25–36; [ParserFactory](../../../bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/core/parser/ParserFactory.java):17–25 | Stream-only parse has no file; file parse retains supplied file. Parser dispatch uses case-sensitive `.xml`; filename is a format selector, not parsed identity. |
| E4 | [ThemeRepository](../../../bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/eclipse/ThemeRepository.java):27–46, 84–145 | `TreeMap<String,Theme>` keyed by exact display name. Reload clears it, loads bundled first, then installed. Both loaders skip duplicate names. Bundle `findEntries` and installed `Files.list` order are not explicitly sorted. Final distinct-name ordering is deterministic; duplicate-name winner is not established by these APIs in this source. |
| E5 | ThemeRepository:49–81 | Destination is `installed_themes/<source basename>`. Copy truncates/replaces before parse. Successful import puts by name, overwriting in-memory same-name entry. Parse failure attempts unchecked destination deletion. Returned Theme points to stored copy. Source directory/provenance is not retained. |
| E6 | [Preference page](../../../bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/eclipse/preferences/EclipseThemesPreferencePage.java):271–320, 343–347, 477–484 | Current marker and list membership rely on Theme equality. Selection recovery is first matching nonblank `getId()` in name-sorted catalog. Preview URL appends the same ID to a remote address. |
| E7 | Preference page:517–540, 560–576, 690–695 | Direct apply writes selected ID before calling manager/service, then assigns current object and displays success. Import immediately reloads catalog but retains returned pre-reload object for selection. OK calls apply only if selected and current objects compare unequal. |
| E8 | Preference page:584–628, 660–674, 712–744 | Removal deletes optional File without checking boolean, resets default if equal to current, reloads and announces success. Default theme choice starts from names. Stop-management resets selected preference to its default after bounded reset success. |
| E9 | [PreferenceInitializer](../../../bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/eclipse/preferences/PreferenceInitializer.java):12–14; [Constants](../../../bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/core/Constants.java):4–7 | Default `ACTIVE_THEME_ID` is `GitHub Light Default`, a label rather than that theme's `e541c789-7f90-4539-b1fc-3e086dd8e63e`. No corpus ID equals the default label. |
| E10 | [ThemeManager](../../../bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/eclipse/ThemeManager.java):19–42; [ThemeService](../../../bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/eclipse/ThemeService.java):23–37; [EclipseThemes](../../../bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/EclipseThemes.java):24–29, 56–62 | Repository uses Platform bundle state directory. Service consumes tokens through adapters; no theme-ID application cache, content change detector or startup reconcile. Backing-store errors are logged and swallowed; selected intent/current marker is not proof of successful application. |

`PreferenceKeys` defines only `ACTIVE_THEME_ID`. Production-source search found its readers/writers only in the initializer/page. Theme identity callers are the page and repository; adapters consume tokens, not IDs. `MANIFEST.MF` exports no packages; extension metadata adds adapters and preferences, not an external theme identity API. Existing in-process signatures still need coordinated updates. Applied editor preference values persist separately, without a theme revision association.

## Identity role map

| Candidate | Actual role today | Persistence/stability |
| --- | --- | --- |
| `ect-id` / Theme.id | Intended source/logical identity, actual equality/hash, lookup and selected-intent key, remote preview lookup | Persisted in XML and preference values; stable only by source author's convention. Can be missing/blank/duplicated/changed. Not a proven universal ID. E1/E2/E6/E7. |
| Legacy XML `id` | Unused source metadata, unrelated to current lookup/equality | May exist in external documents; parser ignores it. No external corpus was inspected. E2. |
| Display name | Presentation, search, sort, defaults lookup; **accidental catalog identity** | XML metadata, not a stored logical reference. Change replaces map key; collisions hide entries. E4/E8/E9. |
| Source filename | Parser selector and **accidental storage/replacement identity** | Installed filename persists; changing it changes destination, not XML ID. E3/E5. |
| Full external filesystem path | Input locator only | Original directory discarded after copy. Not persisted in Theme or a provenance record. E5. |
| Stored filesystem path / Theme.file | Installed-file locator, imported/removable flag | File itself persists; path is reconstructed on load, not serialized as identity. Optional in model. E1/E3/E8. |
| Bundled resource path | Discovery/format/error locator | Packaged path persists in the bundle; not retained in stream-parsed Theme and not an ID. E3/E4. |
| Imported source document | Metadata/content input | Stored bytes copied before validation; there is no installation manifest. E5. |
| Repository entry | Name-keyed in-memory map entry | Rebuilt on reload; no record identifier independent of name/file/ID. E4. |
| Java object reference | In-memory instance and equality self fast-path | Recreated on parse/reload; not durable. Content is mutable behind an unmodifiable map view. E1/E7. |
| `ACTIVE_THEME_ID` | Workspace selected intent, also used to infer current marker | Raw source-ID string written through ScopedPreferenceStore; default is a name. Not a success journal or revision. No new claim about disk flush ordering is made. E6–E10. |
| Page selected/current fields | Transient selection and presumed current object | Not persisted as objects; stale across reload. Other plugins/users may alter editor preferences independently. E7/E10. |
| e4 `themeid` | Eclipse workbench appearance/dark-mode hint | Foreign preference read to choose plugin default; unrelated to this plugin's logical theme identity. Page 633–649. |

## Twelve lifecycle traces

| # / scenario | Current behavior, including conditions and limits |
| --- | --- |
| 1. Bundled normal load | Bundle XML stream parses with no File; first nonblank name wins; catalog sorts by name. `ect-id` becomes equality/lookup value. Bundle path is discarded. E1–E4. |
| 2. Install imported theme | External file copied under basename, parsed with destination File, inserted by name. Page then reloads immediately, so a name conflicting with a bundled theme is skipped; returned object may no longer be the catalog object. E5/E7. |
| 3. Identical file again | For a readable external source at a different path from destination: overwrites same basename, creates a new object, overwrites same map name, then UI reloads. No content comparison/dedup test. Same `ect-id` means equal objects. If source IS destination, opening output can truncate input: Q4 remains. E1/E5/E7. |
| 4. Modified content reimport | Same basename overwrites bytes before parse. With unchanged name and ID, new object equals old current despite different tokens; OK can skip. Invalid candidate can delete valid prior bytes. Different basename leaves old file as well. E1/E5/E7. |
| 5. Display name changes | ID equality unchanged. Reload uses new name; direct repository import can temporarily retain old map key if not reloaded. If both files remain, both names can exist with the same ID; first ID match wins selection. E1/E4–E7. |
| 6. Source filename changes | New basename creates a second stored file; XML ID stays unchanged. Same-name reload hides one; no rename/update recognition. Moving only external directory with same basename changes neither destination nor ID. E3–E5. |
| 7. Two same names | Import overwrites in-memory entry regardless of ID. Reload loads bundled first, skips later same names; installed duplicate winner depends on unsorted encounter order. Distinct IDs do not preserve both entries. E4/E5. |
| 8. Two same `ect-id` | Different names can both enter map, compare equal and show current markers; lookup chooses first name-sorted matching ID. No ID collision check. Same names additionally hit #7. Empty IDs also compare equal, but blank selection lookup returns no result. E1/E4/E6. |
| 9. Same content, different paths | Same basename targets same storage file. Different basenames leave multiple files; same parsed name hides duplicates on reload. Content is never compared. Source directories are forgotten. E4/E5. |
| 10. Bundled content changes between releases | Next catalog parse loads new content. If packaged ID is retained, persisted intent resolves that ID (subject to duplicates). No startup application hook appears in inspected activation/manager/service/page code. Opening page marks newly parsed object current; OK can skip it. Whether an actual published release historically changed IDs is not established. E6/E7/E10. |
| 11. Active stored representation changes | No watcher. Already loaded Theme retains old parsed values, not a live disk view. Import/reload creates new objects while page current can retain old one; ID-only OK suppresses applying the changed tokens. Reopening resolves ID anew without proving colors match. Double-click directly calls apply and bypasses this guard. E1/E6/E7/E10. |
| 12. Remove installed theme | UI allows when File present, deletes without checking outcome, uses equality to decide default reset, reloads, announces success unless exception. Duplicate/empty ID can trigger default reset for a different record. On failed boolean deletion, file may remain despite success message. No revision/history/tombstone. E8. |

## Concepts separated

| Concept | Exists / persisted today? | Stable or changeable; consumers | Representation required in this recommendation? |
| --- | --- | --- | --- |
| Logical theme identity | Only an unreliable interpretation of `ect-id`; no established independent identity | Equality/lookup depend on it; absent and duplicate values break distinction | **Required now:** nonblank established ID, not author-claim inference |
| Display name | Yes / XML | Author can change; UI, catalog and default lookup rely on it | Existing metadata retained; cease using as unique key |
| Source/origin | Website/author metadata and implicit bundled-vs-file flag; original import origin not persisted | Changeable and unverified; UI/preview/removal use pieces | Retain source ID/File; no new provenance subsystem |
| Filename/path | Yes / stored XML location, bundle resource location | Movable; discovery/import/deletion use it | Locator binding required, no path-derived ID |
| Current content | Yes / XML, parsed token/metadata fields in memory | Can change independently of ID; adapters consume tokens | Existing representation is sufficient |
| Revision | No independent field or history | Word 'version' appears in import log, with no revision semantics | **Not required:** current content can be replaced |
| Content fingerprint/digest | No production field; corpus test has a secondary SHA-256 | Test projection includes filename/metadata/resolved roles; it is not a per-theme runtime identity | **Not required:** do not repurpose regression hash |
| Installed record | Implicit stored file and transient name map entry | File can be overwritten/removed; no durable independent binding | Minimal logical-ID-to-locator binding; no separate record ID/class hierarchy |
| Active selection | Raw preference plus transient fields | Changeable, defaults broken, not application success | Persist logical selected intent, retain ambiguity explicitly; no applied-revision field |

## Corpus and historical reconciliation

All 33 `themes/*.xml` root tags were inventoried with case-sensitive grouping on literal `ect-id` and name. Results: 33 nonblank IDs, 33 distinct IDs, 33 distinct names. Values are UUID-shaped, but the current parser accepts arbitrary strings, including `42` in `XmlThemeParserCompatibilityTest.metadataStylesAliasesAndReferences`. This is evidence for reusing curated IDs, not imposing UUID grammar or a global namespace on external sources. Root `id` has no current meaning even if future migration research mentions it.

The compatibility test's sparse stream/file cases and `RepositoryMappingWarningTest` accept documents without `ect-id`; imports cannot be assumed to provide one. No representative downloaded/installed legacy collection is available. The bundled corpus cannot establish uniqueness/stability across the website or other plugins. Its Git history is not evidence of a public immutable identity promise.

[Audit Q3/Q4/Q5/Q12/Q14/Q15](../../../docs/audits/2026-10-complete-project-audit.md#1-package-and-class-review) and U4 were checked against E1–E10. Q3/U4 still hold; Q1/Q2 and mapping work have since changed, so historical claims about no tests or insecure XML are not current identity evidence. Archived XML/mapping proposals explicitly deferred identity, import and migration. The R1 regression archive supplies harness/verification conventions, not an identity contract.

[R2 target](../../../docs/target-specs/R2_THEME_LIFECYCLE.md) correctly calls for presentation-independent identity and avoiding same-ID skip. Its word 'revision' need only mean different current contents. [R9 target](../../../docs/target-specs/R9_THEME_PACKAGE_CONTRIBUTOR.md) assumes schema/revision/digest metadata and generated legacy identity; these remain unaccepted future package assumptions. They do not justify a package schema, per-theme digest, history or globally trusted XML ID now. `CHANGE_QUEUE`'s 'package identity, legacy migration' phrasing is sequencing context, not preapproval of a migration architecture. No roadmap/target spec is silently rewritten by this plan.
