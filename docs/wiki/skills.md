# Arcana Skills

Arcana Skills, YAML ile beceri tanımlamak için Arcana'nın kendi efekt motorudur. Yürütme motoru `arcana-skills`, standart bileşenler `arcana-skills-components` modülünde geliştirilir. İkisi normal Arcana JAR'ına dahildir; ikinci bir eklenti kurman gerekmez. XP, seviye ve yetenek ağacı bu motorun parçası değildir.

## Dosya ve sınıf düzeni

```text
arcana-api/.../core/skills/
  SkillEffect.java          # Ortak efekt sözleşmesi
  SkillCondition.java       # Ortak koşul sözleşmesi
  SkillTrigger.java         # Ortak tetikleyici sözleşmesi
  SkillEngine.java          # Dış eklentilerin kullandığı API
arcana-skills/.../skills/
  compile/                  # YAML'den çalışma tanımı üretir
  execution/                # Sıra, zamanlama, iptal ve cooldown
arcana-skills-components/.../skills/
  effects/ParticleEffect.kt
  effects/HealEffect.kt
  effects/DamageEffect.kt
  conditions/HealthBelowCondition.kt
  conditions/PermissionCondition.kt
  triggers/MeleeAttackTrigger.kt
  triggers/BlockBreakTrigger.kt
  BuiltinComponents.kt      # Yalnızca kayıt listeleri
```

Her efekt, koşul ve tetikleyici ayrı sınıftır. YAML'deki `type: particle`, kayıt sistemi üzerinden [ParticleEffect](../../arcana-skills-components/src/main/kotlin/com/keopsfenks/arcana/skills/effects/ParticleEffect.kt) sınıfına gider. Yeni tür eklemek için merkezi bir `switch/when` büyütülmez. Ortak Java ana sınıfları [SkillEffect](../../arcana-api/src/main/java/com/keopsfenks/arcana/core/skills/SkillEffect.java), [SkillCondition](../../arcana-api/src/main/java/com/keopsfenks/arcana/core/skills/SkillCondition.java) ve [SkillTrigger](../../arcana-api/src/main/java/com/keopsfenks/arcana/core/skills/SkillTrigger.java) dosyalarıdır.

## İlk becerini etkinleştir

Sunucu açılınca `plugins/Arcana/skills/` içine `_welcome.yml`, `_vampiric_strike.yml` ve `_combo.yml` örnekleri kopyalanır. Var olan dosyaların üzerine yazılmaz. `_` ile başlayan dosyalar okunmaz; örneklerde ayrıca `enabled: false` bulunur.

1. `_welcome.yml` dosyasını `welcome.yml` olarak kopyala.
2. İçindeki `enabled: false` değerini `enabled: true` yap.
3. `/arcana skills reload` çalıştır.
4. Bir oyuncu yeniden katıldığında karşılama mesajını görür.

Dosya başına bir beceri yazılır. Alt klasörler ve `.yml`/`.yaml` uzantıları desteklenir. Kimlik dosyanın içindeki `id` alanıdır; dosya adı değişse de kimlik değişmez.

```yaml
id: mining_spark
enabled: true
triggers: [block_break]
permission: arcana.skill.mining_spark
chance: 30
cooldown: 2s
cooldown-group: mining
filters:
  blocks: [DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE]
  items: [DIAMOND_PICKAXE, NETHERITE_PICKAXE]
effects:
  - type: particle
    particle: FLAME
    amount: 8
  - type: sound
    sound: minecraft:entity.experience_orb.pickup
    volume: 0.6
    pitch: 1.5
```

`permission` yazılırsa oyuncuya bu izin verilmelidir. Yazılmazsa beceri bütün uygun oyuncular için adaydır. Yönetim komutu izni oyuncuya beceri iznini otomatik vermez.

## Temel alanlar

| Alan | Davranış |
| --- | --- |
| `id` | Zorunlu, `[a-z0-9_]{1,100}`; tüm dosyalarda benzersiz |
| `enabled` | Varsayılan `true`; kapalı tanım listede kalır |
| `triggers` | Liste; herhangi biri tetikleyebilir. Boş/eksik liste yalnızca API/komut çağrısı içindir |
| `permission` | İsteğe bağlı oyuncu izni |
| `chance` | Sabit yüzde, 0–100; varsayılan 100 |
| `cooldown` | Varsayılan `0s`; oyuncu ve grup için ortak bekleme süresi |
| `cooldown-group` | Varsayılan beceri kimliği; aynı gruptaki beceriler cooldown paylaşır |
| `filters` | Tetikleyicinin sağladığı verilere göre eleme |
| `conditions` | Koşul listesi; bütün koşullar sağlanmalıdır |
| `effects` | En az bir efekt; sırayla yürütülür |

