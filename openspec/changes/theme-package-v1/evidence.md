# Evidence — Theme Package v1

Gathered on `develop` @ `4a4d0b829a84b43826260f8a90647f5965ce58c7`, clean working tree, no active OpenSpec change.

## 1. Current model and why it exists

- `core.models.Theme` is an immutable *visual* definition (source id, name, author, website, description, `ThemeType`, `Map.copyOf`
  tokens). R2.4 removed identity, storage and catalog equality from it on purpose; it uses identity equality.
- `eclipse.ThemeRecord(logicalId, origin, locator, theme)` is the catalog entry; equality is the logical id. Bundled ids are
  `bundled:<ect-id>`; installed ids are `local:<uuid>` persisted in `theme-identities.properties` keyed by locator.
- R2 PM2-A: source claims (`ect-id`) are untrusted metadata and never merge records. R2 PM5-D: no revision, digest or history; an
  explicit Apply always re-dispatches current content. A package needs neither a second identity system nor a digest to satisfy the
  historical R9 scenario "same package ID with new content": update preserves logical identity and replaces content; Apply reapplies.
- `WorkbenchPaletteResolver.resolve(Theme)` is pure, reads only non-syntax tokens, and is the single source of the eight-role palette
  that both the CSS color definitions and `PluginColors.derive(palette)` consume. Authored workbench colors therefore only need to
  enter at this one function to reach chrome *and* plug-in colors.
- Family selection is `WorkbenchFamily.of(theme.getType())`; Eclipse derives dark preference from `id.contains("dark")` on the static
  CSS theme ids, so appearance must stay binary (`dark`/`light`) to have an execution path.
- High contrast: `WorkbenchCapabilityClassifier` reports `UNAVAILABLE/HIGH_CONTRAST` from `Display.getHighContrast()`; editor theming is
  unaffected. No package field can or should override that.

## 2. Downgrade/old-reader behavior (container decision)

- `git show v1.1.0:.../XmlThemeParser.java`: `DocumentBuilderFactory.newInstance()` with no security controls, reads any root's
  `name`/`type`/... attributes, no root check. `v1.1.0` `ThemeRepository` lists only `*.xml`.
- Current `develop`: discovery filters `.xml` (locale-root lower-case); a non-`colorTheme` root is an ERROR refusal.
- Consequence: a package stored as `.xml` would, after a downgrade to v1.1.0, be parsed insecurely and admitted as a theme built from
  whatever direct children match token names. A `.ectheme` file is invisible to both v1.1.0 and current discovery.

## 3. Legacy vocabulary in the real corpus

Element names used by the 33 bundled themes that are *not* canonical `TokenKey` names (resolved by `LegacyEclipsePropertyMapper`):
`writeOccurrenceIndication, typeParameter, typeArgument, singleLineComment, searchResultIndication, parameterVariable,
occurrenceIndication, javadocTag, javadocKeyword, javadoc, filteredSearchResultIndication, deprecatedMember, deletionIndication,
commentTaskTag, javadocLink, warning` (plus the `colorTheme` root). A package editor section that accepted only canonical names would
make copy-paste conversion lossy and silently different.

## 4. Size

Largest bundled theme: 2,822 bytes. No size bound exists today for legacy XML; JAXP `FEATURE_SECURE_PROCESSING` bounds entity
expansion and DOCTYPE is refused outright.

## 5. Preview

`EclipseThemesPreferencePage` builds `Constants.PREVIEW_SERVER_URL + theme.theme().getId()` for the embedded browser. `getId()` is the
legacy `ect-id`. Packages must leave it absent so no package id or author string is sent to that server.

## 6. Prompt truncation

The planning brief received for this change ends mid-sentence in item 13 ("High contrast must continue following the R…"). Items
after 13 were not available; sections, limits, UI and verification were derived from the repository contracts instead. The maintainer
should confirm no later brief item contradicts this proposal.
