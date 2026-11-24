# Arcana Advanced Skills Platform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (the user's preserved native execution choice) to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish Arcana's shared advanced skill platform; do not create separate RPG, Items, Mobs or other addon projects.

**Architecture:** Keep the Java frontend in `arcana-api`, the catalog-independent Kotlin engine in `arcana-skills`, and move built-ins into `arcana-skills-components`. Add a general invocation API beside the unchanged Player API, owned namespaced definition sources and provider contracts. Core composes both modules into the same installed JAR.

**Tech Stack:** Java 21 public contracts, Kotlin 2.3.21, existing Arcana scheduling/expression/data APIs, Bukkit/Paper/Folia, SnakeYAML, JUnit 6 and MockK. No new third-party skill engine dependency.

**Spec:** `docs/superpowers/specs/2026-11-23-advanced-skills-platform-design.md` (approved 2026-11-23; final user correction: Arcana only).

**Coverage:** `docs/superpowers/research/2026-11-23-advanced-skills-coverage.md`; pinned reference identifiers are in its linked CSV. Those references describe behavior, not code to copy. The matrix's planned rows must all be delivered; its excluded/provider-only rows must remain explicit.

## Global Constraints

- Preserve existing schema 1 YAML and the old Java method descriptors, including `SkillContext.builder(Player)`, `getCaster(): Player` and existing component factories.
- New schema is native `schema: 2`; no MythicMobs/Libreforge DSL or package compatibility claim.
- Every mechanic, predicate, trigger, targeter, mutator and interceptor has its own class/file. Registries perform ID lookup; no expanding component type switch.
- No addon projects, product-specific fixtures, mob/boss/AI/spawner/loot managers, RPG progression, class or skill-tree implementation.
- Core includes engine and catalog once in one Arcana JAR. Engine production sources must not depend on catalog or core/NMS implementations.
- All registrations, sources, providers and managed work have owners; unload/register/publication transitions are serialized, and extension callbacks run outside the lifecycle gate.
- Failed global preflight preserves configs, definitions and jobs. Source reload cancels affected dependency graphs only. Queued events bind their definition revisions at emission.
- Live entity/world state is read or changed only on the relevant Arcana scheduler context. No delayed Event references or foreign-region entity iteration.
- Preserve limits: 16 nested casts, 1,024 normal effect steps, 100 repeat iterations, 24-hour maximum delay. New target selection defaults to 64 with hard cap 256; parallel fan-out cap 16; interceptor cap 64 local steps.
- All numbers are finite. Root budgets/dependencies/cancellation apply across branches and nested calls; managed callbacks cannot obtain an unlimited fresh budget.
- Starting managed limits: 64 instances per actor, 4,096 per engine; 4,096 callbacks and 65,536 applied steps per instance; maximum finite instance lifetime 24 hours. These are configurable only below hard caps, measured and documented in Task 21.
- Do not copy external source implementations or descriptions. Mark unfinished coverage honestly; registration-only scaffolds do not complete a feature.

## Review Focus

1. An unchanged old Java extension must retain its descriptors and Player behavior; a mob invocation must never reach a legacy Player-only closure with a null or incorrectly cast caster (Tasks 2,6).
2. Source A may be reloaded/unloaded while source B has a cyclic or dynamic reference to it: stale code must be rejected, dependent jobs cancelled, and unrelated B jobs preserved (Task 4).
3. Shape selection and fast projectile segments can cross Folia regions/chunks while a target retires: every query/application must use ownership boundaries and deterministic missing-target behavior (Tasks 6,7,15).
4. Interceptor plus delayed continuation and concurrent cost/charge reservations must admit and charge once; failing a later resource reservation must not leak prior reservations or consume cooldown (Tasks 10,11).
5. Tick/overwrite/expiry/unload can race for an aura or passive lease: cleanup is once, another owner's modifiers survive, closed owner callbacks do not restart, and managed/parallel budgets cannot reset (Tasks 9,12–16).

## File map and naming rules

The following path aliases expand literally; every task path below uses these roots:

- **A** = `arcana-api/src/main/java/com/keopsfenks/arcana/core/skills/platform/` (new Java contracts).
- **R** = `arcana-skills/src/main/kotlin/com/keopsfenks/arcana/skills/platform/` (new Kotlin runtime).
- **C** = `arcana-skills-components/src/main/kotlin/com/keopsfenks/arcana/skills/components/` (new native catalog).
- **AT** = `arcana-api/src/test/`; **RT** = `arcana-skills/src/test/kotlin/com/keopsfenks/arcana/skills/platform/`; **CT** = `arcana-skills-components/src/test/kotlin/com/keopsfenks/arcana/skills/components/`.

Existing built-in source files move to the new module with their current package names intact (`.../com/keopsfenks/arcana/skills/effects`, `conditions`, `triggers`, and `BuiltinComponents.kt`). New general-actor built-ins use `C` and `*Mechanic`/`*Predicate` names; legacy classes adapt shared actions instead of changing public v1 signatures.

For catalog tasks, each class in the matching coverage row expands to one literal `.kt` file under the task's specified directory; lists are file manifests, not classes to combine. A catalog registration file contains construction/registration only.

Verification commands below use **FLAGS** = `--no-parallel --max-workers=2 -x clean --console=plain`. Replace FLAGS with those exact arguments on execution. Each task's Verify command is run RED after adding its tests, GREEN after implementation, and by `task-done` before its completion ledger line. Keep logs in this plan's own ignored workspace.

## Shared public contract decisions

