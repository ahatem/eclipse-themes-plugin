# Source Evidence

Baseline: branch `feat/atomic-deterministic-theme-import`, HEAD `d6c6c82364c1a2f1547775704323a342138ab566`, re-verified on 2026-10-18 before this plan was written. No production or test file is modified by this planning change. This file records inspected source, not executed red tests. Tasks 0.2 and 6.1 require regressions at implementation time.

## Import path

`bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/eclipse/ThemeRepository.java`

- `importTheme` (lines 61–104) derives the destination from the source basename: `new File(installedThemesDir, file.getName())` (line 62).
- It refuses when the locator is occupied or binding-reserved (lines 66–72). This closes the prior silent overwrite but leaves only "new record or failure".
- Parsing is performed from one source read (lines 79–82), then the source is opened again and copied (lines 85–89). The parsed metadata is therefore not guaranteed to describe the copied bytes.
- The catalog is updated (line 92) after the file copy and identity write. There is no staged-byte validation, no temporary location, and no replacement operation. If binding persistence fails, `discardNewDestination` attempts to delete the copied file but ignores its own failure, so an unbound orphan can remain.
- `discardNewDestination` (lines 106–117) ignores its own delete failure.
- `loadInstalledThemes` (lines 155–…) lists direct, non-directory children and lowercases the name with `Locale.ROOT` to accept `.xml`. A non-catalogued subdirectory would not be admitted as a theme. The repository accepts `.XML`; `ParserFactory` does not (see below).
- There is no `updateTheme` or `removeTheme` repository operation. The preference page deletes the file directly.

## Identity bindings

`bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/eclipse/ThemeIdentityBindings.java`

- `identityFor` (lines 19–20) resolves through `identityFor(locator, false)`.
- When no binding exists, `identityFor` generates `local:<UUID>`, writes it, and returns it (lines 47–60). There is no established-only read; callers cannot distinguish "existing identity" from "identity just created".
- `requireUnused` (lines 23–27) rejects a reserved locator.
- `newIdentityFor` (lines 29–31) requires a new binding.
- Atomic metadata replacement already exists (`Files.createTempFile` in the same directory plus `replace`, lines 50–57), but there is no per-key removal API.

## Removal path

`bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/eclipse/preferences/EclipseThemesPreferencePage.java`

- `removeSelectedTheme` (lines 592–627) confirms, then calls `selectedTheme.getFile().ifPresent(File::delete)` (line 606) and ignores the `boolean` result.
- The identity binding is never removed, so the locator remains reserved after a successful deletion.
- It resets the current theme (lines 607–610) and then unconditionally reports success (line 623), even if the file still exists. Exceptions only cover what is thrown; `File.delete()` does not throw.
- Import (lines 564–590) reloads and verifies that the returned record is present before reporting success (lines 572–584). `Theme.equals` compares only `logicalId`, so that check is identity-based in substance. The page still lacks an update action and has no policy for a repeated import beyond creating another record.
- The page only exposes an import button and a remove button; no update button exists.

## Extension and locale behavior

- `ParserFactory.getParserFor` accepts only `fileName.endsWith(".xml")` (line 22), while the repository filter is ASCII-lowercased. A file named `THEME.XML` is listed and then rejected.
- `ThemeType.nameCapitalized` uses `toUpperCase()`/`toLowerCase()` without a locale (line 7), and `ThemeType.from` uses `type.toLowerCase().equals("dark")` without a locale (line 11). In a Turkish default locale, `"DARK".toLowerCase()` produces a dotless `ı` and classification fails.
- The preference page search also lowercases with the default locale, but that is presentation/search and is deferred (proposal Deferred section), not part of this import contract.

## Platform probes

Executed on 2026-10-18 with Temurin OpenJDK 21.0.12.1 on Windows, in a local temporary directory. These probe only the JDK/filesystem behavior needed by PM-I2 and the new-install no-overwrite rule; they are not a cross-platform matrix.

- `Files.move(staged, target, ATOMIC_MOVE, REPLACE_EXISTING)` over an existing target succeeded; the target contained the new bytes and the staged path was gone.
- `Files.move(staged, target)` with an existing target failed with `FileAlreadyExistsException` and left the existing target unchanged.

The probe supports treating the Windows local filesystem as capable of the planned atomic update and non-replacing new-install move. Network, overlay, and other filesystems still require the truthful-failure behavior in PM-I2 and remain unverified.

## Boundaries confirmed

- `ThemeManager.importTheme` (lines 41–43) only forwards to the repository; no additional transaction or staging exists.
- JavaSE-17 execution environment, product metadata, dependencies, and the target definition are unchanged by this plan.
- No external API, extension point, archive format, revision ID, content digest, or source-ID trust is introduced.
