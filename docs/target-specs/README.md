# Target Spec Program

These files convert the **full audit** into future-state requirements and research acceptance criteria without lying about what is already implemented.

They are **not** canonical current OpenSpec specs. `openspec/specs/` remains the accepted current behavior. A future agent must re-verify the relevant audit evidence, then use the applicable target requirements to create one bounded `openspec/changes/<name>` proposal/delta against the specs that exist at that time.

Why this split exists: pre-creating dozens of active OpenSpec deltas would make later deltas stale and order-dependent because `MODIFIED Requirements` replace whole requirement blocks at archive time. The target program preserves the whole audit while keeping OpenSpec archive semantics correct.
