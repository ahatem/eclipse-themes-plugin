# Design: extend plug-in color coverage

## Context

`PluginColorTable` is the full contract for foreign keys the plugin may write. `PluginColorWriter` applies the table with value-exact
ownership:

- it writes a key only when absent;
- it rewrites a key only while unchanged;
- it releases keys on light themes, when coordination is off, and when another Eclipse theme is chosen;
- it skips a key whose required bundle is missing.

`PluginColors` derives each value from the published `WorkbenchPalette` through a `PluginColorSource` role. This change only adds
table entries.

## Goals / Non-Goals

Goals: coordinate the evidence-qualified keys listed in `evidence/key-classification.md` with existing roles and rules.

Non-goals:

- new roles or derivations;
- new preference nodes;
- syntax colors (PDE target editor);
- keys without readers (Mylyn Context, e4 tools);
- runtime verification of Spring Tools (decision 1A: source-verified only).

## Decisions

1. **Table entries only.** Every new key is a theme color definition in `org.eclipse.ui.workbench`. The writer already handles that
   node, and the owning plug-ins read the color registry with theme listeners. So the existing write path recolors them live, the same
   way it does the R8 Mylyn and EGit keys.
2. **Owning-bundle constants.** Add `SPRING_BOOT_DASH = org.springframework.ide.eclipse.boot.dash`,
   `SPRING_XTERM = org.springframework.ide.eclipse.xterm` and `SPRING_LS_COMMONS = org.springframework.tooling.ls.eclipse.commons`.
   These are the `Bundle-SymbolicName`s at the evidence commit. Mylyn uses the existing `MYLYN_TASKS`.
3. **Roles.**
   - `past.due` uses ERROR, matching its red default and meaning.
   - `overdue.for.others` uses WARNING. Its default is a darker red, and a separate hue keeps "overdue for someone else" apart from
     the user's own overdue tasks.
   - `incoming.background` uses INFO_TINT, a field background with readable foreground on top.
   - Boot Dashboard decorations use SUCCESS and INFO, the text-grade versions of their green and blue defaults.
   - The Boot marker is a line highlight under editor text, so it uses SUCCESS_TINT, as the coverage highlight does.
   - The terminal uses BACKGROUND and FOREGROUND. Spring draws `selection` at alpha 51, so a FOREGROUND selection gives the usual light
     veil, matching Spring's own dark value. `cursorAccent` uses BACKGROUND, which keeps the glyph under the block cursor visible.
4. **Placement.** The new Mylyn keys join section 5 next to the existing Mylyn keys. Spring Tools gets a new section 6. No new key
   defaults to another declared key, so release order has no constraint here (unlike the Javadoc/information pair).
5. **Provenance comment.** The class comment now says a key is declared when its default is unreadable on dark and a reader exists.
   It points to both evidence files.

## Risks / Trade-offs

- Spring key names or readers may change in a future release. Bundle gating plus value-exact ownership limits the damage to an inert
  owned key, which the existing paths still release. The table comment records the evidence commit.
- Users who already set any of these keys keep their values: the existing first-write rule applies.
- Looking better than stock Dark on the three Mylyn surfaces is intentional (decision 2A).

## Migration / Rollback

No migration. New keys are taken on the next explicit dark Apply. Release paths work over the declared table. If the change is
reverted after the new keys were written, the writer keeps their record entries but never visits them, so their values stay behind as
stray instance values. To roll back cleanly, apply a light theme or disable coordination before downgrading. This is the same exposure
R8 documents for uninstalling. It is recorded here and not engineered further, since a revert is not a planned path.