Süre örnekleri: `250ms`, `1.5s`, `2m`, `10t`. Bir tick `50ms` olarak çevrilir; gecikmeler tam tick'e yukarı yuvarlanır. Birimsiz sayılar saniyedir. En uzun tek süre 24 saattir. YAML'nin `yes`, `no`, `on`, `off` gibi sözcükleri boolean sayabileceğini unutma; bunları metin olarak kullanırken tırnakla.

## Tetikleyiciler ve hedef

| Kimlik | Olay ve `victim` |
| --- | --- |
| `join` | Oyuncu katılır; victim yok |
| `respawn` | Yeniden doğar; victim yok |
| `death` | Ölür; victim yok, bildirim efektleri kullanılabilir |
| `block_break` | Blok kırar; `block` kopyası sağlanır |
| `block_place` | Blok yerleştirir; `block` kopyası sağlanır |
| `melee_attack` | Doğrudan/süpürme saldırısı; victim vurulan canlıdır. Thorns dahil değildir |
| `projectile_hit` | Oyuncunun mermisi isabet eder; victim isabet alan canlıdır. Blok isabetinde `block` sağlanır |
| `damage_taken` | Oyuncu hasar alır; victim varsa saldırgan/mermiyi atan canlıdır |
| `right_click` | Ana el sağ tıklaması |
| `left_click` | Ana el sol tıklaması |
| `sneak` | Eğilmeye başlama |
| `sprint` | Koşmaya başlama |

İptal edilmiş olaylar çalışmaz; bu, sunucunun iptal edilmiş olarak bildirdiği etkileşimleri de kapsar. İkinci el tıklamaları çift çalışmayı önlemek için işlenmez. Olay verisi kopyalanıp bir sonraki tick'te oyuncunun bağlamına aktarılır; gecikmeli zincirler Bukkit Event nesnesini tutmaz.

Efekt hedefi varsayılan `caster`, yani beceriyi kullanan oyuncudur. `target: victim` olayın sağladığı karşı canlıyı seçer. Hedef yoksa `NO_TARGET` sonucu alınır; otomatik olarak caster seçilmez. Ölülere iyileştirme/hasar gibi durum efektleri başarıyla uygulanmış sayılmaz.

`/arcana skills cast <id> [player]` içindeki oyuncu **caster** olur; komut ayrıca victim oluşturmaz. Victim isteyen beceriyi bir oyun olayıyla veya API'den victim vererek çağır.

## Filtreler

`worlds`, `blocks`, `entities`, `items` listeleri desteklenir. Alanlar arasında AND, bir alanın değerleri arasında OR vardır. Dünya adları tam eşleşir; materyal ve varlık türleri büyük/küçük harfe duyarsız yazılabilir. Boş liste kısıtlama getirmez. Dolu filtreye gereken veri olayda yoksa eşleşmez.

```yaml
filters:
  worlds: [world, world_nether]
  entities: [ZOMBIE, SKELETON]
  items: [DIAMOND_SWORD]
```

Dünya ve ana el eşyası, beceri çalışmaya başlarken oyuncunun zamanlayıcısında kopyalanır. Blok materyali olaydan kopyalanır. Hedefe ait canlı durum başka Folia bölgesinden okunmaz.

## Koşullar

| Kimlik | Alanlar |
| --- | --- |
| `permission` | `permission` |
| `world` | `name` |
| `health_below`, `health_above` | `ratio`, 0–1; sınırın kesin altı/üstü |
| `food_below`, `food_above` | `amount`, 0–20 |
| `sneaking`, `sprinting` | `expected`, varsayılan true |
| `has_target` | `expected`, varsayılan true |
| `expression` | `expression`; sıfır false, diğer sonlu sayılar true |
| `all`, `any` | Boş olmayan `conditions` listesi |
| `not` | Tek `condition` eşlemesi |

```yaml
conditions:
  - type: all
    conditions:
      - type: health_below
        ratio: 0.5
      - type: not
        condition:
          type: sprinting
```

## Sayısal ifadeler ve metin

Sayısal efekt alanlarında sayı veya tırnaklı matematik ifadesi kullanabilirsin. İfadeler yüklemede bir kez derlenir. Değişkenler `damage`, `caster_health`, `caster_max_health`, `caster_food`, `caster_level` olarak sıralanır. `caster_level`, Minecraft'ın normal XP seviyesidir; ayrı bir Arcana seviye sistemi değildir. `damage`, ilgili hasar olayının final damage değeridir; bu değeri sağlamayan olaylarda 0'dır (`projectile_hit` dahil).

