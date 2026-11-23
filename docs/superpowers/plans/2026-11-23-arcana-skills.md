# Arcana Skills Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the approved, independent YAML skill/effect engine as an integrated `arcana-skills` module with one class per effect, condition and trigger.

**Architecture:** Public Java contracts live in `arcana-api/core/skills`; Kotlin runtime code lives in `arcana-skills` and depends only on that API and server APIs. The core plugin owns lifecycle/commands and the existing Arcana distribution shades the runtime. Registries resolve IDs to separate component classes, never a growing central type switch.

**Tech Stack:** Java 21 public API, Kotlin 2.3.21 implementation, Bukkit/Paper APIs, existing Arcana expression and scheduling APIs, SnakeYAML safe parsing, JUnit 6/MockK.

**Spec:** `docs/superpowers/specs/2026-11-23-arcana-skills-design.md` (approved by user, including separate component classes).

## Global Constraints

- Public namespace `com.keopsfenks.arcana.core.skills`; runtime namespace `com.keopsfenks.arcana.skills`.
- No libreforge source, dependency, or format compatibility; no XP, skill tree, GUI editor, or database feature.
- Single installed Arcana JAR, Java-accessible API, no module dependency cycle.
- One class/file for each built-in effect, condition and trigger; extensible ID registry.
- Failed reload preserves active definitions and jobs, including the general Arcana reload path.
- Actor/target/world state is accessed in its Arcana scheduler context; no delayed Bukkit Event references.
- Maximum 16 nested casts, 1,024 executed steps, 100 repeat iterations, 24-hour delay; reject NaN/infinity.
- Same-player/same-group cooldown reservation is atomic and monotonic; admitted cast keeps cooldown on later error.
- Unregistered extension dependencies reject new work and cancel dependent pending executions.

## Review Focus

1. Cross-region target scheduling must not read the victim's live state on the caster's thread (tasks 3, 4, 5).
2. A bad global reload must fail before `cancelAll`, not just preserve the map after destroying pending work (task 6).
3. Nested casts and delayed repeats must share cancellation/budget state and cannot trigger recursive damage forever (tasks 3, 4).
4. Unregistered external components cannot remain callable through cached compiled closures (tasks 1, 3, 6).
5. Case-sensitive paths, disabled sample skills, boxed YAML numbers and expression syntax errors must produce consistent startup behavior (tasks 2, 6).

## Shared interfaces and file map

API root: `arcana-api/src/main/java/com/keopsfenks/arcana/core/skills/`.
Runtime root: `arcana-skills/src/main/kotlin/com/keopsfenks/arcana/skills/`.
Tests mirror runtime packages under `arcana-skills/src/test/kotlin/`.

- `SkillComponent`: common immutable validated ID. `SkillEffect`, `SkillCondition`, `SkillTrigger`: separate abstract Java component types.
- `SkillEffect.compile(SkillArguments, SkillCompiler): CompiledSkillEffect`; `CompiledSkillEffect.execute(SkillExecution): CompletionStage<SkillResult>`.
- `SkillCondition.compile(SkillArguments, SkillCompiler): CompiledSkillCondition`; `CompiledSkillCondition.test(SkillContext): boolean`. Conditions execute in caster context and use copied invocation data.
- `SkillTrigger.listen(Consumer<SkillContext>): AutoCloseable`; each adapter class owns its listener implementation. The engine owns returned listener handles.
- `SkillArguments`: immutable nested argument data plus source path, typed readers, explicit missing/type errors. No SnakeYAML type appears in the public API.
- `SkillCompiler`: number/expression, nested effect and nested condition compilation. `SkillNumber.evaluate(SkillContext): double` abstracts numeric constants and compiled Arcana expressions.
- `SkillContext`: immutable invocation data with caster reference/UUID, optional living victim reference, trigger scalars, and copied actor/target information. Builder entry `SkillContext.builder(Player)`. Runtime enriches snapshots on proper entity schedulers before filters/conditions; conditions do not read foreign live entity state.
- `SkillExecution`: context, actor/target effect scheduling, location effect scheduling, ordered child execution, delay, nested cast and text/number evaluation helpers. Shared execution state tracks budget, cancellation, registrations and tasks.
- `SkillResult`: immutable status/details including SUCCESS, UNKNOWN_SKILL, DISABLED, NO_PERMISSION, FILTER_FAILED, CONDITION_FAILED, CHANCE_FAILED, COOLDOWN, NO_TARGET, CANCELLED, ERROR, DEPENDENCY_UNAVAILABLE.
- `SkillRegistration extends AutoCloseable`: ID, owner and active state, idempotent non-throwing close.
- `SkillInfo` and `SkillReloadResult`: immutable administrative views with loaded/disabled counts and path-qualified diagnostics.
- `SkillEngine`: `cast(String, SkillContext)`, `trigger(String, SkillContext)`, `registerEffect(Plugin, SkillEffect)`, equivalent condition/trigger registration, `reload()`, `skills()`, `cancel(UUID)`, `shutdown()`.
- `Arcana.getSkillEngine()`: engine access from the existing API bridge. Core initializes before dependent plugins use it on enable.

