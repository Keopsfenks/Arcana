# Arcana Skills API

Arcana ships one server plugin. `arcana-api` contains Java extension contracts,
`arcana-skills` owns compilation/execution/lifetimes, and
`arcana-skills-components` contains separate registered factories. Start with the
[YAML guide](skills.md), [component catalog](skills-components.md), and
[coverage decisions](../superpowers/research/2026-11-23-advanced-skills-coverage.md).

## Loading and registration

Depend on `Arcana` in your plugin descriptor. Compile against the locally built
`arcana-api/build/libs/arcana-api-2026.47.jar`; the server provides these classes.
Do not shade another API or engine into the extension.

```java
SkillPlatform platform = Arcana.get().getSkillEngine().platform();
SkillRegistration registration = platform.register(this, SkillComponentType.MECHANIC,
    new SkillKey("myplugin", "greet"), new SkillMechanic(
        SkillDescriptor.of("greet", "Send a fixed greeting")) {
    @Override public CompiledSkillMechanic compile(SkillArguments args, SkillCompilation compiler) {
        String text = args.string("text"); // compile once; no retained YAML mutation
        return context -> context.on(context.invocation().actor(), entity -> {
            if (!(entity instanceof Player player))
                return SkillResult.of(SkillResult.Status.UNSUPPORTED_ACTOR, "Greeting needs a player");
            player.sendMessage(text);
            return SkillResult.success();
        });
    }
});
```

The namespace must match the registering plugin's normalized name (`MyPlugin`
becomes `myplugin`). Retain/close registrations when replacing a component.
Arcana also closes them on owner disable. Register independent
`SkillMechanic`, `SkillPredicateType`, `SkillTargeter`, `SkillMutator`,
`SkillInterceptor`, `SkillContribution`, `SkillAuraComponent` and
`SkillPlatformTrigger` subclasses. Each compiled factory uses its captured
registration; removing it invalidates dependent definitions and owned work.
Descriptors provide immutable option/help metadata. Native catalog factories use
the same metadata for allowed keys and basic shapes, schema generation and help;
semantic rules, numeric expressions and conditional requirements are checked by
compilation and bounded execution.

## Actors and scheduling

`SkillActor.of(livingEntity)` captures identity, not live health, equipment or
location. `SkillInvocation` is immutable and distinguishes actor, target,
trigger, origin, current location, selection, typed parameters and data. Use
`context.on(actor, action)` for entity reads/mutations and the provided query
services for spatial work. Never retain Bukkit events, scan `World.entities`,
load chunks or read a foreign entity's live state from a callback.

`SkillCompilation.snapshot(SkillSnapshotRequest, path)` declares additional
permission/equipment/biome/world/line-of-sight/resource/stat/aura reads. The host
captures them on their owner and returns a reserved immutable binding alias.
Predicates read snapshots only. Local interception rejects requirements that
need another region or asynchronous provider. A missing required subject/value
fails a predicate. The original Player-only API and schema 1 remain available;
using a legacy component with a mob returns `UNSUPPORTED_ACTOR`.

## Owned sources and providers

Implement `SkillDefinitionSource.key()` and `load()` returning immutable
`SkillDefinition` documents with a source path. `load()` must be read-only.
Register with `registerSource(owner, source)` and selectively publish using
`reloadSource(source.key())`. Preflight compiles references and validates the
complete affected graph before publication. Invalid reload preserves published
code and pending work. Successful reload retires affected revisions, including
unchanged-source dependents; unrelated sources keep running. Events queued
before reload do not select replacement definitions.

`SkillResourceProvider` owns balance/reservation capability; reserve bounded
holds, then prepare/finalize or refund exactly once. Finalization must be local
and nonblocking; do remote I/O before the commit barrier. Native health costs
leave 1 HP, food preserves saturation during refund, and experience uses vanilla
points. Costs/cooldown/charges/every are admitted once even when a local
interceptor resumes later. Failure after committed admission keeps the cost.

`SkillStatProvider` supplies bounded immutable requested keys. Missing keys do
not invent values. `SkillHolderProvider` supplies actor-bound holders; refresh
with `refreshHolders(actor)` when external ownership/equipment changes. Global
skills use `activation: global` (default); holder-bound definitions use
`activation: holder`. Holder reconciliation diffs identities and parameters,
preserves unchanged leases and closes removed owned contributions. Attribute
contributions remove exact modifiers; potion contributions conservatively
preserve external effects. Economy, mana, custom item and other plugin systems
remain provider contracts, not embedded products.

## Events, timers and managed work

Native triggers expose `listen(Consumer<SkillInvocation>)`; close the returned
listener and emit immutable data during the native callback. The host captures
definition revisions before an owned one-tick handoff. Existing player adapters
also reach native definitions exactly once. Custom namespaced events can call
`platform.trigger(key, invocation)` directly or use a registered
`CustomSkillTrigger` adapter when the catalog dependency is intentional.

`subscribeTimer(owner, triggerKey, actor, intervalTicks)` explicitly enrolls one
recipient (1..1728000 ticks). Close the returned `SkillManagedHandle`; owner,
trigger and actor retirement also close it. Declaring a timer in YAML does not
scan/enroll online entities. Timers count against shared managed instance caps.

Aura/projectile services adopt creating invocation lineage: source revision,
parameters, selected actor relationships, original nesting depth, first deadline
and aggregate budgets. Nested managed work cannot reset the budget. Normal
creating-skill completion lets adopted work finish; explicit cancellation,
owner disable, source retirement and engine stop remove it. Normal expiry,
dispel and replacement remove their own native state immediately and permit
already adopted descendants to complete under that same lineage. Cleanup
leases must be idempotent and owner-local. Never open fresh roots from a
mechanic to bypass child-cast or managed budgets.

## Diagnostics and validation

`/arcana skills validate` compiles without publication; `inspect <id>` shows
source/revision/dependencies; `trace <id> on|off|show` controls finite identity
traces. Management permission is `arcana.command.skills`. Trace enrollment and
records expire after 60 seconds, with at most 1024 traces and 256 events each.
Trace summaries contain IDs, counts and status, never Bukkit events or live
plugin/entity objects. Cancellation clears scheduled user closures even when a
native server timer remains queued until its original deadline.

Tests use two generic owners to exercise public source/component/provider
contracts. Paper/Folia ownership is checked using controlled schedulers and
isolated native metadata fixtures; no live server startup or gameplay test has
been performed for this delivery.