```yaml
amount: "2 + damage * 0.1"
```

Sayısal değerler ve hareket bayrakları çağrı başındaki kopyadır; gecikme sonrasında yeniden okunmaz. Canlı hedefe uygulanacak sağlık, konum ve benzeri işlemler ise o hedefin bağlamında yapılır. İzin koşulu oyuncunun kendi bağlamında iznini kontrol eder.

Mesaj/action bar, Arcana renk ve MiniMessage biçimini destekler. `{player}`, `{target}`, `{skill}` değerleri düz metin bileşenleri olarak eklenir; içlerindeki MiniMessage etiketleri veya başka yer tutucular tekrar yorumlanmaz. Komut efektlerinde aynı değişkenler düz komut metnine eklenir.

## Efektler

| Kimlik | Alanlar |
| --- | --- |
| `message`, `action_bar` | `text`, `target`; hedef oyuncu olmalı |
| `particle` | `particle`, `amount` (1–10.000, varsayılan 10), `target` |
| `sound` | Kaynak kimliği olarak `sound`, `volume` (0–100, varsayılan 1), `pitch` (0.5–2, varsayılan 1), `target` |
| `heal`, `damage` | `amount` (0–1.000.000), `target` |
| `feed` | `amount` (-20–20, tam sayı), `saturation` (-20–20, varsayılan 0), `target`; oyuncuya uygulanır |
| `ignite` | `duration`, `target`; sıfır süre ateşi temizler |
| `potion` | `potion` (ör. `speed`), `duration`, `amplifier` (0–255, varsayılan 0), `ambient`, `particles`, `icon`, `target` |
| `remove_potion` | `potion`, `target` |
| `velocity` | `x`, `y`, `z` (-100–100, varsayılan 0), `add` (varsayılan false), `target` |
| `console_command` | `command`; konsolun bağlamında bir komut çalıştırır |
| `player_command` | `command`, `target`; oyuncunun mevcut izinleriyle çalışır |
| `sequence` | `effects` |
| `delay` | `duration`, isteğe bağlı `effects` |
| `repeat` | `times` (1–100), `interval` (varsayılan `0t`), `effects` |
| `branch` | `condition`, `then`, isteğe bağlı `else` |
| `cast` | `skill`; aynı bağlamla başka beceriyi çağırır |

Parçacıklarda mevcut Arcana lookup sistemi kullanılır: `FLAME` gibi veri gerektirmeyen adlar veya `dust:ff00ff` gibi kayıtlı fabrika biçimleri. Veri isteyen düz bir parçacık adı yüklemede reddedilir. Sayısal ifadeler değerlendirilirken de sonlu sayı ve alan sınırları kontrol edilir.

Hasar efekti mevcut Bukkit hasar olayını değiştirmez; yeni, normal sunucu hasarı uygular. Zırh, dokunulmazlık ve diğer eklentiler sonucu etkileyebilir. Başka Folia bölgesindeki caster'ın durumuna erişmemek için hasar çağrısına bir saldırgan nesnesi eklenmez. Beceri kaynaklı bu hasar motoru tekrar tetiklemez.

```yaml
effects:
  - type: branch
    condition:
      type: has_target
    then:
      - type: damage
        target: victim
        amount: 2
    else:
      - type: message
        text: "<gray>Bir hedef gerekiyor.</gray>"
  - type: repeat
    times: 3
    interval: 5t
    effects:
      - type: heal
        amount: 1
```

Bir efekt başarısız olursa zincirin kalan adımları çalışmaz. Daha önce uygulanmış oyun etkileri geri alınmaz. Başlayan çağrının cooldown'u korunur. İç içe `cast`, hedef becerinin bütün izin/koşul/şans/cooldown kontrollerini de uygular.

Otomatik tetiklemelerde çalışma hataları beceri kimliğiyle konsola yazılır; aynı beceri için bildirim beş saniyede birle sınırlıdır. Kendi cooldown'u `0s` olan bir beceri de grubunda hâlen etkin olan ortak bekleme süresine uyar.

## Reload ve sınırlar

`/arcana skills reload` bütün dosyaları yeni bir görüntüye derler. Hatalı dosya varsa mevcut tanımlar ve işler korunur; hata dosya ve alan yoluyla bildirilir. Başarılı reload eski gecikmeli zincirleri iptal eder, cooldown'ları korur. `/arcana reload` da skills doğrulamasını config değişimi ve görev iptalinden önce yapar. İlk açılışta geçerli beceri yoksa hatalı YAML, Arcana'nın diğer servislerinin başlamasını engellemez; skills boş kalır.