### Task 1: Component API and owned registries

**Files:** Create API files listed above; runtime `registry/ComponentRegistry.kt`, `registry/OwnedRegistration.kt`; create `arcana-skills/build.gradle.kts`; modify `settings.gradle.kts`; tests `registry/ComponentRegistryTests.kt` and API `skills/SkillContextTest.java`.

**Produces:** validated Java contracts; typed component registration and ownership. Runtime Gradle group `com.keopsfenks`, version from root; compileOnly API/Paper/SnakeYAML, server and parser dependencies present in tests. Do not wire core startup until task 6.

- [ ] Write failing tests: duplicate IDs reject; invalid IDs reject; immutable argument/context maps reject mutation; closing a handle twice is safe; owner close deactivates every owned handle; removing a component reports the affected dependency.
- [ ] Run `gradlew.bat :arcana-api:test :arcana-skills:test --no-parallel --max-workers=2 -x clean`; verify the new behavior fails before implementation.
- [ ] Implement the API and registries, without event/effect behavior. New registrations cannot silently replace active components.
- [ ] Run the same tests and verify green; commit `feat(skills): add extensible Java contracts and owned registries`.

### Task 2: YAML compilation and immutable definition snapshots

**Files:** Runtime `config/SkillYamlLoader.kt`, `config/SkillArgumentsReader.kt`, `compile/SkillCompilerImpl.kt`, `compile/CompiledSkill.kt`, `compile/SkillSnapshot.kt`, `compile/SkillCompileException.kt`; tests `config/SkillYamlLoaderTests.kt`, `compile/SkillCompilerTests.kt`.

**Consumes:** Task 1 registries/factories. **Produces:** safe YAML loader and compiler yielding an immutable skill map plus trigger-to-skill index; path-qualified errors. Compiler receives the existing `ExpressionEnvironment`, predeclared with the spec's five variables.

- [ ] Write failing tests for duplicate YAML IDs, unknown components, missing required IDs, invalid number/string/list types, unsafe YAML tags, duplicate mapping keys, invalid expressions, absent optional damage=0, recursive all/any/not, finite duration parsing and traversal ignoring `_` sample files.
- [ ] Verify failures with `:arcana-skills:test --tests '*SkillYamlLoaderTests' --tests '*SkillCompilerTests'` and standard flags.
- [ ] Implement safe parsing and full-directory candidate compilation. Disabled definitions remain listed; examples beginning `_` remain ignored. Do not publish a partial snapshot on any error.
- [ ] Verify green and commit `feat(skills): compile validated YAML definitions and expression snapshots`.

### Task 3: Dispatch, cooldowns and cancellable execution

