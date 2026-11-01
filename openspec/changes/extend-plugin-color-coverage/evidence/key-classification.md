# Evidence: classification of plug-in color keys bound to the stock dark theme id only

Scope: `extend-plugin-color-coverage`. Candidates come from vogellacompany/eclipse-themes `plugins/*/css/*_preferences.css`. That
project writes them unconditionally, and we used it only as a list of names. Each candidate was checked against:

- (a) the owning plug-in's `colorDefinition` default (`plugin.xml`);
- (b) the code that reads the id;
- (c) any dark stylesheet the owner ships, and the theme id that sheet is bound to.

Sources:

- Mylyn: `org.eclipse.mylyn.tasks.ui` 4.11.0.v20260527-1436 (Eclipse 4.40 package, `tmp/eclipse-4.40`) and 4.12.0.v20260721-0835
  (4.41 package, `tmp/eclipse-4.41`); `org.eclipse.mylyn.context.ui` 4.11.0 and 4.12.0; `org.eclipse.mylyn.commons.ui` 4.11.0 for
  the `CommonThemes` ids.
- PDE and e4 tools: `org.eclipse.pde.genericeditor.extension` 1.3.400.v20260427-1309 and `org.eclipse.e4.tools.emf.ui`
  4.9.100.v20260513-1042, with sources, from the 2026-06 (4.40) RCP package. Neither bundle is in the stock 4.40/4.41 packages R8
  verified.
- Spring Tools: `spring-projects/spring-tools` at commit `83e669eae40e7be86dd4752bccc8791df6730af9`, source only, not installed.

Contrast is WCAG against stock Dark's view background 47,47,47. For background keys it is measured against the 238,238,238
foreground drawn on them.

Classes follow R8. `REQUIRED_VISIBLE_PARITY` (coordinated) means the default is unreadable or clashing on a dark workbench and a live
reader exists. `ALREADY_COVERED` keys are in the table already. `OBSOLETE_UNUSED` keys have nothing that reads them. `OUTSIDE` covers
readable defaults, values that are not colors, and keys owned by the editor integrations.

## Coordinated (REQUIRED_VISIBLE_PARITY)

All in node `org.eclipse.ui.workbench` (theme color definitions).

