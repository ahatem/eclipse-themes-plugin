# Evidence: stock-Dark preference key classification

Scope: `plugin-color-coordination` (R8). Basis: the preference overrides that stock Eclipse Dark applies through `IEclipsePreferences` stylesheets (`@namespace` / `IEclipsePreferences#<node>` rules bound to `org.eclipse.e4.ui.css.theme.e4_dark`), collected from the plug-ins of the stock Eclipse 4.40 and 4.41 packages (Windows, `tmp/eclipse-4.40`, `tmp/eclipse-4.41`, not committed). Each key was matched against (a) the preference keys the editor integrations manage, (b) the owning plug-in's default value (color definitions, `markerAnnotationSpecification` extensions, preference initializers read from the class files) and (c) the plug-ins that read it. These are other plug-ins' preference names, not API; they are re-checked on each supported Eclipse release.

Classes: `REQUIRED_VISIBLE_PARITY` (the default is unreadable or clashing on a dark workbench and the surface is visible in normal use: coordinated), `ALREADY_COVERED` (an editor integration already manages it), `OBSOLETE_UNUSED` (nothing reads it), `NATIVE_NOT_OURS` (the value is an operating-system color), `OUTSIDE_R8` (readable default or not a color, or the owning plug-in is not part of the verified install).

## Result

| Class | Eclipse 4.40 keys | Eclipse 4.41 keys |
|---|---|---|
| REQUIRED_VISIBLE_PARITY (coordinated) | 64 | 68 |
| ALREADY_COVERED (editor integrations) | 111 | 111 |
| OBSOLETE_UNUSED | 0 | 0 |
| NATIVE_NOT_OURS | 1 | 1 |
| OUTSIDE_R8 | 21 | 21 |
| Total | 197 | 201 |

The four 4.41-only keys are the inline compare highlights (`inlineDiffColor`, `detailedInlineDiffColor`, `deletionInlineDiffColor`, `detailedDeletionInlineDiffColor`, owned by `org.eclipse.compare`, pastel defaults 229,242,229 / 204,229,204 / 255,229,229 / 255,204,204). They are coordinated and are inert on 4.40, where nothing reads them. The `PluginColorTable` has 68 keys.

## Coordinated keys (REQUIRED_VISIBLE_PARITY)

Node is the preference node written; the source names the palette role or derivation (`PluginColorSource`). "Stock Dark" is the literal value stock Dark sets, shown for comparison only; ECT derives its own value from the active theme.