**Files:** Runtime `DefaultSkillEngine.kt`, `execution/SkillRunner.kt`, `execution/ExecutionState.kt`, `execution/SkillExecutionImpl.kt`, `execution/SkillScheduler.kt`, `execution/CooldownStore.kt`, `execution/InvocationSnapshots.kt`; tests `execution/SkillRunnerTests.kt`, `execution/CooldownStoreTests.kt`, `execution/ExecutionLifecycleTests.kt`; shared test fixtures for controlled scheduler, clock, RNG and component factories.

**Consumes:** compiled snapshots. **Produces:** complete cast/trigger result futures, atomic admissions, bounded ordered execution and cancellable scheduling. Time/RNG/scheduler are injected through small constructor seams for deterministic tests; production uses Arcana Scheduler and `System.nanoTime`.

- [ ] Write failing tests for chance=0/100, denied admission consuming no cooldown, concurrent same-key admission allowing exactly one cast, group sharing, clock changes, ordered async effects, no-target handling, repeat/cast budget exhaustion and finite numeric values.
- [ ] Add lifecycle tests: failed reload leaves old jobs running; successful swap cancels old jobs and keeps cooldown; logout/disable cancels and releases tasks; extension removal cancels pending dependent work and prevents subsequent callbacks; callback already executing may finish.
- [ ] Add a scheduler spy test that records caster snapshot reads on caster context, victim reads on victim context, and location effects on location context. No snapshot read occurs before its context runs.
- [ ] Run focused tests, observe failures, implement dispatch/cooldown/cancellation without concrete game effects, then verify the whole skills suite green.
- [ ] Commit `feat(skills): execute bounded skill chains with atomic cooldowns and lifecycle cancellation`.

### Task 4: Built-in effects and conditions, each in a separate class

**Files:** Runtime `effects/` classes `MessageEffect`, `ActionBarEffect`, `SoundEffect`, `ParticleEffect`, `HealEffect`, `DamageEffect`, `FeedEffect`, `IgniteEffect`, `PotionEffect`, `RemovePotionEffect`, `VelocityEffect`, `ConsoleCommandEffect`, `PlayerCommandEffect`, `SequenceEffect`, `DelayEffect`, `RepeatEffect`, `BranchEffect`, `CastEffect`; `conditions/` classes `PermissionCondition`, `WorldCondition`, `HealthBelowCondition`, `HealthAboveCondition`, `FoodBelowCondition`, `FoodAboveCondition`, `SneakingCondition`, `SprintingCondition`, `HasTargetCondition`, `ExpressionCondition`, `AllCondition`, `AnyCondition`, `NotCondition`; `BuiltinComponents.kt` registers instances only; tests `effects/BuiltinEffectTests.kt`, `conditions/BuiltinConditionTests.kt`, `execution/DamageRecursionTests.kt`.

**Consumes:** task 3 scheduling/execution helpers and task 2 typed arguments. **Produces:** spec's concrete YAML IDs with factory-time validation; no central type switch. IDs use lower snake_case corresponding to class names (`particle`, `health_below`, `cast`, etc.).

- [ ] Write tests proving particle/sound/heal/damage/potion/velocity arguments are validated once, health is clamped, dead targets reject state-changing effects, and target omission defaults to caster.
- [ ] Write tests for all/any/not truth tables, empty-composite validation, branch choosing exactly one path, sequence stopping at failure, delay/repeat preserving order and cancellation, and cast applying the target skill's admission while sharing budgets.
- [ ] Write a synchronous generated-damage test proving the runtime marker suppresses recursively generated skill damage triggers and clears in `finally`, including exceptions.
- [ ] Observe focused failures, implement each separate component class, run the full skills suite, then commit `feat(skills): add modular built-in effects conditions and flow controls`.

### Task 5: Separate trigger adapters and snapshot filters