Oyuncu ayrılınca onun işleri/cooldown'ları temizlenir. Bir dış eklenti kapanınca ona ait kayıtlar kaldırılır; bağımlı işler iptal edilir ve bu beceriler yeni çağrı kabul etmez. Zaten başlamış callback tamamlanabilir; sonraki adımlar durur.

YAML içindeki `cast` bağlantılarının bağımlılıkları üst beceriye de taşınır. Böylece gecikmeli bir alt becerinin bileşeni kaldırılırsa üst zincir de hemen iptal edilir. Tetikleyici olayı, oluştuğu andaki tanımlara bağlıdır; reload öncesinde kuyruğa giren bir olay yeni tanımları çalıştırmaz. Efekt sırasında kaybolan victim `NO_TARGET` sonucu verir; oyuncunun çıkışı veya reload nedeniyle bütün zincirin iptali `CANCELLED` olarak kalır.

Tek yürütme en fazla 16 iç içe beceri çağrısı ve 1.024 efekt adımı kullanır. Bir tekrar en fazla 100 kez çalışır. Yapılandırma ağacı en fazla 64 seviyedir; döngülü YAML/API verisi reddedilir. Dizin en fazla 1.000 dosya, dosya başına en fazla 1 MiB kabul eder. NaN/sonsuz sayılar geçersizdir.

## Komutlar

| Komut | İşlev/izin |
| --- | --- |
| `/arcana` | Yardım; `arcana.command` varsayılan herkese açık |
| `/arcana reload` | Genel reload; `arcana.command.reload` varsayılan op |
| `/arcana skills list` | Kimlik, kapalı ve bağımlılık durumu; `arcana.command.skills` varsayılan op |
| `/arcana skills cast <id> [player]` | Elle çağrı; aynı yönetim izni; konsol oyuncu belirtir |
| `/arcana skills reload` | Yalnızca beceri dosyaları; aynı yönetim izni |

## Java ile kendi efektini ekle

Eklentinin `plugin.yml` dosyasında `depend: [Arcana]` bulunmalı. `onEnable` içinde kayıt yap; yalnızca `arcana-api` derleme bağımlılığı yeterlidir.

```java
public final class JumpEffect extends SkillEffect {
    public JumpEffect() { super("jump"); }

    @Override
    public CompiledSkillEffect compile(SkillArguments args, SkillCompiler compiler) {
        SkillNumber power = compiler.number(args.raw("power", 0.6), args.getPath() + ".power");
        return execution -> execution.onTarget(SkillTarget.CASTER, entity -> {
            double value = power.evaluate(execution.getContext());
            if (entity.isDead()) return SkillResult.of(SkillResult.Status.NO_TARGET, "Target is dead");
            if (!Double.isFinite(value) || value < 0 || value > 3) {
                return SkillResult.of(SkillResult.Status.ERROR, "Jump power must be between 0 and 3");
            }
            entity.setVelocity(entity.getVelocity().clone().setY(value));
            return SkillResult.success();
        });
    }
}

// Kendi eklentinin onEnable metodu:
SkillRegistration handle = Arcana.get().getSkillEngine().registerEffect(this, new JumpEffect());
// onDisable içinde handle.close(); ayrıca Arcana sahibi kapanınca kayıtları otomatik temizler.
```

```yaml
effects:
  - type: jump
    power: 0.8
```

Koşullarda `SkillCondition`, tetikleyicilerde `SkillTrigger` ana sınıfı kullanılır. `SkillTrigger.listen` dinleyicini başlatır ve onu kaldıran `AutoCloseable` döndürür. Kayıtlı kimlikler sessizce ezilemez. Derleme fabrikasında argümanları doğrula; çalışma sırasında dünyaya/varlığa erişmek için `SkillExecution.onTarget`, `atTarget` ve `global` yardımcılarını kullan.

Özel bir olaydan çağrı:

```java
SkillContext context = SkillContext.builder(caster)
        .victim(target)
        .value("damage", 4.0)
        .build();
Arcana.get().getSkillEngine().cast("my_skill", context);
// Kayıtlı özel tetikleyici için: engine.trigger("my_trigger", context).
```

Bağlam haritaları değiştirilemez ve Bukkit Event nesnesi gibi keyfi nesneleri kabul etmez. Entity referansları yalnızca kendi zamanlayıcı bağlamlarında kullanılmalıdır. Gelecek tamamlanması başka bir bağlamda olabilir; sonrasında başka oyuncuya işlem yaparken yine uygun scheduler kullan.