- `SkillKey(String namespace, String value)` validates namespace `[a-z0-9_]{1,64}` and value `[a-z0-9_]{1,100}`. `parse(String text, String defaultNamespace)` resolves an unqualified name without locale-sensitive case conversion. Keys are identifiers, never filesystem paths.
- `SkillActor.of(LivingEntity)` retains UUID/entity reference only; actor snapshots read live state on the owner context. `player()` returns `Optional<Player>` based on reference type. `SkillLocation` and `SkillVector` are immutable numeric records; world is UUID, not a mutable Bukkit Location.
- `SkillInvocation.builder(SkillActor)` captures immutable actor/optional trigger/origin/selected entity or location/data/parameters; `derive()` copies values for a child frame. `SkillSelection` contains deduplicated entity and location targets.
- `SkillValue` is a closed typed value model (finite number, string, boolean, immutable list/map, key, location, vector). It rejects cycles, depth over 64 and payload over 256 KiB. No Bukkit object is a variable value.
- Existing `SkillEngine` gains only a default `platform(): SkillPlatform` accessor; unsupported external engine implementations throw `UnsupportedOperationException` rather than pretending success. Existing abstract methods/descriptors remain.
- `SkillPlatform.cast(SkillKey, SkillInvocation): CompletionStage<SkillResult>` and `trigger(SkillKey, SkillInvocation): CompletionStage<List<SkillResult>>` are the new entry points. `register(Plugin, SkillComponentType<T>, SkillKey, T): SkillRegistration` is typed; token constants select independent registries for mechanics, predicates, targeters, mutators, triggers and interceptors.
- `SkillMechanic.compile(SkillArguments, SkillCompilation): CompiledSkillMechanic`; compiled `execute(SkillExecutionContext): CompletionStage<SkillResult>`. `SkillPredicateType.compile(...): SkillPredicate`, whose `test(SkillInvocation): boolean` uses snapshots. `SkillCompilation.number(Object,String): SkillNumericExpression` evaluates against invocation bindings.
- `SkillExecutionContext` exposes `invocation()`, `on(SkillActor,Function<LivingEntity,SkillResult>)`, `at(SkillLocation,Function<Location,SkillResult>)`, `execute(List<CompiledSkillMechanic>)`, `delay(long)`, `cast(SkillKey,Map<String,SkillValue>)` and `services(): SkillServices`. `SkillServices.require(Class<T>): T` provides a read-only typed service view for this invocation (including scoped variables and internal managed services). The public API imports no runtime class. Catalog code may request runtime manager types because its permitted dependency points toward the engine. These contracts do not replace old `SkillExecution`.
- Legacy adapters accept only Player invocations and route old factories/conditions/execution helpers through their existing contracts. New catalog components compile natively. A selected schema 2 skill remains callable from the old `cast(String,SkillContext)` entry point through an explicit Player invocation adapter.

---

### Task 1: Catalog separation with working legacy distribution

**Files:** Create `arcana-skills-components/build.gradle.kts`. Modify `settings.gradle.kts`, root `build.gradle.kts`, core-plugin `build.gradle.kts`, `SkillsRuntime.kt` and `ArcanaImpl.kt`. Move existing `BuiltinComponents.kt`, `effects/*.kt`, `conditions/*.kt`, `triggers/*.kt` and their catalog tests to the new module preserving packages. Keep compiler/execution/registry/filter infrastructure and the three ignored legacy example resources in engine. Test: core-plugin `skills/ModuleBoundaryTests.kt`; retain small test-only fixture copies where catalog and engine need independent fixtures.

**Interfaces:** `SkillsRuntime(plugin,engine,directory,installCatalog:(DefaultSkillEngine,ArcanaPlugin)->Unit)` receives its composition callback; core supplies `BuiltinComponents.install` plus `installTriggers`. Engine startup itself imports no catalog class.

- [x] Write `runtimeNeedsNoCatalogClasses()` and `coreInstallsCatalogExactlyOnce()`; assert empty-catalog runtime starts, injected install count is 1 across repeated start, and existing sample resources still compile.
- [x] Run `.\gradlew.bat :arcana-api:test :arcana-skills:test :arcana-skills-components:test :arcana-core:core-plugin:test --tests '*ModuleBoundaryTests' FLAGS`; expect RED for the absent module/injection seam.
- [x] Extract catalog and wire compileOnly dependencies/core composition; root implementation includes new module. Runtime tests may depend on catalog in test scope only. Legacy example resource paths remain unchanged and exist when starting the engine without the optional catalog.
- [x] Run the same Verify command; expect GREEN, then root `build FLAGS`; expect all legacy tests and seven linkage checks GREEN.
- [x] Commit `refactor(skills): separate built-in catalog from execution engine`.

### Task 2: General invocation API and legacy ABI contract

**Files:** A `SkillKey.java`, `SkillActor.java`, `SkillLocation.java`, `SkillVector.java`, `SkillValue.java`, `SkillInvocation.java`, `SkillSelection.java`, `SkillPlatform.java`, `SkillComponentType.java`, `SkillPlatformComponent.java`, `SkillDescriptor.java`, `SkillArgumentDescriptor.java`, `SkillMechanic.java`, `CompiledSkillMechanic.java`, `SkillPredicateType.java`, `SkillPredicate.java`, `SkillCompilation.java`, `SkillNumericExpression.java`, `SkillExecutionContext.java`, `SkillServices.java`. Modify existing `SkillEngine.java`; create R `legacy/LegacyInvocationAdapter.kt`. Tests AT `java/com/keopsfenks/arcana/core/skills/platform/LegacyConsumer.java`, `kotlin/com/keopsfenks/arcana/core/skills/platform/PlatformCompatibilityTests.kt`, `PlatformValueTests.kt` and `resources/skills-v1-descriptors.txt`.

**Interfaces:** Produce the exact public contracts above. Descriptor holds ID/help/argument names and type/default/required/range metadata. Component tokens carry `Class<T>` for checked registration; no growing type switch.