| Node | Key | Source | Stock Dark |
|---|---|---|---|
| `org.eclipse.ui.workbench` | `org.eclipse.ui.workbench.HOVER_BACKGROUND` | RAISED | 52,57,61 |
| `org.eclipse.ui.workbench` | `org.eclipse.ui.workbench.HOVER_FOREGROUND` | FOREGROUND | 238,238,238 |
| `org.eclipse.ui.workbench` | `org.eclipse.ui.workbench.INFORMATION_BACKGROUND` | RAISED | 81,86,88 |
| `org.eclipse.ui.workbench` | `org.eclipse.ui.workbench.INFORMATION_FOREGROUND` | FOREGROUND | 238,238,238 |
| `org.eclipse.ui.workbench` | `CONTENT_ASSIST_BACKGROUND_COLOR` | RAISED | 52,57,61 |
| `org.eclipse.ui.workbench` | `CONTENT_ASSIST_FOREGROUND_COLOR` | FOREGROUND | 238,238,238 |
| `org.eclipse.ui.workbench` | `org.eclipse.jdt.ui.Javadoc.backgroundColor` | RAISED | 52,57,61 |
| `org.eclipse.ui.workbench` | `org.eclipse.jdt.ui.Javadoc.foregroundColor` | FOREGROUND | 238,238,238 |
| `org.eclipse.ui.workbench` | `HYPERLINK_COLOR` | ACCENT | 111,197,238 |
| `org.eclipse.ui.workbench` | `ACTIVE_HYPERLINK_COLOR` | ACTIVE_ACCENT | 138,201,242 |
| `org.eclipse.jdt.ui` | `content_assist_proposals_background` | RAISED | 52,57,61 |
| `org.eclipse.jdt.ui` | `content_assist_proposals_foreground` | FOREGROUND | 238,238,238 |
| `org.eclipse.jdt.ui` | `content_assist_parameters_background` | RAISED | 52,57,61 |
| `org.eclipse.jdt.ui` | `content_assist_parameters_foreground` | FOREGROUND | 238,238,238 |
| `org.eclipse.jdt.ui` | `javadocElementsStyling.darkModeDefaultColors` | DARK_FLAG | true |
| `org.eclipse.ui.workbench` | `ERROR_COLOR` | ERROR | 247,68,117 |
| `org.eclipse.ui.workbench` | `org.eclipse.ui.workbench.FORM_HEADING_ERROR_COLOR` | ERROR | 255,110,110 |
| `org.eclipse.ui.workbench` | `org.eclipse.ui.workbench.FORM_HEADING_WARNING_COLOR` | WARNING | 255,200,0 |
| `org.eclipse.ui.workbench` | `org.eclipse.ui.workbench.FORM_HEADING_INFO_COLOR` | MUTED | 170,170,170 |
| `org.eclipse.ui.workbench` | `INCOMING_COLOR` | INFO | 31,179,235 |
| `org.eclipse.ui.workbench` | `OUTGOING_COLOR` | FOREGROUND | 238,238,238 |
| `org.eclipse.ui.workbench` | `CONFLICTING_COLOR` | ERROR | 240,15,66 |
| `org.eclipse.ui.workbench` | `RESOLVED_COLOR` | SUCCESS | 108,210,17 |
| `org.eclipse.ui.workbench` | `EDITION_COLOR` | FOREGROUND | 238,238,238 |
| `org.eclipse.ui.workbench` | `org.eclipse.egit.ui.CommitMessageCommentColor` | MUTED | 128,128,128 |
| `org.eclipse.ui.workbench` | `org.eclipse.egit.ui.DiffAddBackgroundColor` | SUCCESS_TINT | 11,121,90 |
| `org.eclipse.ui.workbench` | `org.eclipse.egit.ui.DiffAddForegroundColor` | FOREGROUND | 216,254,245 |
| `org.eclipse.ui.workbench` | `org.eclipse.egit.ui.DiffRemoveBackgroundColor` | ERROR_TINT | 117,2,36 |
| `org.eclipse.ui.workbench` | `org.eclipse.egit.ui.DiffRemoveForegroundColor` | FOREGROUND | 255,232,237 |
| `org.eclipse.ui.workbench` | `org.eclipse.egit.ui.DiffHunkBackgroundColor` | INFO_TINT | 53,97,113 |
| `org.eclipse.ui.workbench` | `org.eclipse.egit.ui.DiffHunkForegroundColor` | FOREGROUND | 233,242,254 |
| `org.eclipse.ui.workbench` | `org.eclipse.egit.ui.DiffHeadlineBackgroundColor` | RAISED | 71,71,71 |
| `org.eclipse.ui.workbench` | `org.eclipse.egit.ui.DiffHeadlineForegroundColor` | FOREGROUND | 242,242,242 |
| `org.eclipse.ui.workbench` | `org.eclipse.egit.ui.IgnoredResourceBackgroundColor` | BACKGROUND | 47,47,47 |
| `org.eclipse.ui.workbench` | `org.eclipse.egit.ui.IgnoredResourceForegroundColor` | MUTED | 120,120,120 |
| `org.eclipse.ui.workbench` | `org.eclipse.egit.ui.UncommittedChangeBackgroundColor` | BACKGROUND | 47,47,47 |
| `org.eclipse.ui.workbench` | `org.eclipse.egit.ui.UncommittedChangeForegroundColor` | ACCENT | 114,157,186 |
| `org.eclipse.ui.workbench` | `org.eclipse.search.ui.match.highlight` | WARNING_TINT | 206,92,0 |
| `org.eclipse.ui.workbench` | `org.eclipse.jface.REVISION_NEWEST_COLOR` | WARNING_TINT | 75,44,3 |
| `org.eclipse.ui.workbench` | `org.eclipse.jface.REVISION_OLDEST_COLOR` | WARNING_TINT_SOFT | 154,113,61 |
| `org.eclipse.ui.editors` | `hyperlinkColor` | ACCENT | 102,175,249 |
| `org.eclipse.ui.editors` | `hyperlinkColor.SystemDefault` | OFF_FLAG | false |
| `org.eclipse.ui.editors` | `linked.slave.color` | INFO_TINT | 66,156,255 |
| `org.eclipse.ui.editors` | `breakpointIndicationColor` | INFO | 51,119,193 |
| `org.eclipse.ui.editors` | `fullcoverage_color` | SUCCESS_TINT | 6,74,6 |
| `org.eclipse.ui.editors` | `partialcoverage_color` | WARNING_TINT | 98,98,0 |
| `org.eclipse.ui.editors` | `nocoverage_color` | ERROR_TINT | 98,5,5 |
| `org.eclipse.ui.editors` | `org.eclipse.ui.editors.stickyLinesSeparatorColor` | BORDER | 81,86,88 |
| `org.eclipse.ui.editors` | `inlineDiffColor` | SUCCESS_TINT_SOFT | (4.41) |
| `org.eclipse.ui.editors` | `detailedInlineDiffColor` | SUCCESS_TINT | (4.41) |
| `org.eclipse.ui.editors` | `deletionInlineDiffColor` | ERROR_TINT_SOFT | (4.41) |
| `org.eclipse.ui.editors` | `detailedDeletionInlineDiffColor` | ERROR_TINT | (4.41) |
| `org.eclipse.ui.workbench` | `org.eclipse.jdt.ui.ColoredLabels.inherited` | MUTED | 143,143,191 |
| `org.eclipse.ui.workbench` | `org.eclipse.jdt.ui.ColoredLabels.writeaccess_highlight` | WARNING_TINT | 255,128,128 |
| `org.eclipse.debug.ui` | `org.eclipse.debug.ui.PREF_CHANGED_VALUE_BACKGROUND` | WARNING_TINT | 150,80,115 |
| `org.eclipse.debug.ui` | `org.eclipse.debug.ui.MemoryHistoryKnownColor` | FOREGROUND | 235,235,235 |
| `org.eclipse.debug.ui` | `org.eclipse.debug.ui.changedDebugElement` | ERROR | 255,128,128 |
| `org.eclipse.ant.ui` | `org.eclipse.ant.ui.informationColor` | INFO | 140,175,210 |
| `org.eclipse.ant.ui` | `org.eclipse.ant.ui.debugColor` | FOREGROUND | 235,235,235 |
| `org.eclipse.ant.ui` | `org.eclipse.ant.ui.errorColor` | ERROR | 225,30,70 |
| `org.eclipse.ui.workbench` | `org.eclipse.mylyn.tasks.ui.colors.foreground.today.scheduled` | INFO | 52,101,164 |
| `org.eclipse.ui.workbench` | `org.eclipse.mylyn.tasks.ui.colors.foreground.past.scheduled` | INFO | 52,101,164 |
| `org.eclipse.ui.workbench` | `org.eclipse.mylyn.tasks.ui.colors.foreground.thisweek.scheduled` | MUTED | 85,87,83 |
| `org.eclipse.ui.workbench` | `org.eclipse.mylyn.tasks.ui.colors.foreground.task.active` | ACCENT | 117,80,123 |
| `org.eclipse.ui.workbench` | `org.eclipse.mylyn.tasks.ui.colors.category.gradient.start` | RAISED | 85,87,83 |
| `org.eclipse.ui.workbench` | `org.eclipse.mylyn.tasks.ui.colors.category.gradient.end` | RAISED | 136,137,133 |
| `org.eclipse.ui.workbench` | `org.eclipse.mylyn.tasks.ui.color.text.quoted` | INFO | 106,133,255 |
| `org.eclipse.epp.mpc.ui` | `native-borders` | OFF_FLAG | false |