| Key | Owner (required bundle) | Default | Contrast | Reader | Dark sheet | Role |
|---|---|---|---|---|---|---|
| `org.eclipse.mylyn.tasks.ui.colors.foreground.past.due` | `org.eclipse.mylyn.tasks.ui` | 198,4,38 | 2.2:1 | `TaskElementLabelProvider` (overdue tasks), `UiLegendControl` via `CommonThemes.COLOR_OVERDUE` | none (Mylyn's sheet sets 7 other ids) | ERROR |
| `org.eclipse.mylyn.tasks.ui.colors.foreground.overdue.for.others` | `org.eclipse.mylyn.tasks.ui` | 128,0,0 | 1.22:1 | `TaskElementLabelProvider` via `COLOR_OVERDUE_FOR_OTHERS` | none | WARNING |
| `org.eclipse.mylyn.tasks.ui.colors.incoming.background` | `org.eclipse.mylyn.tasks.ui` | 225,236,255 | 1.03:1 (fg on it) | `AttributeEditorToolkit` (incoming-change fields in the task editor) via `COLOR_INCOMING_BACKGROUND` | none | INFO_TINT |
| `org.springframework.ide.eclipse.boot.dash.TextDecorColor` | `org.springframework.ide.eclipse.boot.dash` | 0,100,0 | 1.8:1 | `BootDashLabels` (theme registry) | `css/dark-theme.css`, bound to `e4_dark` only | SUCCESS |
| `org.springframework.ide.eclipse.boot.dash.AltTextDecorColor` | `org.springframework.ide.eclipse.boot.dash` | 0,0,139 | 1.14:1 | `BootDashLabels` | same | INFO |
| `org.springframework.tooling.ls.eclipse.commons.STS4BootMarkerIndicationColor` | `org.springframework.tooling.ls.eclipse.commons` | 219,236,207 | 1.07:1 (fg on it) | `LanguageServerCommonsActivator`: theme listener copies it into the editors annotation key `STS4BootMarkerIndicationColor` and repaints highlights | `css/e4-dark_sts4_prefstyle.css`, `e4_dark` only | SUCCESS_TINT |
| `org.springframework.ide.eclipse.xterm.background` | `org.springframework.ide.eclipse.xterm` | 255,255,255 | 1.16:1 (fg on it) | `TerminalView.createUrl` (theme registry, listener re-creates the terminal page) | `css/terminal-dark-theme.css`, `e4_dark` only | BACKGROUND |
| `org.springframework.ide.eclipse.xterm.foreground` | `org.springframework.ide.eclipse.xterm` | 0,0,0 | 1.0:1 on the dark background | same | same | FOREGROUND |
| `org.springframework.ide.eclipse.xterm.selection` | `org.springframework.ide.eclipse.xterm` | 45,45,45 | drawn at alpha 51, invisible on dark | same | same | FOREGROUND |
| `org.springframework.ide.eclipse.xterm.cursor` | `org.springframework.ide.eclipse.xterm` | 0,0,0 | invisible on dark | same | same | FOREGROUND |
| `org.springframework.ide.eclipse.xterm.cursorAccent` | `org.springframework.ide.eclipse.xterm` | 0,0,0 | glyph under the block cursor | same | same (sets 255,255,255, equal to its cursor, which hides the glyph) | BACKGROUND |

The Spring rows depend on Decision Gate 1.

The terminal cursor accent is the glyph drawn under the block cursor. It must equal the terminal background, so it uses the same role.

## ALREADY_COVERED

| Key | Note |
|---|---|
| `org.eclipse.mylyn.tasks.ui.colors.category.gradient.end` | in the table since R8 (RAISED) |
| `org.eclipse.mylyn.tasks.ui.colors.foreground.{today.scheduled,past.scheduled,thisweek.scheduled,task.active}`, `.category.gradient.start`, `org.eclipse.mylyn.tasks.ui.color.text.quoted` | in the table since R8 |

## OBSOLETE_UNUSED

| Key | Evidence |
|---|---|
| `org.eclipse.mylyn.context.ui.colors.foreground.landmark` | `org.eclipse.mylyn.context.ui` 4.11 and 4.12 declare no color definitions. `ColorMap` hard-codes `LANDMARK = 80,140,200` ("TODO: use themes?"). The id string occurs in no Mylyn jar of either package |
| `org.eclipse.mylyn.context.ui.colors.foreground.predicted` | same; `ColorMap.GRAY_*` are hard-coded |
| `org.eclipse.mylyn.context.ui.colors.foreground.uninteresting` | same |
| `org.eclipse.mylyn.context.ui.colors.foreground.relationship` | same; `ColorMap.RELATIONSHIP = 0,254,0` hard-coded |
| `org.eclipse.e4.tools.emf.ui.color.{default,tag,string,comment,procInstr}` | no color definitions in `org.eclipse.e4.tools.emf.ui`. The XMI editor's scanners read `COLOR_org.eclipse.e4.tools.emf.ui.{DEFAULT,TAG,STRING,XML_COMMENT,PROC_INSTR}` from its own `IResourcePool`. The `.color.` ids occur in no `org.eclipse.e4.tools*` jar |

## OUTSIDE

| Key | Reason |
|---|---|
| `org.eclipse.mylyn.tasks.ui.colors.foreground.today.completed` | default 107,169,128, 4.85:1, readable |
| `org.eclipse.mylyn.tasks.ui.colors.foreground.completed` | default 128,128,128, 3.39:1, intentionally de-emphasized; readable by the R8 precedent (`MemoryHistoryUnknownColor` 114,119,129) |
| `org.springframework.ide.eclipse.boot.dash.MutedTextDecorColor` | default 169,169,169, 5.7:1, readable |
| `org.eclipse.pde.genericeditor.extension.editor.color.{text,comment,quote,tag,attribute,header}` | syntax colors of the PDE target-platform editor (defaults 0,128,0 / 0,128,0 / 0,0,255 / 0,128,128 / 128,0,0 / 192,192,192, read from the theme registry by its presentation rules). The `plugin-color-coordination` spec keeps syntax keys with the editor integrations, and the theme already has `XML_TAG`, `XML_ATTRIBUTE`, `XML_COMMENT` and `STRING` tokens. Deferred to `editor-integrations` |
| Mylyn WikiText `block-h1` … `block-bc` | CSS fragment strings, not colors (unchanged from R8) |