### Schema 2 event interception

`interceptors` is a separate synchronous phase registered at Bukkit `HIGHEST` with already-cancelled events ignored. Each entry uses an interceptor factory, for example `{type: add_damage, amount: 2}`, `{type: multiply_damage, factor: 1.5}` or `{type: cancel_event}`. The maximum is 64 local steps. Normal `effects` run from the immutable `MONITOR` snapshot and reuse the exact committed admission once; they do not reserve costs, charges or cooldown again. A separate explicit cast still makes its own admission attempt.

Interception permits only local snapshot predicates, local resource/stat providers and nonpersistent variable bindings. Delay, targeting, mutators, asynchronous providers and ordinary effect/projectile nodes are rejected in the interceptor list. All required entities must already be owned by the callback's region; otherwise interception returns `UNSUPPORTED_CONTEXT` without querying providers. An event facade is invalidated before returning and cannot be used from another thread or retained for later work.

Admission commits before event mutation. Another plugin can cancel the event later, including at the same priority; committed costs are retained and the normal continuation is discarded when cancellation is observed at `MONITOR`. Event changes already applied before an interceptor fails are not rolled back. Native skill-generated damage is guarded at the actual damage application so delayed descendants cannot recursively intercept themselves. No compatibility with arbitrary third-party event ordering is promised.


### Holder activation and passive contributions

Schema 2 defaults to `activation: global`. Use `activation: holder` for skills that should respond to triggers only while a registered holder provider includes their key. Explicit API casts remain available. A `SkillHolder` contains a provider-qualified key, a slot, distinct native skill keys and immutable typed parameters. Holder parameters replace the definition's supplied parameters and are validated against its declarations. Identical skill/parameter trigger bindings are deduplicated. Interception follows the same holder activation rule.

`passives` contains separately registered contribution factories, for example `{type: attribute, attribute: max_health, amount: 4, stat: bonus_health}` and `{type: potion, potion: speed, amplifier: 0}`. A definition may contain only passives. These contributions activate through holders, without spending a normal skill admission. Conditions and permission are checked when the holder contribution is initialized. A slot, parameter or definition revision change removes the previous leases and applies the new set; an equal reevaluation preserves the leases. The optional attribute `stat` exports that contribution amount, summed by the built-in `contributions` stat provider.

The generic holder API is `platform.register(owner, SkillComponentType.HOLDER, key, provider)` and `platform.refreshHolders(actor)`. Providers capture live equipment on its owner scheduler. Call refresh when the provider's state changes; Arcana coalesces in-flight reads and reevaluates explicitly tracked actors every 20 ticks. It does not scan the complete skill catalog or every server entity each tick. A refresh that takes more than 100 ticks retires that actor's holder leases and ignores late results. Logout, provider disappearance, definition retirement and shutdown also retire leases.

Limits are 64 holders, 256 bound native definitions and 64 passive instances per actor, 4,096 tracked actors and 4,096 shared managed instances per engine. Owner and revision checks apply before publication and to late completions. Exceeding a limit rejects the refresh rather than partially keeping its new contributions.

Attribute contributions use Paper transient modifiers so they are not saved with entity NBT. Cleanup removes only the unique modifier it added and never resets the base value. Potion contributions use finite 40-tick effects refreshed every 20 ticks; Arcana reconciles its own strongest contribution when one owner is removed. Existing external potion effects take priority until they expire or are removed. Arcana does not save and restore an old potion snapshot over a newer effect. External potion changes are observed at `MONITOR`; native cancellation is respected.


### Managed aura lifetimes

Compiled aura definitions hold immutable start/tick/end and activation-component lists, source revision and dependencies. Runtime mechanics obtain the invocation-bound `AuraManager` from execution services and call `apply(definition, invocation, options)`. The returned `SkillManagedHandle` exposes the instance UUID, aura key, source revision UUID, recipient, active state and first terminal removal reason. Its `remove(reason)` is idempotent.

The default stack key contains the aura key, source revision, recipient and caster. `shareCaster` explicitly merges casters. A shared instance retains the first caster, parameters and source admission until overwritten; later applications refresh duration/charges and add stacks up to the configured limit. Overwrite waits for the previous end hook before starting the replacement. Reaching zero charges removes the instance. Timed hooks are sequential: the next interval starts after the preceding hook completes, while expiry runs independently.