- [x] Test `legacyDescriptorsAndConsumerRemainUnchanged()` against a checked v1 descriptor list and unchanged Java fixture; `mobCannotBecomeALegacyPlayerContext()` rejects adaptation with `UNSUPPORTED_ACTOR`; `invocationValuesCopyMutableInputs()` rejects NaN, cycles, excess depth/payload and mutable Number aliasing.
- [x] Verify `.\gradlew.bat :arcana-api:test :arcana-skills:test FLAGS`; expect RED for missing contracts in the named API tests and a runtime legacy-adapter test under RT `legacy/LegacyInvocationAdapterTests.kt`.
- [x] Implement immutable values/contracts and explicit Player legacy adapter. Add v2-only result statuses `UNSUPPORTED_ACTOR`, `UNSUPPORTED_CONTEXT`, `INSUFFICIENT_RESOURCE`, `BUDGET_EXCEEDED`, `PROVIDER_UNAVAILABLE`; retain v1 result behavior.
- [x] Verify the named API tests and runtime adapter tests GREEN, including a normal Player cast through the existing public API.
- [x] Commit `feat(skills): add general invocation contracts without breaking player API`.

### Task 3: Typed namespaces and shared lifecycle gate

**Files:** R `registry/PlatformRegistry.kt`, `registry/PlatformRegistration.kt`, `lifecycle/SkillLifecycleGate.kt`, `DefaultSkillPlatform.kt`; modify `DefaultSkillEngine.kt` and legacy owner transitions. Tests RT `registry/PlatformRegistryTests.kt`, `lifecycle/ProviderLifecycleTests.kt`.

**Interfaces:** `PlatformRegistry<T>.register(owner:Plugin,key:SkillKey,value:T): SkillRegistration`, `.get(key): RegisteredComponent<T>?`; `SkillLifecycleGate.admit(owner,action)` and `.disable(owner)` collect handles atomically for closing outside the gate. `DefaultSkillPlatform` shares the parent engine's lifecycle gate; it does not implement a second independent owner state.

- [x] Assert two mock owners can register equal local IDs in different namespaces; wrong-owner namespace and duplicate typed key reject; closing one owner preserves the other. Pause insertion with latches and race disable/shutdown; no active late registration survives. Callback tries re-entry without deadlock.
- [x] Verify `.\gradlew.bat :arcana-skills:test --tests '*PlatformRegistryTests' --tests '*ProviderLifecycleTests' FLAGS`; expect RED.
- [x] Implement tokens/registries, normalized owner namespace with `Locale.ROOT`, idempotent registrations and shared gate. Existing legacy registration still rejects duplicate unqualified legacy IDs.
- [x] Same Verify GREEN; also existing registry/execution lifecycle tests GREEN.
- [x] Commit `feat(skills): isolate namespaced registrations with shared owner lifecycle`.

### Task 4: Owned definition sources and dependency-aware reload

**Files:** A `SkillDefinitionSource.java`, `SkillDefinition.java`, `SkillDefinitionInfo.java`; R `sources/DirectoryDefinitionSource.kt`, `sources/DefinitionRepository.kt`, `sources/DefinitionCandidate.kt`, `sources/DefinitionRevision.kt`, `sources/DefinitionGraph.kt`; modify loader, reload coordinator and platform service. Tests RT `sources/DefinitionSourceTests.kt` and `SourceReloadRaceTests.kt`.

**Interfaces:** `SkillPlatform.registerSource(Plugin,SkillDefinitionSource): SkillRegistration`, `reloadSource(SkillKey): SkillReloadResult`, `definitions(): List<SkillDefinitionInfo>`; sources expose key and immutable `load(): List<SkillDefinition>` outside locks. Repository `prepare(Set<SkillKey>)` and `commit(DefinitionCandidate)` publish immutable snapshots/revision leases.

- [x] Tests load directory/in-memory definitions from two fake owners; unqualified IDs inherit source namespace; duplicate key rejects; invalid source/global preflight preserves jobs/configs. Cyclic/static and dynamic references reject invalidated revisions, while unrelated owner work survives source reload. Source load racing unregister cannot publish.
- [x] Verify `.\gradlew.bat :arcana-skills:test --tests '*DefinitionSourceTests' --tests '*SourceReloadRaceTests' FLAGS`; expect RED.
- [x] Implement safe existing YAML limits per source, schema dispatch (absent=1; explicit=2), source ownership/revision graph and global prepare-before-cancel integration. A callback never reads files or runs factories under the lifecycle gate. Full reload retains existing cancel-all-old-work semantics; source reload invalidates only affected graphs.
- [x] Same Verify GREEN plus existing reload/lifecycle tests; test qualified cross-source references and missing provider diagnostics with full source/field paths.
- [x] Commit `feat(skills): load owned definition sources with selective atomic reload`.

### Task 5: Parameters, scoped variables and expression bindings

**Files:** A `SkillVariableScope.java`, `SkillVariables.java`, `SkillParameter.java`; R `values/ParameterBindings.kt`, `values/InvocationFrame.kt`, `values/VariableStore.kt`, `values/PersistentVariableStore.kt`, `compile/PlatformExpressionCompiler.kt`; C `effects/variables/SetVariableMechanic.kt`, `AddVariableMechanic.kt`, `RemoveVariableMechanic.kt`. Tests RT `values/ParameterBindingTests.kt`, `VariableStoreTests.kt`, `BindingIntegrationTests.kt`; CT `VariableMechanicTests.kt`.

