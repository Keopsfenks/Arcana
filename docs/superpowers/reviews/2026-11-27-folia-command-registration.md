# Folia startup command registration — 2026-11-27

## Observed problem

The user's Folia 26.3 build 28 log shows Arcana enabled successfully and the
single-file game-test skill loaded, followed by `Unknown or incomplete command`
for `arcana`. The plugin was active; its dynamic command registration disappeared.

During startup, `ArcanaPluginCommand.register()` inserted the new command on the
startup thread, while `ArcanaImpl.unregisterCommand()` queued cleanup for the
global thread. That later name-based cleanup removed the newly inserted command;
the following command-tree sync therefore saw an empty registration.

## Change

- Keep old-command cleanup, insertion and tree sync in one Folia global-owner
  callback for individual registration/removal. Already-owned calls stay inline.
- Move the complete startup registration batch into one global callback on Folia,
  preserving begin/end batching and sync coalescing. This deliberately submits the
  Folia enable batch to the next global tick; Paper keeps synchronous behavior.
- No new public API method, metadata command workaround, dependency or permission
  change. Existing commands continue to use the same frontend contracts.

## Verification

Two backend regression cases failed on the original code: deferred cleanup erased
the newly registered root, and registration mutated the map before the global
callback. Both pass with the fix. Already-owned and Paper behavior remain covered.
Three batch ownership/order tests cover the single queued Folia batch, invocation
from an existing owner, and synchronous Paper behavior. Existing reload-preflight
checks also pass. The fixture calls the real private batching implementation.

```powershell
.\gradlew.bat build --no-parallel --max-workers=2 -x clean --console=plain
python tools/verify-skills-distribution.py
python .git/verify-arcana.py
```

Full build GREEN in 2 minutes 58 seconds: 1663 tests across
204 suites, zero failures/errors/skips; seven NMS linkage checks;
96 retained v1 descriptors, 125 native entries, 10 disabled packaged examples,
1595 source paths and documentation links verified.

Artifact: `build/libs/arcana-2026.47-all.jar`, 16070029 bytes.
SHA256: `dba717b65059b76aee37bb4ac15e3162b751ff554eba473d1aa7f7a2025fecf7`.

## Local delivery

The verified JAR is staged as
`test-server-folia-26.3/plugins/update/arcana-2026.47-all.jar`.
The server's `bukkit.yml` uses `update-folder: update`; this is the documented
[Paper plugin update workflow](https://docs.papermc.io/paper/updating/#step-2-update-plugins).
The running plugin JAR, world, configuration and game-test YAML were not replaced.
The user controls stop/start. The initial startup was observed in their log;
the corrected command's live behavior awaits their restart. Unit tests and the
full build are not claimed as a post-fix live-server test.