Ordinary skill completion leaves explicitly adopted auras active. Explicit cancellation, a cancelled outcome, cancellation of a still-running parallel branch, actor retirement, owner removal, definition reload and shutdown retire the affected work. Already-completed scopes are distinguished from cancelled scopes. Native contributions and event subscriptions are released immediately on removal, before an optional asynchronous end hook. An unavailable source never reenters its old end hook.

An adopted lineage shares 4,096 hook callbacks and 65,536 applied steps, including its branches, nested casts and explicitly adopted descendants. Creating another managed instance cannot reset those counters or the first lineage's 24-hour deadline. Normal skills keep their existing 1,024-step limit. Actor/caster/recipient references are indexed for cancellation; a lineage may retain at most 256 related actor identities. Instance caps remain 64 per actor and 4,096 per engine.

Duration and interval are positive and at most 1,728,000 ticks. Refresh never moves the hard deadline. At the deadline even a delayed end hook is stopped. Normal expiry, dispel, overwrite and charge exhaustion complete the creating managed scope normally, so previously adopted descendants can finish within their shared remaining budget and deadline. Explicit cancellation, unload, invalidation, shutdown and budget failure cancel descendants. All invocation payloads are immutable snapshots and detached local-variable copies, with no retained event facade.

### Modular aura mechanics and components

Schema 2 uses `apply_aura`, `remove_aura` and `add_aura_charge` as independent mechanic factories. An application declares `name`, positive `duration`, optional `aura-type`, `tags`, `stack-limit`, `charges`, `refresh`, `overwrite` and `share-caster`. Its `period` controls the aura's own `on-tick` hook; common node `interval` retains its repeat meaning. Hooks accept inline effects or a named skill with optional typed parameters. Invalid references and unknown components reject the candidate before publication.

Definition-local `templates.auras.<name>` can hold a mechanic configuration whose `type` is `apply_aura`; an application references it with `aura: <name>`. Templates use the generic compilation API, are preflighted even when unused and cannot recurse indefinitely. Common node targeting and repeat options belong to the application site. Removal matches `name`, `aura-type` or `tag`; caster policy defaults to `own`, with explicit `any` available. Inside an aura hook, `current: true` selects that exact instance, including an aura gifted by another caster.

The `components` list uses the separate `AURA_COMPONENT` registry. Built-ins are `tick` (`interval`, `effects`), `attack` and `damaged` (`effects`, optional local `interceptors` and `conditions`), and `stat` (`stat`, `amount`, `per-stack`). Stats reconcile when stacks or charges change, preserve their contribution identity and discard stale asynchronous snapshots. Expressions expose `aura_stacks` and `aura_charges`. Timers, event subscriptions and stat leases count against the shared managed instance limits and borrow the existing lineage budget.

Event components execute as the aura recipient, retaining original caster parameters and admission. Ordinary hooks receive immutable event roles/selection and run after the synchronous event callback. Local interception uses only recipient-owned snapshots and nonpersistent, synchronous bindings. `consume-charge` applies once after successful local interception; normal hooks explicitly use `add_aura_charge` with `current: true`. Cancelled normal damage observations and skill-generated damage are ignored. API callers can publish custom kinds through `SkillPlatform.trigger`.

Extension factories subclass public `SkillAuraComponent` and obtain `SkillAuraContext` from execution services. This API owns cleanup, timed/event subscriptions, hook execution and charge changes without importing engine classes. `SkillCleanup` schedules host-owned entity cleanup after an invocation has ended; retain only the exact modifier or lease identity your component created. Cleanup is idempotent and must not restore an old entity snapshot.

The packaged `arcana-skills/examples/v2/aura-shield.yml` and `aura-periodic-buff.yml` demonstrate a two-charge synchronous shield and a finite stat buff with periodic variable updates. Both have `enabled: false`; enable them deliberately after reviewing their settings.

### Projectile simulation service

Runtime mechanics obtain the invocation-bound `ProjectileManager` and call `launch(invocation, options, hooks)`. The immutable invocation supplies the current location, original origin, caster, optional target and typed parameters. `ProjectileHooks` contains copied start/tick/hit/end lists, a hit predicate and dependency handles. The returned managed handle survives ordinary parent completion and shares the same cancellation, owner, revision, callback and step budgets as other managed work.

Simulation advances once per tick in blocks per tick. Each complete swept segment checks its loaded chunk bounds, collects block and entity contacts through `SkillTargetQuery`, sorts by nearest contact and commits movement on the destination location's scheduler. Query completion precedes movement and hit callbacks. Entity snapshots are captured on their own entity schedulers; no world entity scan, implicit chunk loading or native projectile entity is used. A block or narrow entity between endpoints is still detected. An unloaded path ends the flight; a retired homing target retires it.