**Files:** Runtime `triggers/` classes `JoinTrigger`, `RespawnTrigger`, `DeathTrigger`, `BlockBreakTrigger`, `BlockPlaceTrigger`, `MeleeAttackTrigger`, `ProjectileHitTrigger`, `DamageTakenTrigger`, `RightClickTrigger`, `LeftClickTrigger`, `SneakTrigger`, `SprintTrigger`; `filters/SkillFilters.kt`; tests `triggers/TriggerAdapterTests.kt`, `filters/SkillFiltersTests.kt`.

**Produces:** separately registered MONITOR listeners with cancelled events ignored; IDs as lower snake_case class names. Manual casts use API and do not require an event listener. World/block/entity/item filters operate on immutable invocation snapshots.

- [ ] Write failing adapter tests: player projectile shooter resolves to caster; cancelled events do not fire; sneak/sprint only start transitions fire; missing victim/block snapshot makes a corresponding filter fail; damage generated by task 4's marker never redispatches.
- [ ] Test event callbacks only enqueue/snapshot permitted data and never retain Event objects in delayed execution; unregister each adapter removes its listener.
- [ ] Implement the adapters independently, register through `BuiltinComponents`, run all skills tests and commit `feat(skills): add event trigger adapters and invocation filters`.

### Task 6: Core lifecycle, commands, docs and distribution

**Files:** Modify root `build.gradle.kts`, `arcana-core/core-plugin/build.gradle.kts`, `Arcana.java`, `ArcanaPlugin.java`, `ArcanaImpl.kt`, and command implementation locations identified from current core-backend command construction. Create runtime `SkillsCommand.kt` and separate command subtypes as needed; runtime `src/main/resources/arcana-skills/examples/_vampiric_strike.yml`, `_welcome.yml`, `_combo.yml`; docs `docs/wiki/skills.md`; update README/wiki index. Tests: API `skills/ReloadPreflightTest.java`, runtime `integration/SkillLifecycleTests.kt`, `integration/SkillsCommandTests.kt`, `config/ExampleSkillsTests.kt`.

**Consumes:** tasks 1–5. **Produces:** one installed Arcana JAR, bootstrapped engine, atomic general reload, admin commands, documented native schema and examples.

- [ ] Write failing lifecycle tests verifying a new boolean `tryReload(boolean cancelTasks)` checks a protected preflight hook before both config updates and scheduler cancellation; retain both existing void `reload` signatures as delegating wrappers. Existing plugins' default preflight returns true. Global reload command reports a rejection accurately.
- [ ] Test startup samples are disabled/ignored, a file-based manual skill casts, `/arcana skills` permission gates work, console requires a player, completion suggests IDs/online players, and reload command returns compiler diagnostics without claiming success.
- [ ] Wire `Arcana.getSkillEngine` to `DefaultSkillEngine`; stage skills reload during core preflight, commit it in reload lifecycle, cancel jobs/close registrations at owner disable/quit, and prevent duplicate listener registration across reloads. Core uses a compileOnly skills dependency; root includes the runtime implementation once.
- [ ] Add three independently editable sample files and Turkish schema/API docs. Explain IDs, conditions, filters, snapshot timing, cooldown/error behavior and adding a standalone `ParticleEffect`-style class.
- [ ] Run focused integration tests, then `gradlew.bat build --no-parallel --max-workers=2 -x clean --console=plain`; require all existing/new tests and seven NMS linkage checks green.
- [ ] Inspect root JAR for API + skills implementation + correct example paths, Arcana plugin metadata and bundled license/NOTICE. Verify no libreforge class/source entered the distribution.
- [ ] Commit `feat(skills): integrate lifecycle commands samples and documentation`; request a whole-branch code review, fix material findings with regressions, rerun affected verification and publish the completed work to the user's fork. State whether live server startup was exercised.

## Plan review record

The user-approved spec is fully mapped to the six tasks. The component-per-file requirement is explicit in tasks 4–5. The five review focus items have concrete assertions assigned above. Interface names are shared here instead of redefined by later tasks; factory internals and small helper class partitioning remain implementer decisions. Existing checkout is preferred; no new worktree is requested.