## ALREADY_COVERED

Managed by the editor integrations (`JavaEditorThemeAdapter`, `TextEditorThemeAdapter`, `AntEditorThemeAdapter`, `PluginEditorThemeAdapter`, `CppEditorThemeAdapter`), which own these keys. They are disjoint from the plug-in color table (`PluginColorTableOwnershipTest`).

- `org.eclipse.ant.ui`: 9 keys
- `org.eclipse.debug.ui`: 3 keys
- `org.eclipse.jdt.ui`: 74 keys
- `org.eclipse.ui.editors`: 24 keys
- `org.eclipse.ui.genericeditor`: 1 keys

## OUTSIDE_R8

| Node | Key | Reason |
|---|---|---|
| `org.eclipse.debug.ui` | `org.eclipse.debug.ui.MemoryHistoryUnknownColor` | default 114,119,129 (gray), readable on dark |
| `org.eclipse.debug.ui` | `org.eclipse.debug.ui.inColor` | default 0,200,125 (green console input), readable on dark |
| `org.eclipse.jdt.ui` | `content_assist_completion_replacement_background` | non-editable "Dummy" definition, overwrite-mode highlight only; default yellow/red is readable |
| `org.eclipse.jdt.ui` | `content_assist_completion_replacement_foreground` | non-editable "Dummy" definition, overwrite-mode highlight only; default yellow/red is readable |
| `org.eclipse.mylyn.wikitext.ui` | `block-h1` | CSS fragment string for wiki markup preview, not a preference color |
| `org.eclipse.mylyn.wikitext.ui` | `block-h2` | CSS fragment string for wiki markup preview, not a preference color |
| `org.eclipse.mylyn.wikitext.ui` | `block-h3` | CSS fragment string for wiki markup preview, not a preference color |
| `org.eclipse.mylyn.wikitext.ui` | `block-h4` | CSS fragment string for wiki markup preview, not a preference color |
| `org.eclipse.mylyn.wikitext.ui` | `block-h5` | CSS fragment string for wiki markup preview, not a preference color |
| `org.eclipse.mylyn.wikitext.ui` | `block-h6` | CSS fragment string for wiki markup preview, not a preference color |
| `org.eclipse.mylyn.wikitext.ui` | `phrase-code` | CSS fragment string for wiki markup preview, not a preference color |
| `org.eclipse.mylyn.wikitext.ui` | `block-bc` | CSS fragment string for wiki markup preview, not a preference color |
| `org.eclipse.ui.editors` | `TM4EMatchingPairColor` | owning bundle is not in the stock install; default not evidenced |
| `org.eclipse.ui.workbench` | `org.eclipse.ui.editors.inlineAnnotationColor` | default 128,128,128, readable on dark |
| `org.eclipse.ui.editors` | `asOccurencesIndicationColor` | owning bundle is not in the stock install; default not evidenced |
| `org.eclipse.ui.editors` | `infoIndicationColor` | default 130,160,190 squiggle, readable on dark |
| `org.eclipse.ui.editors` | `matchingTagIndicationColor` | owning bundle is not in the stock install; default not evidenced |
| `org.eclipse.ui.editors` | `overrideIndicatorColor` | default 180,207,205 squiggle, readable on dark |
| `org.eclipse.ui.editors` | `searchResultHighlighting` | boolean/style switch for editor search-result display, not a color |
| `org.eclipse.ui.editors` | `searchResultTextStyle` | style switch (BOX), not a color |
| `org.eclipse.ui.editors` | `spellingIndicationColor` | default 255,128,64 squiggle, readable on dark |