Options include normalized direction, speed, gravity, finite lifetime, radius, homing interpolation, deterministic seeded angular spread, bounce count/energy, piercing, total hits and per-target immunity ticks. Defaults ignore the caster. Hit predicates and hooks receive fresh snapshots of the actual contact target and preserve origin/parameters. Rejected or retired entity contacts do not spend a hit or piercing allowance. Blocks still stop or reflect physical motion. Remaining movement after a bounce is queried again, and particle interpolation cannot add damage hits.

Hard geometry limits are speed 64 blocks per tick (also the gravity-adjusted velocity cap), radius 16 blocks, 100 bounces, 255 pierced contacts and 256 total hits. Existing query limits of 256 chunks, 4,096 candidates and 65,536 sampled blocks apply. Bounds include the radius and must all be loaded, so a wide projectile near an unloaded neighbor terminates conservatively. Lifetime is at most 1,728,000 ticks, with inherited managed budgets able to terminate it sooner. Normal impact/lifetime completion uses `EXPIRED`; explicit cancellation, target retirement, owner removal, reload and shutdown retain their distinct reasons. End hooks and cleanup run at most once; invalidated definitions cannot run an end hook.

### Projectile, ray, beam and chain YAML

Each mechanic has its own factory class and registry entry. `projectile` accepts `speed`, optional `direction: {x: 1, y: 0, z: 0}`, `duration`, `gravity`, `radius`, `homing`, `bounces`, `bounce-energy`, `pierce`, `max-hits`, `immune-delay`, `hit-caster`, `spread` and integer `seed`. Missing direction uses captured yaw/pitch. `missile` accepts the same fields and requires an actual target; its homing default is 1 instead of 0. Numeric durations are seconds, explicit tick strings use `t`.

All five mechanics accept `on-start`, `on-tick`, `on-hit`, `on-end` and `hit-conditions`. Hooks can be inline effects or named skills with optional typed parameters. Contact hooks keep the original origin/parameters, use the actual entity as target (empty for a block) and put the contact position in `location`. Skill-generated descendants inherit the original call depth and shared budgets. An unavailable entity is skipped rather than spending a hit. A rejected block predicate still leaves the physical wall in place.

`ray` runs once using `length` (at most 256), `radius`, `direction`, `pierce`, `max-hits` and `hit-caster`. `beam` anchors the same bounded segment at the captured start, runs immediately and then every `period` until `duration`. Its total hit ledger persists across pulses; optional `immune-delay` prevents repeated target hits. Both wait for ownership-safe query results. Beam uses a finite managed pulse, without creating a Bukkit entity or entering aura stacking/matching. Source retirement cancels pending pulses and cannot invoke the old end hook.

`chain` accepts `radius` (at most 64), `jumps` (1–100) and `hit-caster`. It starts from the supplied target or the nearest eligible actor, then searches from each accepted contact. UUIDs are visited once, including rejected candidates, with stable distance/UUID ordering. Each hop uses owned snapshots and the original invocation budget. No global entity enumeration is used.

Disabled examples are packaged as `projectile-bolt.yml`, `projectile-beam.yml` and `projectile-chain.yml` alongside the aura examples. Each compiles independently when enabled and uses the existing variable mechanics to record contacts.

### Combat, status and movement mechanics

These native mechanics accept `target: selected` by default: use the invocation target when present, otherwise its actor. Explicit `actor`/`caster`, `target` and `trigger` select those roles; a missing explicit role returns `NO_TARGET`. Live properties and mutations run on the selected entity's scheduler. `feed` requires a player and returns `UNSUPPORTED_ACTOR` for a mob.

`damage` uses normal server damage with its generated-damage guard at the actual application. `heal` clamps to native maximum health. Both use finite nonnegative `amount`. `feed` adds integer food and optional saturation within native food bounds. `ignite` extends native fire ticks for `duration`; `extinguish` clears them. `absorption` adds `amount` up to configured `maximum` (default 40), with native server constraints still applying.

`potion` declares `potion`, `amplifier`, positive `duration` and optional `ambient`, `particles`, `icon`. Its owned contribution uses the same conservative coexistence rules as passive potions. `remove_potion` dispels timed Arcana contributions and closes their managed leases immediately, preserving holder contributions and external effects. Explicit `external: true` also requests native removal of the current effect and obeys the native event result.

