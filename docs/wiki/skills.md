# Arcana Skills

Arcana Skills, YAML ile beceri tanımlamak için Arcana'nın kendi efekt motorudur. Ayrı `arcana-skills` modülünde geliştirilir ve normal Arcana JAR'ına dahil edilir; ikinci bir eklenti kurman gerekmez. XP, seviye ve yetenek ağacı bu motorun parçası değildir.

## Dosya ve sınıf düzeni

```text
arcana-api/.../core/skills/
  SkillEffect.java          # Ortak efekt sözleşmesi
  SkillCondition.java       # Ortak koşul sözleşmesi
  SkillTrigger.java         # Ortak tetikleyici sözleşmesi
  SkillEngine.java          # Dış eklentilerin kullandığı API
arcana-skills/.../skills/
  effects/ParticleEffect.kt
  effects/HealEffect.kt
  effects/DamageEffect.kt
  conditions/HealthBelowCondition.kt
  conditions/PermissionCondition.kt
  triggers/MeleeAttackTrigger.kt
  triggers/BlockBreakTrigger.kt
  BuiltinComponents.kt      # Yalnızca kayıt listeleri
  compile/                  # YAML'den çalışma tanımı üretir
  execution/                # Sıra, zamanlama, iptal ve cooldown
```

Her efekt, koşul ve tetikleyici ayrı sınıftır. YAML'deki `type: particle`, kayıt sistemi üzerinden [ParticleEffect](../../arcana-skills/src/main/kotlin/com/keopsfenks/arcana/skills/effects/ParticleEffect.kt) sınıfına gider. Yeni tür eklemek için merkezi bir `switch/when` büyütülmez. Ortak Java ana sınıfları [SkillEffect](../../arcana-api/src/main/java/com/keopsfenks/arcana/core/skills/SkillEffect.java), [SkillCondition](../../arcana-api/src/main/java/com/keopsfenks/arcana/core/skills/SkillCondition.java) ve [SkillTrigger](../../arcana-api/src/main/java/com/keopsfenks/arcana/core/skills/SkillTrigger.java) dosyalarıdır.

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