## NATIVE_NOT_OURS

| Node | Key | Reason |
|---|---|---|
| `org.eclipse.ui.workbench` | `org.eclipse.ui.editors.rangeIndicatorColor` | default is the system list-selection color (COLOR_LIST_SELECTION), owned by the OS |

## Stock Dark selectors not present verbatim in our sheet

`workbench-dark.css` names the same surfaces as the stock Dark theme (Eclipse 4.40 `org.eclipse.ui.themes`, dark layer plus `e4-dark_win.css`, `e4_basestyle.css`, `e4_default_win.css`) but states some rules once through broader selectors, where stock repeats them per `.MPartStack.active` or per view id. Selector strings that differ and what they are:

- Excluded on purpose: selectors that also match an editor's `StyledText` (`.MPart Composite > StyledText`, `#org-eclipse-wst-jsdt-ui-SourceView StyledText`, the compatibility editor's `Canvas > *`). Editor text belongs to the editor integration.
- Stock-only fallbacks of the Light default sheet (`e4_default_win.css`: white backgrounds, `swt-outer-keyline-color`, `#org-eclipse-ui-editorss`, `TitleRegion`) which the ECT sheets replace with role references rather than carry.
- Per-view and per-activity repeats of rules that our sheet states through `.MPart Form …`, `Form`, `ScrolledForm` and the global layer (Help view forms, `.MPartStack.active .MPart Form …`, `DependenciesComposite`, `ListEditorComposite`, `MasterDetailsBlock-MDSashForm`).