**Interfaces:** `SkillVariables.get(scope,key): Optional<SkillValue>`, `set(scope,key,value,ttl): CompletionStage<Void>`, `add(scope,key,delta): CompletionStage<Double>`, `remove(scope,key): CompletionStage<Boolean>`. `ParameterBindings.bind(declarations,supplied)` validates before admission. Numeric bindings use `param_`, `var_`, `stat_` names; base snapshot names remain valid. Persistence port uses existing Arcana player/local data APIs, not a new database implementation.

- [x] Assert required/default/type/unknown parameter behavior, child readonly parameter copies, isolated fork locals, atomic concurrent actor additions, TTL expiry with injected monotonic clock, server separation, opt-in player persistence round trip and offline cleanup. Real expression exception includes field path and position; unbound reference fails compilation.
- [x] Verify `.\gradlew.bat :arcana-api:test :arcana-skills:test :arcana-skills-components:test FLAGS`; expect RED in new named tests.
- [x] Implement typed storage/serialization and parameter-aware expression environments through an injected environment builder; never substitute and reparse text. Player persisted variable keys are namespaced; entity variables remain transient.
- [x] Same Verify GREEN; malicious template values remain literal, and existing formula tests remain GREEN.
- [x] Commit `feat(skills): bind typed parameters and scoped persistent variables`.

### Task 6: General actor execution and safe invocation snapshots

**Files:** A `SkillBounds.java`, `SkillCollision.java`, `SkillTargetQuery.java`; R `execution/PlatformExecutionState.kt`, `PlatformExecutionContext.kt`, `PlatformRunner.kt`, `InvocationSnapshots.kt`, `legacy/LegacyMechanicAdapter.kt`, `legacy/LegacyPredicateAdapter.kt`, `targets/RegionQuery.kt`; modify scheduler and platform dispatch. Tests RT `execution/InvocationSchedulingTests.kt`, `PlatformLimitsTests.kt`, `PlatformCompatibilityTests.kt`.

**Interfaces:** `PlatformRunner.run(definition,invocation,state,depth): CompletableFuture<SkillResult>`; state owns dependencies/revisions/cancellation, 1,024 shared steps and per-root fan-out. `RegionQuery` implements public `SkillTargetQuery.entities(SkillBounds): CompletionStage<List<SkillActor>>` and `.collisions(SkillLocation from,SkillLocation to,double radius): CompletionStage<List<SkillCollision>>`; bounds store world UUID/min/max, collision stores immutable location/normal/optional actor. Partition by owning chunk without loading unloaded chunks. Public target contexts depend on SkillTargetQuery, never runtime RegionQuery.

- [x] Scheduler spies throw on actor/trigger/victim/world access from the wrong owner; cover cross-region selection, player and mob roots, invalidated target, rejected legacy mob closure and old API casting a schema 2 skill. Verify nested/parallel effects share the 1,024 limit and cancellation.
- [x] Verify `.\gradlew.bat :arcana-api:test :arcana-skills:test FLAGS`; expect RED.
- [x] Implement general runner and owner-context reads, capability gates, legacy bridge and definition-revision enrollment. Region queries schedule chunk-local entity/block enumeration and skip unloaded chunks; no global foreign-region fallback. New actor fields are snapshotted before predicates run.
- [x] Same Verify GREEN with all old Player admission/cooldown/status assertions retained.
- [x] Commit `feat(skills): execute general actors through ownership-safe invocation contexts`.

### Task 7: Entity and location targeter catalog

**Files:** A `SkillTargeter.java`, `CompiledSkillTargeter.java`, `SkillTargetingContext.java`; R `targets/TargetSelectionPolicy.kt`, `TargetGeometry.kt`; C `targeters/` with the 12 class files in coverage T-SELF/T-LOC/T-AREA/T-SHAPE. Tests CT `TargeterTests.kt`, `TargeterGeometryTests.kt`.

**Interfaces:** targeter `compile(args,compilation): CompiledSkillTargeter`; compiled `select(context): CompletionStage<SkillSelection>`; public context supplies invocation, SkillTargetQuery and injected `DoubleSupplier` RNG. Policy takes filters, sort, limit and random selection; default64/hard256, duplicate UUID/location removal and stable tie-breaks.

- [x] Test exact radius boundary, cone angle/zero direction, line/box inclusion, nearest ray hit, self/trigger/origin distinction, random reproducibility, 257 rejection and missing required snapshots. Partition-spy asserts no foreign live reads, no chunk loads and retired target removal.
- [x] Verify `.\gradlew.bat :arcana-skills-components:test --tests '*TargeterTests' --tests '*TargeterGeometryTests' FLAGS`; expect RED.
- [x] Implement separate targeter classes and shared numeric geometry; compile all constants/expressions once and apply policy after combining query snapshots.
- [x] Same Verify GREEN; each built-in selector has a compiling schema 2 example fixture.
- [x] Commit `feat(skills): add bounded entity and location targeters`.

### Task 8: Immutable mutators and schema 2 node compilation

**Files:** A `SkillMutator.java`, `CompiledSkillMutator.java`; R `compile/PlatformSkillCompiler.kt`, `CompiledPlatformSkill.kt`, `CompiledNode.kt`, `NodeOptions.kt`; C `mutators/` with the nine files M-ROLE/M-LOC/M-DATA. Tests RT `compile/PlatformCompilerTests.kt`; CT `MutatorTests.kt`.

**Interfaces:** mutator compiled `mutate(SkillInvocation): SkillInvocation`; `PlatformSkillCompiler.compile(SkillDefinition): CompiledPlatformSkill`; node options store targeter/mutators/conditions/chance/delay/repeat/failure policy. Generic compilation exposes typed token lookup; references are recorded by qualified SkillKey.

