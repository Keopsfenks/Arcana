# Arcana Skills verification and review

An independent, read-only whole-branch review covered `bc33ba56..e2de5f93` against the approved design and implementation plan. It reported six material findings, no deferred minor findings and no behaviors declined for judgment. The author addressed all six in one regression-driven fix pass.

| Finding | Regression and final behavior |
| --- | --- |
| Reload commit exceptions were logged but reported as success | `ReloadPreflightTests`: the real lifecycle boundary returns false after a handler failure; independent lifecycle work still runs. |
| Literal nested casts omitted child component dependencies | `ExecutionLifecycleTests`: removing a reachable child dependency cancels waiting ancestors and rejects new admissions, including cyclic cast graphs. |
| Registration raced with owner disable and engine shutdown | `ExecutionLifecycleTests`: controlled concurrent registration/closure leaves no active late registration. Registry admission is serialized with lifecycle transitions. |
| Queued events selected the configuration installed a tick later | `TriggerAdapterTests`: emission binds to its snapshot; a replaced snapshot is discarded, and an empty trigger index schedules no handoff. |
| Retired Folia victims returned a whole-cast cancellation | `SkillBoundaryTests`: delayed entity and location effects return `NO_TARGET` for victim retirement; explicit root cancellation remains cancellation. |
| Real expression compilation errors lost the YAML field path | `CoreSkillsIntegrationTests`: the production expression compiler reports the exact effect argument path and retains compilation position. |

Each regression was observed failing before its corresponding fix and passing afterward. The reload test explicitly executes the private lifecycle method rather than substituting its mock implementation.

Final local verification on 2026-11-23:

- `gradlew.bat build --no-parallel --max-workers=2 -x clean --console=plain`: successful, 1,389 tests; zero failures, errors or skipped tests.
- Seven NMS linkage checks passed.
- Distribution inspection confirmed the API, runtime, separate built-in components, three disabled example resources, Arcana metadata, MIT license and NOTICE. No old owned namespace or libreforge classes were present.
- Package paths, public Java type names, Gradle module paths and owned documentation links passed validation.
- User-approved bStats ID: `35538`.

Live Paper/Folia server startup and gameplay were not exercised. Automated scheduling tests use controlled scheduler boundaries; they do not replace a live server trial.
