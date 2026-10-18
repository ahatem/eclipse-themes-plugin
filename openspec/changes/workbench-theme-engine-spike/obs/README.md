# Evidence provenance

These are observations, not a contributed theme or a reusable probe. No Java, CSS, bundle,
launcher or probe build implementation is committed. The disposable instrumentation and full
outputs remain in ignored `tmp/workbench-spike/v2/`. Earlier local experiments remain in
`tmp/workbench-spike/`; their inherited extract was preserved locally as
`v2/inherited-evidence-extract.txt` because its JDK/OS header was inaccurate.

## Primary record

`runtime-evidence.txt` contains selected **original lines** from W01–W05, W07–W08,
G03–G05 and W10, in run order. W06 is an interrupted instrumentation attempt, not an
acceptance run. W09 repeats W08 on a fresh workspace and is hashed, but is omitted from the
extract to avoid duplicating the same evidence. The extraction retains:

- environment, inventory, steps, active/persisted state, postconditions, exceptions and registration;
- Appearance combo values, restart prompts, accessibility flags and layout instrumentation;
- widget class/ID and RGB values for the representative surfaces;
- all non-boot preference differences in W01/W02, and theme-node/renderer-node differences in
  the other runs. This preserves the exact broad CSS preference effects once without repeating them.

Trailing whitespace is trimmed. The nondeterministic `css=<Java object identity>` field is omitted. No RGB value or active ID is
changed. Initial preference inventories remain in full local logs; explicit absent `themeid`
state is retained. `log-hashes.csv` records SHA256 hashes of the full original local logs, not the
extract. Run IDs are local labels; they are not test-framework pass/fail assertions.

`IThemeEngine-javap.txt` is the complete public signature listing from the target binary.
`missing-css-diagnostic.txt` preserves the platform error entry from W05. Matching source JARs
were inspected locally; whole upstream sources are not vendored here.
`theme-bundle-manifest.txt` is the exact binary's main manifest section, including the friend
export and source reference. `source-hashes.txt` identifies the matching source JARs.

## Visual evidence

- `windows-a.png`: W08, own theme; restored view/editor areas.
- `windows-live-dark.png`: W08, switch from A to built-in Dark.
- `windows-dark-restart.png`: W10, persisted Dark after a new process.
- `gtk-a.png`, `gtk-dark.png`: G05, GTK3/X11 own theme and switch to Dark.

Each included image was opened and inspected. Windows Dark captures show anomalous tab-label
glyphs, including after restart. This is an observation of this test installation, not an
attributed Eclipse defect. Do not infer exact visual restoration from RGB equality. GTK captures
use SWT widget printing; Windows captures use a raised shell/display copy. Earlier blank,
occluded or unrelated-window captures are excluded. No Appearance screenshot is used as evidence;
Appearance results use actual combo values, page OK, active ID and preference observations.

## Experiment isolation and reconstruction

Windows uses the existing local Eclipse 4.33 test installation, JDK 21, a local probe bundle,
and isolated `-data` / `-configuration`. GTK uses the official Eclipse Platform 4.33 Linux archive,
Ubuntu WSL2, GTK3 and `GDK_BACKEND=x11` on WSLg. Probe provisioning uses a simpleconfigurator
bundle list with dropins reconciliation disabled for that isolated configuration. No product
source or shipped extension registration is changed. This is not p2 install/uninstall testing.
The final Windows launch also pins the simpleconfigurator URL as a VM property. One earlier
auxiliary launch emitted the old probe's default log; it was retained locally and is not used
to establish any result. Actual cited runs identify the v2 observation format and complete DONE.

The existing Windows test installation contains an older editor-theme plugin
`1.1.0.202610171415`; it is not asserted to be the current 7ebb343 implementation. Findings about
current editor intent use inspected source at 7ebb343 and are labelled as code evidence. GTK is
the stock Platform archive plus the probe. Neither environment establishes a complete integrated
R8 product or arbitrary third-party appearance-plugin coexistence.

To reconstruct the experiment, use a temporary bundle outside the reactor with:

1. A theme ID `spike.probe.theme.a` / label `Spike A (research)` and one base stylesheet.
2. A stylesheet contribution restricted by `themeid refid` to the built-in Light ID.
3. A startup observer using the workbench's IThemeEngine service on the SWT thread, explicit
   active-ID checks after setTheme, instance-preference differences and representative widgets.
4. The call sequences in `runtime-evidence.txt`; preserve the same workspace between restart runs.

The final A stylesheet has no imports or explicit renderer class: Shell `#f8bbd0`, `.MPartStack`
background and selected/unselected tab gradients `#d1c4e9`, `.MTrimBar`/ToolBar `#ffe082`,
`#org-eclipse-ui-StatusLine` `#a5d6a7`, Tree `#ffcc80`, Table `#b2ebf2`, and
SashForm/`.MPart`/`.MCompositePart` `#f8bbd0`. The tab gradient properties are
`swt-selected-tabs-background` / `swt-unselected-tabs-background`. B sets Tree/Table/ToolBar
to `#80cbc4` only on Light. C sets Tree to `#ce93d8`. These are tested selector/property ideas,
not a theme design or a stable cross-version CSS API promise.

W01–W05 used an earlier A stylesheet that explicitly selected Eclipse's internal CTabRendering
renderer. W07 onward removed that directive; W08/G05 prove the listed surfaces without directly
naming an internal renderer. The earlier directive is not proposed as a product dependency.

No committed probe is required to assess these findings. Recreating executable instrumentation
for a later independent verification is a separate local activity; this record does not create
a new contributor toolchain or test obligation.
