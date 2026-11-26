# Advanced skill platform validation — 2026-11-26

The approved Arcana-only 21-task implementation and one independent whole-branch
review/fix pass are complete. Final verification finished on 2026-11-27 (Europe/Istanbul).
No separate addon project was created. Publication is recorded after verification below.

## Reproducible validation

```powershell
.\gradlew.bat build --no-parallel --max-workers=2 -x clean --console=plain
python tools/verify-skills-distribution.py
```

Final full build: **GREEN**, 5 minutes 20 seconds, followed by a **GREEN** 39-second
whole build after strengthening the supplementary real-query flight regression.
Unchanged tasks were cached; changed engine tests and isolated native fixtures reran.

- 1653 tests across 201 suites; 0 failures/errors/skips.
- Seven NMS linkage checks pass (common plus six version adapters).
- 96 original v1 JVM descriptors and unchanged Java consumer retained.
- 125 native registered entries:57 mechanics,23 predicates,12 targeters,9 mutators,
  3 interceptors,2 contributions,4 aura components,3 resources,1 stat provider,
  11 general triggers;12 original Player adapters also reach native definitions.
- Nine inactive bundled samples. Three schema1 samples compile with the existing
  real-expression tests; all six schema2 samples really compile after removing
  their startup-only leading underscore in temporary copies. Exact loaded count6
  prevents a vacuous ignored-files assertion. User-modified originals survive copying.
- All six schema2 YAML samples independently validate with AJV CLI5.0.0 against
  the generated JSON Schema2020-12. No new production dependency added.
- 1592 owned Java/Kotlin package/type/source paths and owned Markdown links checked.
- Root JAR includes API, engine, catalog, schema, examples, metadata and LICENSE/NOTICE;
  no copied third-party namespaces or test fixtures. bStats35538/resource-id0 retained.

Artifact: `build/libs/arcana-2026.47-all.jar` (16057692 bytes).

SHA256: `5a148d81ffef5f604df2b7f1a34a1a831248c0c5a17118284891545ed532fa0d`.

## Limits and cleanup evidence

```text
load: managed=4096 actors=64 cleanups=4096 retainedRegistry=0 heapDeltaBytes=10485760 (non-GC estimate)
load: simultaneousBranches=16 overflowRejected=true permitsReleased=true
load: targetDefault=64 targetMaximum=256 candidates=303 deterministic=true
```

These are controlled fixture allocation estimates without a forced GC, not a
production throughput or retained-heap guarantee. Registry entries and all4096
cleanup leases return to zero/exactly-once; cancelled native timer payloads drop
their user action before the original deadline. Additional regressions exercise
4096 callbacks/65536 steps with overflow rejection and shared descendant budgets.
Configuration can only lower hard caps, with startup validation.

Live Paper/Folia server startup/gameplay was **not run**. Ownership spies,
controlled schedulers, API consumers, real expression compilation, isolated
Paper registry fixtures and embedded NMS display-record tests supply the evidence.

## Independent review

One fresh-context read-only reviewer, `gpt-6.2-astra/high`, reviewed
`d4141b014db5935dee66aaa5a0a7e81d82d8d2f5..66fda57c9dcae26c85a9786048e5f39f7dba86a2`
against the approved plan/spec, all five review-focus cases and the initial 26
rulings. Verdict: one Critical, eleven Important and one Minor. The author
verified the material claims, fixed eleven real defects in one RED→GREEN pass,
and demonstrated that the cooldown claim was already handled by the reviewed
HEAD. No re-review or per-task reviewer was used.

| Finding | Verification and resulting behavior |
| --- | --- |
| Native write/cleanup race (Critical) | Four actual-write unload regressions RED→GREEN: holder attribute/potion and timed stun/potion. Late native leases compensate exactly once. |
| Old payload conversion | Manual and registered schema-1 nested-null payload regressions RED→GREEN. Conversion happens only for real native/aura recipients; conversion failure cannot suppress schema 1. |
| Queued projectile handoffs | One-step flight with genuine next-tick owner/global/location handoffs RED→GREEN. Completed simulation steps determine duration; a separate finite stall watchdog retains the original lineage deadline. Stalled-query late hits are discarded. |
| Holder authorization lifetime | Provider-unload continuation and unequip/equal-reequip running-root regressions RED→GREEN. Exact holder/provider dependencies retire held work while unrelated roots finish. |
| Interception revision selection | Earlier interceptor reload/replacement regression RED→GREEN. Dispatch uses the captured exact revision and snapshot. |
| Damage input roles | Two local/foreign counterpart/cause regressions RED→GREEN. Local interception reads owned roles; normal continuation captures full roles on their owners and refreshes final damage without another admission. |
| Unsupported interceptor triggers | `move` plus `cancel_event` baseline RED→GREEN, exact `triggers[0]` diagnostic. The separate baseline run supplied RED after the earlier combined run had stopped before catalog tests. Built-in synchronous adapters advertise capability. |
| Mixed-schema cooldown claim | Regression GREEN on unchanged store: both cast orders, schema migration and a distinct external namespace. Existing `removePrefix("arcana:")` already unifies keys. No unnecessary source fix. |
| Unloaded sweep partition | Successful preflight followed by missing middle partition RED→GREEN. Strict collision collection aborts; a strengthened real-flight case asserts middle pass 2/destination preflight pass 1 and no movement. |
| Neighboring overlapping hitbox | Reference discovery RED→GREEN, plus actual swept-hit assertion. Bounded native spatial queries replace center-assigned chunk enumeration; live snapshots remain on entity owners. |
| Repeated bounce distance | Two halfway bounces at speed 8 RED→GREEN. Recursive segments carry the remaining original-step fraction. |
| Managed branch counters | Sixteen pending parent branches plus child adoption RED→GREEN. Descendants share the same counter and exact permit-release tracker. |

