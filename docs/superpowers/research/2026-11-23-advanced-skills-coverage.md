# Arcana gelişmiş skill kapsam matrisi

Bu dosya onaylanan tasarımın uygulama sınırını gösterir. Ürün yalnızca Arcana'dır; başka eklenti projeleri oluşturulmaz. Referans ürünlerle dosya/ID uyumluluğu hedeflenmez. `Planlandı` durumu çalışan özellik anlamına gelmez.

Libreforge referans kimlikleri [CSV envanterinde](2026-11-23-libreforge-reference-identifiers.csv), kaynak commit'i `40fd6708698efd2cabd8e505a88ed293263c42ee` ile sabitlenmiştir. CSV yalnızca işlev kimlikleri ve kaynak bağlantıları içerir; kaynak uygulama kodu veya açıklama metinleri kopyalanmaz. Alias veya harici ürün entegrasyonu ayrı bir yerleşik mekanik gibi sayılmaz.

Referanslar: **L-E** [Libreforge effects](https://github.com/Auxilor/libreforge/blob/40fd6708698efd2cabd8e505a88ed293263c42ee/ecohub/configuring-an-effect.md), **L-C** [chains](https://github.com/Auxilor/libreforge/blob/40fd6708698efd2cabd8e505a88ed293263c42ee/ecohub/configuring-a-chain.md), **L-M** [mutators](https://github.com/Auxilor/libreforge/blob/40fd6708698efd2cabd8e505a88ed293263c42ee/ecohub/Developers/creating-a-mutator.md), **M-S** [Mythic skills](https://wiki.mythiccraft.io/mythicmobs/Skills/Skills), **M-T** [targeters](https://wiki.mythiccraft.io/mythicmobs/Skills/Targeters), **M-V** [variables](https://wiki.mythiccraft.io/mythicmobs/Skills/Variables), **M-C** [metaskills](https://wiki.mythiccraft.io/mythicmobs/Skills/Metaskills), **M-A** [auras](https://wiki.mythiccraft.io/mythicmobs/skills/mechanics/aura), **M-P** [projectiles](https://wiki.mythiccraft.io/mythicmobs/skills/mechanics/projectile).

Tablodaki çoklu isimler ayrı sınıf/dosyalardır; tek sınıfta tür switch'i anlamına gelmez. Yeni bileşenler `arcana:` namespace'inde kayıt edilir. Test sütunu davranış grubunu gösterir; uygulama planı kesin test adlarını ve assertion'ları taşır.

| Kimlik | Referans | Arcana karşılığı / dosya sınıfları | Görev | Durum | Davranış kanıtı |
| --- | --- | --- | --- | --- | --- |
| MOD | Mevcut Arcana | Motor/katalog ayrımı, tek dağıtım | 1 | Tamamlandı | ModuleBoundaryTests |
| API | M-S | SkillPlatform, SkillActor, SkillInvocation, legacy adaptör | 2,6 | Planlandı | PlatformCompatibilityTests |
| OWNER | L-E | Namespaced typed registry ve owner temizliği | 3 | Planlandı | PlatformRegistryTests |
| SOURCE | L-C,M-C | Sahipli directory/in-memory kaynaklar, seçici reload | 4 | Planlandı | DefinitionSourceTests |
| PARAM | L-C,M-C | Tipli parametreler, varsayılan, readonly child binding | 5 | Planlandı | ParameterBindingTests |
| VAR | M-V | Invocation/actor/target/server değişkenleri | 5 | Planlandı | VariableStoreTests |
| VALUE | M-V | SetVariableMechanic, AddVariableMechanic, RemoveVariableMechanic | 5 | Planlandı | VariableMechanicTests |
| CONTEXT | M-S,M-T | Origin/trigger/victim ayrımı ve snapshot | 6 | Planlandı | InvocationSchedulingTests |
| T-SELF | M-T | SelfTargeter, VictimTargeter, TriggerTargeter | 7 | Planlandı | TargeterTests |
| T-LOC | M-T | OriginTargeter, LocationTargeter, ForwardTargeter | 7 | Planlandı | TargeterTests |
| T-AREA | M-T | EntitiesInRadiusTargeter, PlayersInRadiusTargeter, BoxTargeter | 7 | Planlandı | TargeterGeometryTests |
| T-SHAPE | M-T | ConeTargeter, LineTargeter, RayTargeter | 7 | Planlandı | TargeterGeometryTests |
| T-SELECT | M-T | TargetSelectionPolicy: filtre/sıra/limit/rastgele | 7 | Planlandı | TargeterTests |
| M-ROLE | L-M | SwapActorTargetMutator, TargetAsActorMutator, TriggerAsTargetMutator | 8 | Planlandı | MutatorTests |
| M-LOC | L-M | OffsetLocationMutator, RotateLocationMutator, CenterLocationMutator | 8 | Planlandı | MutatorTests |
| M-DATA | L-M | SetValueMutator, MultiplyValueMutator, ClampValueMutator | 8 | Planlandı | MutatorTests |
| NODE | L-E,M-C | Node başına koşul/şans/delay/repeat/filter/skip | 8 | Planlandı | PlatformCompilerTests |
| FLOW-BASE | L-C,M-C | SequenceMechanic, DelayMechanic, RepeatMechanic, BranchMechanic, CastMechanic | 9 | Planlandı | FlowMechanicTests |
| FLOW-MULTI | L-C,M-T | ForEachMechanic, ParallelMechanic, WaitUntilMechanic | 9 | Planlandı | FlowMechanicTests |
| FLOW-PICK | L-C | WeightedRandomMechanic, CycleMechanic, ShuffleMechanic | 9 | Planlandı | FlowMechanicTests |
| ADMIT | L-E,M-C | Cooldown, every-N, charges/refill, ortak kabul kimliği | 10 | Planlandı | AdmissionTests |
| COST | L-E | SkillResourceProvider; health/food/vanilla XP sağlayıcıları | 10 | Planlandı | ResourceReservationTests |
| STAT | L-E,M-A | SkillStatProvider ve namespaced binding | 10,12 | Planlandı | StatContributionTests |
| EVENT-DMG | L-E,M-S | AddDamageInterceptor, MultiplyDamageInterceptor | 11 | Planlandı | InterceptorTests |
| EVENT-CANCEL | M-S | CancelEventInterceptor; capability/priority sınırları | 11 | Planlandı | InterceptorTests |
| HOLDER | L-E | HolderManager, SkillHolderProvider, slot değişim farkı | 12 | Planlandı | HolderLifecycleTests |
| PASSIVE | L-E,M-A | AttributeContribution, PotionContribution, katkı lease'leri | 12 | Planlandı | StatContributionTests |
| AURA-LIFE | M-A | AuraManager; start/tick/end/stack/refresh/removal | 13 | Planlandı | AuraLifecycleTests |
| AURA-OPS | M-A | ApplyAuraMechanic, RemoveAuraMechanic, AddAuraChargeMechanic | 14 | Planlandı | AuraMechanicTests |
| AURA-COMP | M-A | AuraTickComponent, AuraAttackComponent, AuraDamagedComponent, AuraStatComponent | 14 | Planlandı | AuraMechanicTests |
| PROJECTILE | M-P | ProjectileManager, bölge geçişi, segment çarpışması | 15 | Planlandı | ProjectileSimulationTests |
| PROJECTILE-OPS | M-P | ProjectileMechanic, MissileMechanic, RayMechanic, BeamMechanic | 16 | Planlandı | ProjectileMechanicTests |
| PROJECTILE-CHAIN | M-P,M-T | ChainMechanic; hit filtresi/homing/pierce/bounce | 15,16 | Planlandı | ProjectileSimulationTests |
| COMBAT | L-E,M-S | DamageMechanic, HealMechanic, FeedMechanic, IgniteMechanic, ExtinguishMechanic, AbsorptionMechanic | 17 | Planlandı | CombatMechanicTests |
| STATUS | L-E,M-S | PotionMechanic, RemovePotionMechanic, FreezeMechanic, StunMechanic | 17 | Planlandı | CombatMechanicTests |
| MOVE | L-E,M-S | VelocityMechanic, KnockbackMechanic, PullMechanic, LaunchMechanic, DashMechanic, TeleportMechanic | 17 | Planlandı | MovementMechanicTests |
| VISUAL | L-E,M-S | ParticleMechanic, ParticleLineMechanic, ParticleRingMechanic, SoundMechanic, StopSoundMechanic | 18 | Planlandı | VisualMechanicTests |
| TEXT | L-E,M-S | MessageMechanic, ActionBarMechanic, TitleMechanic, BossBarMechanic | 18 | Planlandı | PresentationMechanicTests |
| ITEM | L-E | GiveItemMechanic, RemoveItemMechanic, DurabilityMechanic, EnchantMechanic | 18 | Planlandı | ItemMechanicTests |
| WORLD | L-E | SetBlockMechanic, BreakBlockMechanic, LightningMechanic, ExplosionMechanic | 18 | Planlandı | WorldMechanicTests |
| COMMAND | L-E,M-S | ConsoleCommandMechanic, PlayerCommandMechanic | 18 | Planlandı | PresentationMechanicTests |
| COND-COMMON | L-E,M-S | AllPredicate, AnyPredicate, NotPredicate, ExpressionPredicate | 19 | Planlandı | PlatformConditionTests |
| COND-ACTOR | L-E,M-S | ActorTypePredicate, PermissionPredicate, HealthPredicate, FoodPredicate, MovementPredicate | 19 | Planlandı | PlatformConditionTests |
| COND-WORLD | L-E,M-S | WorldPredicate, BiomePredicate, TimePredicate, WeatherPredicate, HeightPredicate | 19 | Planlandı | PlatformConditionTests |
| COND-TARGET | M-T,M-C | DistancePredicate, LineOfSightPredicate, EntityTypePredicate, HasTargetPredicate | 19 | Planlandı | PlatformConditionTests |
| COND-DATA | L-E,M-V,M-A | EquipmentPredicate, VariablePredicate, ResourcePredicate, StatPredicate, HasAuraPredicate | 19 | Planlandı | PlatformConditionTests |
| TRIGGER-BASE | L-E,M-S | Mevcut 12 tetikleyici için invocation bridge | 20 | Planlandı | PlatformTriggerTests |
| TRIGGER-MOVE | L-E,M-S | MoveSkillTrigger, JumpSkillTrigger, TimerSkillTrigger | 20 | Planlandı | PlatformTriggerTests |
| TRIGGER-ITEM | L-E,M-S | EquipSkillTrigger, HeldSlotSkillTrigger, ConsumeSkillTrigger, ShootSkillTrigger | 20 | Planlandı | PlatformTriggerTests |
| TRIGGER-FIGHT | L-E,M-S | KillSkillTrigger, EntityAttackSkillTrigger, EntityDamagedSkillTrigger, CustomSkillTrigger | 20 | Planlandı | PlatformTriggerTests |
| BIND | L-C,M-V,M-C | Tipli param/var/stat/snapshot binding ve güvenli metin | 5,6,21 | Planlandı | BindingIntegrationTests |
| TOOLS | M-S | validate/inspect/trace, metadata/schema, bütçe sayaçları | 21 | Planlandı | SkillDiagnosticsTests |
| LIMITS | L-C,M-P,M-A | Hedef/fan-out/callback/hit/actor/server sınırları | 6,9,13,15,21 | Planlandı | PlatformLimitsTests |
| V1 | Mevcut Arcana | Şemasız YAML, eski Java çağrıları ve sonuçlar | 1,2,21 | Mevcut; regresyon gerekli | PlatformCompatibilityTests |
| PROVIDER | L-E,M-S | Economy/mana/özel stat/özel item API'si için genel sözleşme | 3,10,12 | Yalnızca genel API | ProviderLifecycleTests |
| NO-ADDONS | Kullanıcı kapsamı | Ayrı RPG/Items/Mobs eklenti veya ürün örnekleri | — | Kapsam dışı | Dağıtımda ek eklenti yok |
| NO-MOBS | Kullanıcı kapsamı | Mob/boss tanımı, AI ve faction/threat yönetimi | — | Kapsam dışı | Yalnızca aktör/skill API'si |
| NO-SPAWN | Kullanıcı kapsamı | Spawner, doğal spawn, mob/loot paketi yönetimi | — | Kapsam dışı | Yeni yönetici eklenmez |
| NO-RPG | Kullanıcı kapsamı | XP progression, sınıf, skill tree, mana ekonomisi | — | Kapsam dışı | Sadece provider kontratı |
| NO-COMPAT | Onaylı native şema | Libreforge/MythicMobs YAML veya paket uyumluluğu | — | Kapsam dışı | Native schema 1/2 |

Her `Planlandı` satırı, plandaki dosya ve testlerle tamamlanmadan bitmiş olarak raporlanamaz. Kaynak CSV daha geniş referans yüzeyini görünür tutar; satır sayısı Arcana'da o sayıda mekanik üretildiği anlamına gelmez. Yeni bir referans işlevinin bu sınırın dışında kaldığı anlaşılırsa bunun native karşılığı, provider bağımlılığı veya kapsam dışı gerekçesi kaydedilir; sessizce mevcut bir sınıfa alias verilerek eşdeğerlik iddia edilmez.