- [x] Assert a mutator changes only its child's origin/actor/data, not sibling or parent; correct degree rotation and clamping; unknown key/type and invalid expression paths; required argument/default behavior; max64 nesting/1,024 static nodes and unsafe YAML reject. Schema1 fixtures are unchanged.
- [x] Verify `.\gradlew.bat :arcana-skills:test --tests '*PlatformCompilerTests' :arcana-skills-components:test --tests '*MutatorTests' FLAGS`; expect RED.
- [x] Implement schema 2 compiler and per-node pipeline target→copy/mutate→conditions/chance→execute. Default node failure stops sequence; `on-failure: skip` is explicit. Node delay/repeat share root budgets and do not re-admit the root.
- [x] Same Verify GREEN plus existing SkillYamlLoader/SkillCompiler tests.
- [x] Commit `feat(skills): compile schema two nodes with immutable context mutators`.

### Task 9: Parameterized and bounded flow controls

**Files:** C `effects/flow/` with the 11 files FLOW-BASE/FLOW-MULTI/FLOW-PICK; R `execution/ParallelScope.kt`, `FlowCursorStore.kt`. Tests CT `FlowMechanicTests.kt`; RT `execution/PlatformLimitsTests.kt`.

**Interfaces:** child invocation derives parameters/local frame; parallel execution returns ordered child results and shares state. Cursor key is actor+qualified definition+node revision; reload/removal retires stale cursors. `WaitUntil` requires timeout and interval≥1 tick.

- [x] Assert weighted choice and zero/negative weight validation, cycle/shuffle isolation per actor, 17th parallel branch rejection, foreach per-target result aggregation/fail-fast, nested call17 rejection, exactly1,024 applied steps and cancellation of pending siblings. Wait-until timeout and logout release all scheduled work.
- [x] Verify `.\gradlew.bat :arcana-skills:test :arcana-skills-components:test --tests '*FlowMechanicTests' --tests '*PlatformLimitsTests' FLAGS`; expect RED.
- [x] Implement each flow mechanic separately; preserve ordinary sequence ordering and fail-stop semantics. Child calls apply target skill admission while inline nodes share current admission.
- [x] Same Verify GREEN; finite elapsed/tick bounds and no detached callbacks after completion.
- [x] Commit `feat(skills): add parameterized bounded flow and parallel execution`.

### Task 10: Resource/stat providers, charges and atomic admission

**Files:** A `SkillResourceProvider.java`, `SkillResourceReservation.java`, `SkillStatProvider.java`, `SkillAdmission.java`; R `admission/AdmissionCoordinator.kt`, `ChargeStore.kt`, `EveryCounterStore.kt`, `ResourceReservations.kt`, `providers/ProviderRegistry.kt`; C `providers/HealthResourceProvider.kt`, `FoodResourceProvider.kt`, `ExperienceResourceProvider.kt`. Tests RT `admission/AdmissionTests.kt`, `ResourceReservationTests.kt`, `providers/ProviderLifecycleTests.kt`.

**Interfaces:** resource `reserve(actor,amount): CompletionStage<SkillResourceReservation>`; reservation `commit()`, `release()` are idempotent. Optional `reserveLocal` is declared by provider capability for interceptors. Stat `snapshot(actor,Set<SkillKey>): CompletionStage<Map<SkillKey,Double>>`. Admission carries one UUID plus reserved costs/charge/cooldown permit.

- [x] Assert conditions/chance reject with zero resource calls; second resource failure releases first, cooldown denial releases costs, exactly one concurrent shared-group/last-charge winner; interception continuation reuses admission; monotonic refill/wrap, zero-duration shared cooldown and unloaded provider checks. Later effect error keeps committed costs.
- [x] Verify `.\gradlew.bat :arcana-skills:test :arcana-skills-components:test FLAGS`; expect RED.
- [x] Implement reserve-all→cooldown/charge permit→commit admission with compensating release before commitment; no RPG mana/economy product is created. Native resources validate Player capability and prevent a health cost killing its actor by default.
- [x] Same Verify GREEN and real vanilla XP point math round trips; missing provider produces a qualified validation error.
- [x] Commit `feat(skills): coordinate resource reservations charges and shared admission`.

### Task 11: Synchronous event interceptors

**Files:** A `SkillInterceptor.java`, `CompiledSkillInterceptor.java`, `SkillEventAccess.java`; R `events/InterceptorCompiler.kt`, `InterceptorDispatcher.kt`, `AdmittedContinuation.kt`; C `interceptors/AddDamageInterceptor.kt`, `MultiplyDamageInterceptor.kt`, `CancelEventInterceptor.kt`. Tests CT `InterceptorTests.kt`; RT `events/InterceptorAdmissionTests.kt`.

**Interfaces:** compiled interceptor `apply(SkillEventAccess,SkillInvocation,SkillAdmission): SkillResult`; event access is callback-scoped and invalidated before returning from listener. Interception runs at HIGHEST and ignores previously cancelled events; normal observation remains MONITOR. Normal continuation accepts immutable snapshot+admission only. Compiler permits only locally synchronous providers/predicates, no delayed/async/cross-region nodes. Runtime verifies required local entity ownership before any reads/reservations; unsupported ownership returns `UNSUPPORTED_CONTEXT`. Accepted interception costs are not promised to roll back if another plugin later cancels the event; document this event-order limitation.

- [ ] Test event damage changes before callback return, cancellation compatibility/priority, 65th local step rejection, no retained Event after return, and compile rejection of delay/projectile/async cost in interceptor. Same trigger's normal continuation charges once and a competing cast cannot steal its reservation.
- [ ] Verify `.\gradlew.bat :arcana-skills:test :arcana-skills-components:test FLAGS`; expect RED.
- [ ] Implement callback-scoped event façade and dedicated interception registry/listener path. Generated skill damage sets recursion scope at actual application; origin/admission is carried across delayed descendants. Existing MONITOR adapters keep their cancellation policy.
- [ ] Same Verify GREEN including cancellation by another plugin and reload between intercept/continuation (no stale continuation or second cost).
- [ ] Commit `feat(skills): add bounded synchronous event interception`.