The supplementary actual-flight test initially counted only x despite its radius
covering two z cells. Its assertion was strengthened before final release to
prove retirement during collision collection rather than merely during preflight.
The original required-partition regression had already proved the post-preflight
race. No production change was needed for this test correction.

All material fixes are covered by the final 1653-test complete build. Live server
behavior remains untested. Unattributed local `.gitignore`/`.vscode` changes were
preserved and excluded from the implementation commits.

## Rulings I made

Chronological ledger decisions, including their cost if wrong. Decision 31
supersedes the projectile-grace policy in decision 24; the earlier decision is
retained here to preserve the full record.

1. source preflight must compile native definitions before Task 8 node compiler exists — bring forward the minimal CompiledPlatformSkill contract and basic source factory dispatcher; Task 8 still adds target/mutator/node-option compilation — cost if wrong: an internal compiler refactor, with no v1 API/schema change.
2. bound one ownership-safe query to 256 chunks, 4096 candidates and 65536 block samples — chunk partitioning also needs finite work limits before projectile/targeter callers exist — cost if wrong: dense or very large queries fail with a budget result and may need later tuning below documented hard caps.
3. cap transient cycle/shuffle cursor state at1024 entries per actor and65536 per engine — per-revision cleanup alone does not bound actively invoked definitions — cost if wrong: unusually broad active flow catalogs receive a budget failure and need measured cap tuning.
4. prepare asynchronous/nonlocal resources first, then hold local/native resources and finalize all permits within one actor-owner callback; reservation commit/release are idempotent nonblocking finalizers — prevents health/food/XP holds crossing actor retirement while a remote preparation waits — cost if wrong: custom adapters must honor the documented prepare/finalize contract.
5. add explicit activation global(default)/holder to schema2 and passives-only native definitions — holder-provided skills must not accidentally trigger for everyone, while existing global behavior stays compatible — cost if wrong: holder-only users must declare the activation mode explicitly.
6. cap tracked holder actors at4096, per-actor holders64, bound definitions256 and passive instances64, sharing managed64/4096 caps — pending provider results and mass holder expansion need finite limits before native lease application — cost if wrong: unusually broad equipment/provider sets reject a refresh and need cap tuning below hard limits.
7. expire an unfinished holder refresh after100 actor ticks and retire its old leases — a hung provider cannot leave an unverified equipment passive active indefinitely — cost if wrong: providers taking longer than about five normal-rate seconds must refresh again.
8. preserve an existing external potion until its natural removal instead of overriding it and later restoring a snapshot — Bukkit potion effects lack a per-provider modifier identity and removing an owned stronger chain could erase external hidden effects — cost if wrong: an Arcana potion can wait behind a weaker external potion; documented conservative coexistence.
9. an explicitly shared aura retains its first caster and source admission until overwritten — sharing merges refresh/stack keys without transferring another source`s owned work — cost if wrong: later casters refresh the existing instance, but its original caster still controls cancellation.
10. normal expiry/dispel/overwrite/charge removal lets previously adopted descendants finish, while cancellation/unload/invalidation/shutdown/budget failure cancels them — distinguish ordinary managed completion from explicit cancellation consistently with normal root adoption — cost if wrong: a child effect already launched before dispel can persist until its own deadline within the shared lineage budget.
11. cap the accumulated related actor identities of a managed lineage at256 — descendant adoption inherits ancestor cancellation without retaining an unbounded actor-reference set — cost if wrong: exceptionally long chains changing actors are rejected even if fewer instances remain active.
12. bound projectile speed and gravity-adjusted velocity to64 blocks per tick, bounce count100, total hits256 and piercing255; require the complete radius-expanded query bounds to be loaded — finite geometry and conservative loaded-region snapshots preserve existing query budgets — cost if wrong: very fast or wide projectiles terminate/reject sooner and may require measured tuning within the hard query caps.
13. use a generic finite ManagedPulse for anchored beam callbacks instead of representing beams as stackable auras — both share ManagedExecutionState budgets, but beams must not enter aura name/tag matching — cost if wrong: an internal timer helper refactor; native timed leases can reuse its cancellation/cleanup contract.
14. stun suppresses native walking/jumping through unique transient modifiers; freeze refreshes finite vanilla freeze ticks without owning a global freeze lock or restoring old state — native freeze ticks have no per-provider identity — cost if wrong: native freeze visuals can linger while vanilla decays after the requested duration; stun is movement control rather than a blanket action/skill lock.
15. remove_potion defaults to dispelling timed Arcana claims, preserves holder claims, and requires external:true for native external removal — a holder remains active across an equal refresh and must not silently lose its claimed passive — cost if wrong: users wanting to suppress a holder passive must change its holder definition instead of using timed dispel.
16. give_item accepts at most64 native stacks within its4096-item bound and reports inventory overflow without rolling back previously inserted stacks — per-owner native inventory insertion is bounded while preserving server insertion rules — cost if wrong: large unstackable grants reject, and callers must handle partial insertion when inventory fills.
17. titles expire through finite native client timings rather than a delayed clearTitle; bossbars retain exact-object managed cleanup — native titles lack an owner-qualified identity, so a later clear could erase another plugin`s title — cost if wrong: a title may remain until its configured native timing after its source unloads.
18. suppress Arcana trigger roots during its generated block protection events while still dispatching those native events to other plugins — otherwise a block-triggered edit can restart with a fresh root budget — cost if wrong: Arcana skills cannot chain through their own native block events; explicit child casts remain available.
19. bound a compiled definition to256 distinct extra snapshot requests and reject asynchronous world/LOS/provider requirements in synchronous interception — metadata needs finite owner-context work and local callbacks cannot await another region — cost if wrong: unusually broad condition definitions reject preflight and need splitting or measured cap tuning.
20. timers require explicit owner/actor API subscriptions rather than scanning all online entities from YAML — this is a shared extension platform and its recipient set must be owned and bounded — cost if wrong: timer YAML alone does not auto-enroll players; callers must enroll/close their recipients.
21. move ignores rotations/teleports, equip emits one changed-slot map, entity_attack/entity_damaged exclude players covered by existing player keys — stable event semantics prevent duplicate roots and skill teleport feedback — cost if wrong: consumers needing other transitions must use explicit custom events or another adapter.
22. reserve at most4096 generated native damage identities for60seconds, retaining a1second entity-retirement grace — lightning damage occurs after the creating callback and needs a bounded cross-region identity guard — cost if wrong: an unusually persistent native damage entity beyond60seconds is no longer suppressed; source-cap exhaustion rejects new damaging lightning.
23. expose positive configurable managed instance/callback/step/lifetime budgets only at or below hard caps, applied before startup jobs — retain measured bounds and prevent descendant budget reset — cost if wrong: changing these settings requires restart, and smaller budgets terminate unusually large skills sooner.
24. projectile duration counts movement ticks; stalled asynchronous work gets at most one extra expiry tick within the original managed lifetime — lifetime1 otherwise expired before any collision step — cost if wrong: stalled flight cleanup can occur one tick after its configured simulation duration.
25. generated schema/help validates supported keys and shared basic shapes; factory compilation remains authoritative for expression ranges, references and conditional/named-hook forms — these forms cannot be faithfully reduced to a scalar JSON schema without rejecting valid configurations — cost if wrong: editor/schema validation alone does not catch every semantic error; operators must run skills validate.
26. variable predicates may read bounded data.field/param.name map paths as well as declared var_ bindings — trigger snapshots need typed equality without text evaluation — cost if wrong: callers relying on unbounded/deeper data paths must reshape their immutable input.
27. full foreign catalog/YAML parity stays outside the delivery — the approved native inventory and explicit805 reference dispositions bound this framework — cost if wrong: foreign configurations need native rewrites or new extensions.
28. no separate addon, mob/boss/AI/spawner/loot/progression product is added — the user explicitly kept this delivery to the shared Arcana framework — cost if wrong: those future product features require later work.
29. do not certify standalone Spigot gameplay from Paper/Folia unit/ownership evidence — the approved live-runtime validation target and the actual tests cannot establish another server implementation — cost if wrong: Spigot needs its own compatibility validation before deployment.
30. retain existing cooldown canonicalization instead of changing it for finding8 — the store already unifies dash/arcana:dash and the mixed-schema/migration regression passes unchanged while alpha:dash stays separate — cost if wrong: an untested custom cooldown-key form could still need a separate regression.
31. supersede Task21's one-tick projectile grace with a100-actor-tick pending-step/initialization stall timeout and simulation-step expiry, always within the original managed lineage deadline — ordinary Folia global/region/entity handoffs can require several ticks — cost if wrong: a stalled flight may retain its bounded lease for about five normal-rate seconds; a healthy very slow step taking longer is conservatively retired.

## Deferred minors

- both lifecycle listeners repeat idempotent PlayerQuit cleanup scans; consolidate in a later polish change.