`stun` suppresses walking and supported jumping with uniquely named transient attribute modifiers for `duration`. Removal deletes only those modifiers, preserving other contributions and base values. `freeze` refreshes finite native freeze ticks for its duration, without changing another plugin's freeze lock or lowering an existing freeze value. On removal it stops refreshing; vanilla freeze decay determines the remaining visual duration. These effects use finite managed leases and indexed recipient cancellation.

`velocity` accepts `x`, `y`, `z` and optional `normalize: true` with `speed`. `knockback` and `pull` use `strength` relative to the captured caster position; zero separation produces a finite zero impulse. `launch` replaces vertical velocity using `strength`. Vectors are capped at length 16. `dash` applies a captured `direction` and `speed` immediately and each `period` until `duration`. Cleanup clears its last impulse only if current velocity still matches, so a later changed impulse survives.

`teleport` requires `destination: {x: 20, y: 70, z: 4}`, optionally with world UUID, yaw and pitch. It resolves the destination and calls `teleportAsync` on the actor scheduler, awaiting its boolean result before continuing. False returns `NO_TARGET`; cancellation prevents subsequent skill effects. A teleport already submitted to the server follows the server's asynchronous teleport lifecycle. See the [Paper entity API](https://jd.papermc.io/paper/1.21.8/org/bukkit/entity/Entity.html) for teleport and freeze tick behavior.

### Visual, presentation, item and world mechanics

`particle`, `particle_line` and `particle_ring` resolve an Arcana `particle` lookup once during compilation. Plain vanilla particle names requiring data are rejected; use an Arcana lookup such as `dust:ff00ff`. `amount` is per point. A line uses `points` (2–256), `length` (at most 256) and optional direction. A horizontal ring uses `points` (3–256), `radius` (at most 64) and degree `rotation`. Each geometry is capped at 10,000 particles total and consumes the existing step budget. Locations are immutable and scheduled separately. `sound` uses a resource key plus bounded `volume`/`pitch`; `stop_sound` stops that key for a selected player. Visual/world mechanics use the invocation location unless `target` explicitly selects an entity role.

`message` and `action_bar` take `text`. `title` takes `title`, optional `subtitle`, `fade-in`, `stay` and `fade-out`; its total native client timing is finite and at most 24 hours. No delayed title clear can erase a later external title. `boss_bar` takes `text`, positive `duration`, `progress` (0–1), `color` and `overlay`; its unique bar is hidden on expiry, cancellation, reload, recipient retirement or owner shutdown. These presentation actions require players.

Templates support `{actor}`, `{player}`, `{target}`, `{skill}`, `{param.name}` and `{data.name}`. Rich template formatting is parsed first; replacements are literal components and are never reparsed as formatting. Static text is limited to 8,192 characters and expanded text to 32,768. `console_command` dispatches on the global scheduler and `player_command` on the selected player scheduler. Commands use a single literal replacement pass and reject blank or multiline expansion.

`give_item` and `remove_item` use Arcana's existing `item` lookup with integer `amount` (1–4,096). Giving clones the lookup prototype into at most 64 native stacks; inventory overflow returns `NO_TARGET` and retains items already inserted. Removal preflights matching storage slots and changes nothing when the full amount is unavailable. `durability` adds signed integer damage to the main-hand item, clamped to its native maximum. `enchant` changes a resolved `enchantment` at bounded `level` (0 removes it); native level and item compatibility apply unless `unsafe: true`. Inventory actions require players and run on their entity scheduler.

`set_block` takes resolved `block` data and optional `physics` (default false). Player placement stages the proposed data without physics so the native placement event exposes the proposed block and replaced state. Cancellation or source invalidation restores the previous state only while the staged data remains unchanged. Player placement/break emit [BlockPlaceEvent](https://jd.papermc.io/paper/1.21.8/org/bukkit/event/block/BlockPlaceEvent.html)/BlockBreakEvent; nonplayer edits emit [EntityChangeBlockEvent](https://jd.papermc.io/paper/1.21.8/org/bukkit/event/entity/EntityChangeBlockEvent.html) and obey cancellation. `break_block` defaults to no drops and no physics; explicit `drops` follows the native break path. All edits check loaded chunks and world height on the location scheduler. These event hooks allow protection listeners to intervene; they do not reproduce every third-party protection system or vanilla interaction rule.

`lightning` defaults to `effect-only: true`. `explosion` accepts `power` (0–8), `fire` and `break-blocks`, with both destructive flags false by default. Its affected bounds must be loaded, and cancellation from the native explosion call is respected. See the [Paper world API](https://jd.papermc.io/paper/1.21.8/org/bukkit/World.html). Arcana-generated block protection events remain visible to other plugins but do not start new Arcana block-trigger roots; synchronous native damage is guarded at application.