### Task 12: Holder providers and contribution leases

**Files:** A `SkillHolderProvider.java`, `SkillHolder.java`, `SkillContribution.java`, `SkillContributionLease.java`; R `holders/HolderManager.kt`, `HolderDiff.kt`, `ContributionStore.kt`, `managed/ManagedWorkRegistry.kt`; C `passives/AttributeContribution.kt`, `PotionContribution.kt`. Tests RT `holders/HolderLifecycleTests.kt`, `StatContributionTests.kt`.

**Interfaces:** holder provider `holders(SkillActor): CompletionStage<List<SkillHolder>>`; holder key/slot/definition keys/parameters are immutable. Contribution store applies/removes one owner-qualified contribution ID and returns an idempotent lease. Managed registry indexes handles by owner, actor and definition revisions with engine/actor instance caps.

- [ ] Assert slot changes apply only differences, duplicate reevaluation is idempotent, owner removal closes leases once, another owner's stat modifier survives, expired potion is not restored over a new external potion, and raced refresh/unload cannot resurrect a lease. No full catalog scan runs every tick.
- [ ] Verify `.\gradlew.bat :arcana-skills:test --tests '*HolderLifecycleTests' --tests '*StatContributionTests' FLAGS`; expect RED.
- [ ] Implement event/dirty-driven reevaluation with bounded 20-tick fallback, owner-qualified modifiers and effect ownership/reconciliation. Do not blindly reset whole attributes or clear all potion effects.
- [ ] Same Verify GREEN with two fake owners, logout, provider disappearance and shutdown.
- [ ] Commit `feat(skills): manage holder-driven passive contribution leases`.

### Task 13: Owned aura lifetime engine

**Files:** A `SkillManagedHandle.java`, `SkillRemovalReason.java`; R `managed/ManagedExecutionState.kt`, `auras/AuraManager.kt`, `AuraInstance.kt`, `AuraStackKey.kt`, `AuraDefinition.kt`, `AuraOptions.kt`. Tests RT `auras/AuraLifecycleTests.kt`, `managed/ManagedBudgetTests.kt`.

**Interfaces:** `AuraManager.apply(AuraDefinition,SkillInvocation,AuraOptions): CompletionStage<SkillManagedHandle>`; definition contains compiled hook/component lists and their dependencies; options contain duration/interval/stack limit/share-caster/refresh/overwrite/charges. Handle exposes instance UUID, revision and `remove(reason)`. Managed adoption outlives normal parent completion but inherits owner/actor/revision cancellation; root completion only releases work not explicitly adopted. All tick children borrow the instance's total budget.

- [ ] Test start once, tick intervals, expiry/end once, stack/refresh/overwrite policies and caster-sharing keys. Concurrent expiry/dispel/unload closes once; invalidated owner/revision never runs user on-end. Parent success leaves adopted aura active, explicit cancel/unload cleans it, and callback4,097 / step65,537 fail closed.
- [ ] Verify `.\gradlew.bat :arcana-skills:test --tests '*AuraLifecycleTests' --tests '*ManagedBudgetTests' FLAGS`; expect RED.
- [ ] Implement finite managed lifecycle on owning actor/target contexts, separate root completion from explicit cancellation, and shared actor/engine caps. Cleanup removes native contributions even when skill callbacks are no longer callable.
- [ ] Same Verify GREEN including target retirement and registration-removal races.
- [ ] Commit `feat(skills): implement owned bounded aura lifecycles`.

### Task 14: Aura mechanics and components

**Files:** C `effects/auras/ApplyAuraMechanic.kt`, `RemoveAuraMechanic.kt`, `AddAuraChargeMechanic.kt`; C `auras/AuraTickComponent.kt`, `AuraAttackComponent.kt`, `AuraDamagedComponent.kt`, `AuraStatComponent.kt`; A `SkillAuraComponent.java`; tests CT `AuraMechanicTests.kt`.

**Interfaces:** aura components register by token and compile with explicit dependencies; timed/event subscriptions return managed leases. Charge zero removes with its reason. Matching supports name/type/tag plus explicit caster policy.

- [ ] Assert inline/named hook compilation, tag-specific removal, charge exhaustion, stack-scaled stat contribution, generated damage recursion prevention and subscriptions removed on every shutdown path. Bad hook/unknown aura rejects candidate before mutation.
- [ ] Verify `.\gradlew.bat :arcana-skills-components:test --tests '*AuraMechanicTests' FLAGS`; expect RED.
- [ ] Implement separate mechanics/components using AuraManager and interception/holder contribution contracts; callbacks do not create unbounded fresh roots.
- [ ] Same Verify GREEN with a working shield and periodic buff example fixture, disabled by default.
- [ ] Commit `feat(skills): expose modular aura mechanics and components`.

### Task 15: Region-safe projectile simulation

**Files:** R `projectiles/ProjectileManager.kt`, `ProjectileInstance.kt`, `ProjectileMotion.kt`, `CollisionResolver.kt`, `ProjectileHitLedger.kt`, `ProjectileOptions.kt`, `ProjectileHooks.kt`; tests RT `projectiles/ProjectileSimulationTests.kt`.

**Interfaces:** `ProjectileManager.launch(SkillInvocation,ProjectileOptions,ProjectileHooks): CompletionStage<SkillManagedHandle>` uses immutable segment snapshots and SkillTargetQuery; motion advances only after required owner-context collision results are available. Options carry finite speed/gravity/lifetime/radius/homing/bounce/pierce/max-hit/immune-delay; hooks contain compiled start/tick/hit/end lists and dependency handles.

