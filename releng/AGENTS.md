# Releng Agent Guide

Root `AGENTS.md` applies. This subtree owns target-platform and p2/update-site behavior.

- Changes here can alter dependency resolution or installability even when Java code is untouched.
- Keep the older compatibility target and the current-release validation strategy conceptually separate.
- Do not replace release repositories, target IUs, or qualifiers casually; record the reproducibility/compatibility reason.
- Packaging/release changes require a clean reactor build and, where practical, a p2 install smoke test.
- Stable release artifacts should be built once, validated, then published immutably. Avoid rebuilding "the same release" through separate paths with different qualifiers.
- Main/develop snapshots and stable release channels must not silently overwrite one another.
