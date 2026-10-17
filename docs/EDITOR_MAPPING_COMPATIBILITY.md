# Editor mapping corrections

PM1–PM6 are approved. The bounded corrections are covered by focused tests and
real Eclipse 2024-09 consumer/listener checks. See the
[verification record](../openspec/changes/archive/2026-10-17-correct-editor-preference-mappings/verification.md)
for exact evidence and compatibility limits.

Legacy XML keeps separate `selectionBackground` and `sourceHoverBackground`,
`comment` and `xmlComment`, and `constant` and `staticFinalField` roles, including
bold, italic, underline and strikethrough flags. `singleLineComment` is a legacy
alias for `comment`. Missing XML comments inherit the whole generic-comment
token; missing static-final fields inherit the whole constant token, then the
existing static-field/field/foreground chain. Neither fallback runs in reverse.
Missing selection background still inherits background, independently of hover.

For example, these values remain independent in either document order:

```xml
<selectionBackground color="#123456"/>
<sourceHoverBackground color="#abcdef"/>
<comment color="#123456" italic="true"/>
<xmlComment color="#abcdef" bold="true"/>
<constant color="#123456"/>
<staticFinalField color="#abcdef" underline="true"/>
```

For true aliases, a valid canonical spelling wins regardless of order. At equal
rank, the last valid occurrence wins. The winner supplies the whole token; losing
style flags are never merged. Accepted conflicting colors or effective flags
produce a warning with the role, source names/occurrences and selected winner.
Equivalent candidates produce no conflict warning. Invalid/missing colors retain
the existing skip behavior. This does not define a new XML format or relax XML
security. The alias table is in the [change design](../openspec/changes/archive/2026-10-17-correct-editor-preference-mappings/design.md).

JDT uses static-final fields, CDT enumerators use constants, JDT/CDT use generic
comments, and PDE/Ant use XML comments. Explicit source-hover background controls
only JDT's source-viewer hover color/system-default pair. Omission removes both
managed overrides and exposes editor/product defaults; background fallback does
not count as explicit presence. Other hovers, native tooltips and workbench
surfaces are outside this mapping. The preference couplings are based on Eclipse
2024-09 (R4_33); CDT is covered by isolated adapter tests, with no CDT runtime
claim or expanded OS/version support matrix.

PDE and Ant support color, bold and italic in these integrations. They do not
render the model's underline/strikethrough flags; those unsupported preference
suffixes are now left untouched during apply/reset, including historical values.
This prevents boolean-style notifications from being parsed as colors. All four
flags remain independent in the theme model and other supporting integrations.

PDE live updates use only the PM6-approved
`PDEPlugin.getDefault().getPreferenceStore()` internal entry point, isolated to
the optional PDE integration. It is version-fragile and verified against R4_33
PDE UI `3.15.300.v20240815-2201`. Typed Boolean notifications accompany explicit
instance values, including flags equal to defaults and repeat application. No
substitute scoped-store fallback claims success. Missing/incompatible access is
reported through the existing integration failure path while other integrations
continue. A public/upstream replacement remains deferred R6 compatibility work.

The four existing optional JDT roles (`inheritedMethod`, `inheritedField`,
`templateArgument` / legacy `typeArgument`, and `deprecated` / legacy
`deprecatedMember`) are enabled only by explicit tokens. Omission writes
`.enabled=false` and removes their color/four style overrides. This is the bounded
legacy-XML policy, with no universal future missing-token semantics. In particular,
deprecated-member highlighting no longer inherits its default-enabled state
during sparse-theme application. Stop/reset removes managed enable overrides and
exposes defaults. Mandatory semantic roles keep their existing fallback behavior.

Debug instruction colors now write `currentIPColor` and `secondaryIPColor` in
`org.eclipse.ui.editors`, as declared by Eclipse 2024-09 debug annotation specs.
Apply and stop/reset preserve `currentIPIndication` and `secondaryIPIndication`
exactly, including absence or historical malformed RGB-shaped values. Correcting
the color destinations cannot recover unknown original booleans. Affected users
must choose desired indication values manually at **Window → Preferences →
General → Editors → Text Editors → Annotations** on the verified Windows target.
Select **Debug Current Instruction Pointer** or **Debug Call Stack** and choose
the desired **Text as** indication setting. The hierarchy, both entries and that
control were verified by instantiating the real preference page. This does not
recover unknown originals; whole-node deletion and guessed repair are not recovery steps.

Reapply using the existing explicit Apply action to receive corrected mappings.
Themes relying on previous document-order collisions, cross-role overwrites or
default re-enablement can render differently. Bundled themes and synthetic
one-sided/alias cases are tested; non-bundled legacy compatibility remains an
acknowledged evidence limitation. XML is not rewritten. Reset removes declared
instance overrides to expose defaults, without reconstructing earlier user
values. Downgrading may restore the defects and is not a recovery mechanism.
