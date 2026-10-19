# Runtime evidence: plug-in color coordination (R8 closure)

Measured with a scripted real workbench (a probe bundle that drives the Eclipse Themes preference page, Appearance page and
workbench windows, then reads the instance preference nodes, the live JFace color registry and a preference-change trace). The final
table order (JDT Javadoc keys first) was in place for every run below.

| | Eclipse 4.40 | Eclipse 4.41 |
|---|---|---|
| Platform / theme bundle | `4.40.0.v20260601-0713` / `0.15.100` | `4.41.0.v20260828-1142` / `0.15.200` |
| Java (launch VM) | 21.0.11 | 25.0.4.1 |
| OS / ws | Windows 11, win32/win32 | Windows 11, win32/win32 |
| Plugin | `1.1.0.202610182337` | same |

Logs: `pc<ver>-r1..r4` (the apply sequence with restarts), `pu<ver>-u1` (user values), `pw<ver>-w1` (order of the plug-in's
release against stock Dark's writes). Step scripts: `r1..r4.txt`, `u1.txt`, `w1.txt`. `PCOL <step> keys=N present=P owned=O` counts the
68 declared keys that have an instance value (`present`) and those the ownership record says are the plugin's (`owned`).

## Results (identical on 4.40 and 4.41)

| Scenario | Result |
|---|---|
| One Apply of a dark theme (Atom One Dark), started from stock Dark | 68 present, **66 owned**, 2 kept as not ours (see below); live JFace colors equal the stored values |
| Dark → dark (Dracula), dark → dark (Catppuccin Mocha) | 66 owned, values updated in place |
| Dark → light (Solarized Light) | owned keys removed, `present=2` (the same 2 keys that were never ours); live colors return to the platform defaults, **including the Javadoc hover** (it defaults to the information colors; before the table was reordered it stayed stale) |
| External Appearance choice of stock Light, then restart | owned keys removed without a restart, nothing re-written; after the restart `present=2`, `owned=0` |
| External Appearance choice of stock Dark (`w1`) | the plugin's release runs first and removes only its own values; stock Dark's writes follow (`PREFCHG` order: 39 release events, then 197 stock events, all after the last release). After the switch 63 (4.40) / 67 (4.41) keys hold stock's values and `owned=0`. Leaving stock Dark again leaves `present=0`, so stock's values did not become anyone's user value |
| Restart with ownership (`r4` start) | `owned=66` is read back from the record and the keys are intact |
| Disable coordination | owned keys removed; the keys that remain are stock Dark's (the plugin hands back to the prior stock theme) |
| User values (`u1`, started from stock Light: `HYPERLINK_COLOR`, an EGit diff color, JDT `content_assist_proposals_background` set by hand) | after Apply `present=68`, `owned=64`: the user's three values (and one stock residue) are kept; unchanged across dark → dark and after disabling |
| Owned key changed by hand (`HOVER_BACKGROUND=5,5,5`), then a different dark theme applied | `owned=63`: the key is released and keeps the user's value; disabling coordination leaves it |

## Observations

- The 2 keys that are present but not owned after an Apply that started from stock Dark are JDT's
  `content_assist_proposals_background/foreground`. Stock Dark wrote them; they are not removed when its theme is left, so the plugin
  correctly treats them as a user value (conservative rule) and does not rewrite them. They are dark popup colors and stay readable.
  The residue depends on the path: a fresh `w1` run that left stock Dark ended with `present=0`.
- All 68 declared keys are written even on 4.40; the 4 inline compare keys are written but 4.40 does not read them.
- Stock Dark overwrites a user value when it is applied and removes it when it is left (also when the user's value was set under
  stock Dark): measured, and the reason the plugin writes nothing through CSS.
- One launch of the 4.41 sequence stalled at shutdown for the 300 s harness limit after `DONE` (`r1`, first run) and was killed; its
  restart steps therefore saw unsaved state. It did not recur: `r1` to `r4` were rerun from fresh directories and are the logs here.
  The cause is not established; no plugin code was running at the time (the workbench `close()` was already in progress).

## Not verified, stated plainly

- **Visual proof of every surface.** The probe reads stored and live color values; popups, EGit, Mylyn and Marketplace views were not
  inspected one by one. Visual evidence for the workbench chrome comes from the `dynamic-workbench-theming` change.
- **CSS warning check.** The Eclipse log was not checked for CSS selector warnings after the final sheets.
- **Page text for kept values.** The runtime probe does not read the status line; the "N plug-in colors kept your own values" text is
  covered by `WorkbenchSectionModelTest`, the key names by `WorkbenchDiagnostics` tests.
- **Linux/GTK, macOS, other Windows versions, Java other than the launch VMs above**: not run, not claimed.
