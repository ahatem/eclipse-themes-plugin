# OpenSpec Setup

This pack contains project OpenSpec content but intentionally does not commit generated command/skill files for a specific AI tool.

From the real repository root:

```bash
npm install -g @fission-ai/openspec@latest
openspec init
openspec validate --all --strict
openspec view
```

`openspec init` installs the appropriate commands/skills for the coding assistants you select. OpenSpec's current default flow is conceptually `explore → propose → apply → sync/archive`; expanded profiles add finer-grained new/continue/fast-forward/verify commands.

After upgrading OpenSpec or changing selected tooling:

```bash
openspec update
```

Useful checks:

```bash
openspec list
openspec list --specs
openspec status --change eliminate-destructive-preference-reset
openspec validate eliminate-destructive-preference-reset --strict
openspec validate --all --strict
```

Do not maintain separate hand-written copies of OpenSpec slash commands for Codex, Claude, Cursor, OpenCode, etc. Let OpenSpec generate the integration expected by the installed tool/version.