- [ ] Test deterministic seeded paths, fast segment crossing a narrow entity/block, chunk/region handoff, unloaded chunk termination, retired target, homing, bounce energy/direction, piercing, per-target immunity and total hits. Query spy rejects global/foreign reads. Tick/hit/end races and callback budgets are bounded.
- [ ] Verify `.\gradlew.bat :arcana-skills:test --tests '*ProjectileSimulationTests' FLAGS`; expect RED.
- [ ] Implement swept segment collision and nearest hit ordering, moving owner-location scheduling, and managed lifetime adoption. Visual interpolation cannot increase damage hits; no implicit chunk loading or unsafe global ticking.
- [ ] Same Verify GREEN, including reload/owner-unload mid-flight and at most one end/cleanup.
- [ ] Commit `feat(skills): simulate bounded projectiles across region ownership boundaries`.

### Task 16: Projectile, beam and chain catalog

**Files:** C `effects/projectiles/ProjectileMechanic.kt`, `MissileMechanic.kt`, `RayMechanic.kt`, `BeamMechanic.kt`, `ChainMechanic.kt`; tests CT `ProjectileMechanicTests.kt`.

**Interfaces:** separate factories compile start/tick/hit/end hooks and inherit origin/hit target/parameters. Beam uses a bounded region query rather than persistent entity ownership; Chain carries visited UUIDs and maximum jumps.

- [ ] Assert hit hooks inherit actual victim/origin, parameter power, hit conditions and no duplicate same-target chain; child recursion/managed budgets remain shared. Unknown hooks and invalid geometry fail with exact node paths. Callbacks after disposal never execute.
- [ ] Verify `.\gradlew.bat :arcana-skills-components:test --tests '*ProjectileMechanicTests' FLAGS`; expect RED.
- [ ] Implement five mechanics through shared manager/targeting services; cap chain jumps at100 and keep overall effect/hit limits. Add independently compiling, disabled bolt/beam/chain fixtures.
- [ ] Same Verify GREEN alongside projectile simulation and aura recursion tests.
- [ ] Commit `feat(skills): add native projectile ray beam and chain mechanics`.

### Task 17: General-actor combat, status and movement catalog

**Files:** C `effects/combat/` (six COMBAT classes), `effects/status/` (four STATUS classes), `effects/movement/` (six MOVE classes), per coverage manifest; shared small actions under C `actions/`. Tests CT `CombatMechanicTests.kt`, `MovementMechanicTests.kt`.

**Interfaces:** mechanics consume SkillExecutionContext and snapshots; operations requiring a Player declare capability and return `UNSUPPORTED_ACTOR`. Timed stun/freeze use managed contribution leases. Teleport uses the server's supported asynchronous ownership-safe path, not an off-owner synchronous teleport.

- [ ] Test health/absorption/dead-target clamps, no-target results, actual damage guard, potion ownership, impulse normalization/zero-distance cases, dash cancellation, safe teleport completion/failure and stun teardown retaining unrelated movement contributions. No state read before target context.
- [ ] Verify `.\gradlew.bat :arcana-skills-components:test --tests '*CombatMechanicTests' --tests '*MovementMechanicTests' FLAGS`; expect RED.
- [ ] Implement each file separately, sharing actions with legacy effects where behavior is identical; do not change normal armor/invulnerability behavior without an explicit supported argument.
- [ ] Same Verify GREEN and legacy effect/damage-recursion tests GREEN.
- [ ] Commit `feat(skills): expand native combat status and movement mechanics`.

### Task 18: Presentation, item and world catalog

**Files:** C `effects/visual/` (five VISUAL classes), `effects/presentation/` (four TEXT and two COMMAND classes), `effects/items/` (four ITEM classes), `effects/world/` (four WORLD classes), per coverage manifest. Tests CT `VisualMechanicTests.kt`, `PresentationMechanicTests.kt`, `ItemMechanicTests.kt`, `WorldMechanicTests.kt`.

**Interfaces:** use existing Arcana particle/item/text APIs; geometry is immutable, each world/block mutation is scheduled at location, each inventory mutation on its entity. Timed bossbars return managed leases. Commands retain explicit player/console semantics and literal context formatting.

- [ ] Test particle resolver once, finite geometry/count cap, safe literal text placeholders, title/bossbar removal, item amount/durability/enchant bounds and missing material errors. Block/world operations respect cancellable native events and loaded chunk bounds; no bypass of protection by editing from a global callback.
- [ ] Verify `.\gradlew.bat :arcana-skills-components:test --tests '*VisualMechanicTests' --tests '*PresentationMechanicTests' --tests '*ItemMechanicTests' --tests '*WorldMechanicTests' FLAGS`; expect RED.
- [ ] Implement manifest files and location-safe mutations. Player block edits emit cancellable BlockPlace/BlockBreak events; non-player edits emit EntityChangeBlockEvent and obey cancellation. Default explosion fire/block damage are false; explicit true follows native explosion events. Document these hooks without promising compatibility with every external protection plugin. No custom item product, loot manager or separate plugin is introduced.
- [ ] Same Verify GREEN with multiple targets on different contexts and cleanup after owner shutdown.
- [ ] Commit `feat(skills): expand presentation item and world skill mechanics`.

### Task 19: Snapshot condition catalog

**Files:** C `conditions/` with the 23 distinct class files in COND-COMMON/ACTOR/WORLD/TARGET/DATA rows; tests CT `PlatformConditionTests.kt`.

**Interfaces:** factories implement SkillPredicateType and use only named required snapshots/bindings. Metadata declares snapshot needs so Task 6 captures them on their owner. Actor/target/trigger selection is explicit; missing required data fails the condition, never invents true.