These classifications come from selector comparison and the semantics of the CSS engine (exact widget class names), not from a per-selector runtime check. The complete list for Eclipse 4.40 follows; it is reproduced by the same comparison.

<details><summary>111 selector strings</summary>

- `#org-eclipse-e4-ui-compatibility-editor Canvas`
- `#org-eclipse-e4-ui-compatibility-editor Canvas > *`
- `#org-eclipse-e4-ui-compatibility-editor Composite`
- `#org-eclipse-help-ui-HelpView Form`
- `#org-eclipse-help-ui-HelpView Form Button`
- `#org-eclipse-help-ui-HelpView Form FormText`
- `#org-eclipse-help-ui-HelpView Form Group`
- `#org-eclipse-help-ui-HelpView Form Label`
- `#org-eclipse-help-ui-HelpView Form Sash`
- `#org-eclipse-help-ui-HelpView Form ScrolledPageBook`
- `#org-eclipse-help-ui-HelpView Form Section`
- `#org-eclipse-help-ui-HelpView Form Text[style~='SWT.READ_ONLY']`
- `#org-eclipse-ui-editorss CTabItem`
- `#org-eclipse-wst-jsdt-ui-SourceView StyledText`
- `.MPart CTabFolder`
- `.MPart Composite > StyledText`
- `.MPart Form Button[style~='SWT.CHECK']`
- `.MPart Form Button[style~='SWT.RADIO']`
- `.MPart Form DependenciesComposite`
- `.MPart Form DependenciesComposite > SashForm > Section > *`
- `.MPart Form ListEditorComposite`
- `.MPart Form Section Tree`
- `.MPart PageBook > SashForm Label`
- `.MPart SashForm > Text[style~='SWT.BORDER']`
- `.MPart Section > Label`
- `.MPart Section Tree`
- `.MPart Text[style~='SWT.READ_ONLY']`
- `.MPartStack .MPart Form MasterDetailsBlock-MDSashForm`
- `.MPartStack .MPart Form Sash`
- `.MPartStack .MPart Form SashForm`
- `.MPartStack.active #org-eclipse-help-ui-HelpView Form`
- `.MPartStack.active #org-eclipse-help-ui-HelpView Form Button`
- `.MPartStack.active #org-eclipse-help-ui-HelpView Form FormText`
- `.MPartStack.active #org-eclipse-help-ui-HelpView Form Group`
- `.MPartStack.active #org-eclipse-help-ui-HelpView Form Label`
- `.MPartStack.active #org-eclipse-help-ui-HelpView Form Sash`
- `.MPartStack.active #org-eclipse-help-ui-HelpView Form ScrolledPageBook`
- `.MPartStack.active #org-eclipse-help-ui-HelpView Form Section`
- `.MPartStack.active #org-eclipse-help-ui-HelpView Form Text[style~='SWT.READ_ONLY']`
- `.MPartStack.active .MPart Form`
- `.MPartStack.active .MPart Form Button`
- `.MPartStack.active .MPart Form Button[style~='SWT.CHECK']`
- `.MPartStack.active .MPart Form Button[style~='SWT.RADIO']`
- `.MPartStack.active .MPart Form DependenciesComposite`
- `.MPartStack.active .MPart Form DependenciesComposite > SashForm > Section > *`
- `.MPartStack.active .MPart Form FormText`
- `.MPartStack.active .MPart Form Group`
- `.MPartStack.active .MPart Form Label`
- `.MPartStack.active .MPart Form Link`
- `.MPartStack.active .MPart Form ListEditorComposite`
- `.MPartStack.active .MPart Form MasterDetailsBlock-MDSashForm`
- `.MPartStack.active .MPart Form Sash`
- `.MPartStack.active .MPart Form SashForm`
- `.MPartStack.active .MPart Form ScrolledPageBook`
- `.MPartStack.active .MPart Form Section`
- `.MPartStack.active .MPart Form Section Tree`
- `.MPartStack.active .MPart Form Text[style~='SWT.READ_ONLY']`
- `.MPartStack.active .View TitleRegion`
- `.MPartStack.active > CTabItem`
- `.MPartStack.active > CTabItem CLabel`
- `.MPartStack.active > CTabItem:selected`
- `.MPartStack.active > CTabItem:selected CLabel`
- `.MPartStack.active CTabFolder Canvas`
- `.MPartStack.active Table`
- `.MPartStack.active.noFocus > CTabItem:selected`
- `.MTrimBar#org-eclipse-ui-main-toolbar`
- `.MTrimBar#org-eclipse-ui-trim-status`
- `.Mpart OleFrame`
- `.Mpart ScrolledComposite ProgressInfoItem`
- `.View Browser`
- `.View BusyIndicator`
- `.View Button[style~='SWT.CHECK']`
- `.View Button[style~='SWT.PUSH']`
- `.View Canvas`
- `.View Composite`
- `.View Composite Label`
- `.View Composite PrependingAsteriskFilteredTree`
- `.View Composite Text`
- `.View Composite Tree`
- `.View FigureCanvas`
- `.View FormText`
- `.View Group`
- `.View Group Combo`
- `.View Group Label`
- `.View Group Text`
- `.View Hyperlink`
- `.View Link`
- `.View OleFrame`
- `.View PrependingAsteriskFilteredTree Text`
- `.View SashForm`
- `.View Section`
- `.View StyledText[style~='SWT.READ_ONLY']`
- `.View TabbedPropertyList`
- `.View Text[style~='SWT.READ_ONLY']`
- `.View TitleRegion`
- `.View ToolBar`
- `.View Toolbar ToolItem`
- `.View WebSite`
- `@import url("platform:/plugin/org.eclipse.ui.themes/css/common/e4_globalstyle.css"); @import url("platform:/plugin/org.eclipse.ui.themes/css/dark/e4-dark_ide_colorextensions.css"); @import url("platform:/plugin/org.eclipse.ui.themes/css/dark/e4-dark_globalstyle.css"); @import url("platform:/plugin/org.eclipse.ui.themes/css/dark/e4-dark_partstyle.css"); @import url("platform:/plugin/org.eclipse.ui.themes/css/dark/e4-dark_tabstyle.css"); @import url("platform:/plugin/org.eclipse.ui.themes/css/dark/e4-dark-drag-styling.css"); CTabFolder Canvas`
- `CTabFolder[style~='SWT.DOWN'][style~='SWT.BOTTOM']`
- `CategoryItem > GradientCanvas`
- `CategoryItem > GradientCanvas > Label`
- `Composite > StyledText`
- `Composite.MArea`
- `Composite.MPartSashContainer`
- `Group > StyledText`
- `SashForm > StyledText`
- `Shell [style~='SWT.DROP_DOWN'] > StyledText`
- `Text[style~='SWT.SEARCH']`
- `Text[style~='SWT.SEARCH'] + Label`
- `ThemesExtension`

</details>