- [ ] Truth tables and nested validation; exact distance/height/health/time bounds; Bukkit enum localeROOT; permission/item capability on mobs; line-of-sight sampled on correct owner; variable/resource/stat/aura values and missing provider behavior. Parent/target/trigger conditions remain distinct.
- [ ] Verify `.\gradlew.bat :arcana-skills-components:test --tests '*PlatformConditionTests' FLAGS`; expect RED.
- [ ] Implement individual predicates and snapshots, compile numeric/enum/list arguments once. All/any reject empty lists, not takes one predicate; conditions contain no live Bukkit reads.
- [ ] Same Verify GREEN and schema 2 nested condition examples compile.
- [ ] Commit `feat(skills): expand typed actor target world and data predicates`.

### Task 20: General trigger adapters and event-time binding

**Files:** A `SkillPlatformTrigger.java`; R `events/PlatformTriggerDispatch.kt`, `events/TriggerSubscriptions.kt`; C `triggers/` with the 11 new classes in TRIGGER-MOVE/ITEM/FIGHT plus bridges for existing12; tests CT `PlatformTriggerTests.kt`.

**Interfaces:** trigger `listen(Consumer<SkillInvocation>): AutoCloseable`; prepare captures definition revisions at event emission. Custom triggers expose a namespaced API dispatch, not a generic arbitrary Event retained for later. Timer subscriptions are managed jobs with interval≥1 tick and bounded recipients.

- [ ] Test cancelled events, main hand, attack-vs-thorns, projectile shooter, move/jump transitions, slot diff, consume/shoot/kill and entity actor events; no duplicate emissions. Empty index queues nothing; reload between emission and handoff runs neither replacement nor disposed definition. Owner close removes listeners/timers and pending callbacks.
- [ ] Verify `.\gradlew.bat :arcana-skills-components:test --tests '*PlatformTriggerTests' FLAGS`; expect RED.
- [ ] Implement separate adapters and snapshot capability metadata. New triggers use the same causality guard, ownership boundaries and definition binding as legacy adapters.
- [ ] Same Verify GREEN with existing trigger tests and no event reads after callback return.
- [ ] Commit `feat(skills): expand owned general skill trigger adapters`.

### Task 21: Diagnostics, documentation, coverage completion and release validation

**Files:** R `diagnostics/SkillTraceRecorder.kt`, `SkillInspector.kt`, `SkillSchemaWriter.kt`; extend `arcana-skills/.../commands/` with separate validate/inspect/trace subcommands. C `StandardSkillCatalog.kt`, `StandardSkillExamples.kt` and descriptor lists. Update `docs/wiki/skills.md`, add `docs/wiki/skills-api.md`, `docs/wiki/skills-components.md`, schema2 disabled examples and `arcana-skills-components/src/main/resources/arcana-skills/schema-v2.json`. Core invokes the catalog's separate example copier without overwriting files; the catalog registration list itself does no file I/O. Tests RT `diagnostics/SkillDiagnosticsTests.kt`, CT `CatalogCoverageTests.kt`; core-plugin `skills/PlatformDistributionTests.kt` and API `LegacyConsumer` fixture.

**Interfaces:** descriptors generate supported argument schema/help from the same metadata factories validate. Trace is opt-in, finite buffer256 events per trace and engine-wide1,024 traces with TTL60s; it records IDs/snapshot summaries, not live Event or unbounded plugin objects. Validate/inspect are read-only and use existing management permission.

- [ ] Test exact qualified errors/source paths, every registered built-in descriptor, schema1/2 samples, permission gates, bounded tracing/expiration, management commands and catalog/matrix consistency. Two generic mock owners prove public source/component/provider use; no named addon project or product example is created.
- [ ] Verify focused named tests RED; implement commands/schema/docs/catalog metadata; compare every planned coverage row with real behavior tests. Explicitly retain provider-only/excluded distinctions and list any remaining reference dispositions instead of claiming unimplemented parity.
- [ ] Run `.\gradlew.bat build FLAGS`; require all existing/new tests and seven NMS linkage checks GREEN. Inspect root JAR for API+engine+catalog, schema/examples, metadata/license/NOTICE and absence of copied third-party namespaces. Validate owned package paths/docs links and unchanged v1 descriptors. Run deterministic load tests at limits64/256 targets,16 branches,64/4096 instances and65,536 managed steps; record measured memory/task cleanup, adjust defaults only below hard caps.
- [ ] Record exact test counts, artifact checksum and whether live Paper/Folia scenarios were run; commit `feat(skills): document inspect and package the advanced skill platform`.
- [ ] Generate one whole-branch review package and request the required strongest-model fresh-context independent review. Root implements material findings in one RED→GREEN fix pass and reruns the full suite; no implementer delegation or per-task reviewer. Publish the completed Arcana branch to the user's fork with a reviewable PR; do not merge or create any external addon/repository.

## Plan self-review and handoff

Every approved feature family maps to Tasks 1–21 and coverage IDs; all five review focus items have explicit regressions. Public v1 signatures remain; new actor contracts are in `platform`. Sources/resources/holders are generic extension interfaces and Arcana tests, not implementations of future products. Delivery chunks are dependency stages of one platform: foundation1–4, execution5–11, managed12–16, catalog/finish17–21. Each stage remains independently testable, and the whole platform is complete only after all tasks and matrix rows pass.

Preserved execution method: root implements inline in this chat, with one independent whole-branch review at the end. Existing checkout on `codex/advanced-skills-platform` is reused; no new worktree requested. Ask for review of this concrete plan once, then execute continuously using the preserved method; do not re-ask for a method or pause between tasks.
