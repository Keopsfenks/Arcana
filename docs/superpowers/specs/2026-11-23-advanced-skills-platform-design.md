# Arcana gelişmiş beceri platformu tasarımı

Durum: taslak; kullanıcı tasarım onayı bekleniyor. Bu belge uygulama tamamlandı iddiası değildir.

## Amaç ve kullanıcının kararları

Arcana, daha sonra geliştirilecek ArcanaItems, ArcanaRPG ve ArcanaMobs eklentilerinin ortak beceri altyapısı olacak. Amaç bu eklentilerde aynı efekt, koşul, hedefleme ve yürütme kodunu tekrar yazmadan içerik geliştirmeyi kolaylaştırmak.

Kullanıcı Libreforge ve MythicMobs benzeri gelişmiş beceri özelliklerini istedi. Kapsamı şimdilik skill sistemiyle sınırladı: mob tanımları, boss yönetimi, AI yönetimi, spawner ve ganimet yönetimi bu çalışmanın parçası değil. Kullanıcı motor ile bileşen kataloğunun iki modüle ayrılmasını seçti. Önceki Arcana'ya özgü YAML ve bağımsız uygulama kararı sürüyor; başka eklentilerin kaynakları veya YAML ayrıştırıcıları taşınmıyor.

ArcanaMobs kullanımını mümkün kılmak için oyuncu ve mob aktörlerini desteklemek tasarımcının önerisidir. Moblara beceri kullandıran API sağlanır; mob oluşturma/yönetme ürünü yapılmaz. ArcanaRPG'ye ait XP, sınıf, yetenek ağacı veya mana ekonomisi burada kurulmaz; bunları bağlayacak arayüzler sağlanır.

## İnceleme dayanakları ve sınır

- Libreforge `master`: `40fd6708698efd2cabd8e505a88ed293263c42ee`, 2026-11-21. Yerel inceleme kopyası ile uzak master eşleşti.
- [Efekt yapılandırması](https://github.com/Auxilor/libreforge/blob/40fd6708698efd2cabd8e505a88ed293263c42ee/ecohub/configuring-an-effect.md), [zincirler](https://github.com/Auxilor/libreforge/blob/40fd6708698efd2cabd8e505a88ed293263c42ee/ecohub/configuring-a-chain.md), [mutator](https://github.com/Auxilor/libreforge/blob/40fd6708698efd2cabd8e505a88ed293263c42ee/ecohub/Developers/creating-a-mutator.md) ve [geliştirici örneği](https://github.com/Auxilor/libreforge/blob/40fd6708698efd2cabd8e505a88ed293263c42ee/ecohub/Developers/full-example-triggered-effect.md).
- MythicMobs'un 2026-11-23 tarihinde erişilen resmi belgeleri: [beceriler](https://wiki.mythiccraft.io/mythicmobs/Skills/Skills), [hedefleyiciler](https://wiki.mythiccraft.io/mythicmobs/Skills/Targeters), [değişkenler](https://wiki.mythiccraft.io/mythicmobs/Skills/Variables), [metaskills](https://wiki.mythiccraft.io/mythicmobs/Skills/Metaskills), [auralar](https://wiki.mythiccraft.io/mythicmobs/skills/mechanics/aura), [projectile](https://wiki.mythiccraft.io/mythicmobs/skills/mechanics/projectile).

Bu referanslar davranış ve özellik envanteri içindir. Hedef, sınırlandırılan skill alanlarının Arcana karşılıklarıdır; başka ürünlerin bütün ek eklentilerini, ileride ekleyecekleri özellikleri veya hazır içerik paketlerinin doğrudan çalışmasını kapsamaz. Mevcut bir MythicMobs/Libreforge paketinin birebir çalıştığı iddia edilmeyecek.

## Mevcut yapı ve önemli eksikler

Mevcut motor 18 efekt, 13 koşul ve 12 tetikleyici içeriyor. YAML doğrulama, ifadeler, sıra/delay/repeat/branch/cast, ortak cooldown, sahipli kayıtlar, güvenli reload ve Folia zamanlama sınırları mevcut. Son doğrulanmış taban 1.389 test ve yedi NMS bağlantı kontrolüyle geçti.

`SkillContext` oyuncu caster ve tek victim ile sınırlı. Çoklu hedef, bağımsız origin/konum, tipli parametreler, değişken depoları, aura/projectile yaşam döngüleri, kaynak maliyetleri ve eklentiye ait tanım kaynakları bulunmuyor. Mevcut Bukkit tetikleyicileri MONITOR aşamasından sonra beceri çalıştırıyor; bu yol artık tamamlanmış bir hasar olayını iptal etmek veya değiştirmek için kullanılamaz.

## Değerlendirilen yaklaşımlar

1. **Tek `arcana-skills` modülünde ayrı paketler.** Daha az modül bağlantısı gerektirir; büyük bileşen kataloğu ile yürütme motoru aynı derleme sınırında kalır.
2. **Motor ve katalog ayrı modüller — kullanıcının seçimi.** `arcana-skills` yürütme ve yaşam döngüsünü; `arcana-skills-components` standart bileşenleri taşır. Katalog genişlerken motor bağımsız test edilir. Başlatma ve bağımlılık bağlantılarının düzenlenmesi gerekir. Kurulum tek JAR olarak kalır.

## Modüller ve sorumluluklar

| Yer | Sorumluluk |
| --- | --- |
| `arcana-api/core/skills` | Java ile kullanılan kararlı sözleşmeler; aktör/hedef tanımları, derleyici, bileşen kayıtları, kaynak sağlayıcıları, tanım kaynakları ve sahiplik |
| `arcana-skills` | Tanım kaynaklarını derleme, hedefleme yürütmesi, bağlam/değişkenler, kabul ve maliyetler, event aşamaları, zamanlama, yönetilen işler ve reload |
| `arcana-skills-components` | Standart efekt, koşul, tetikleyici, hedefleyici, mutator ve interceptor sınıfları; yalnızca kayıt yapan katalog |
| `arcana-core:core-plugin` | Arcana servisini başlatma, standart kataloğu bağlama, komutlar ve mevcut servislerle entegrasyon |
| Gelecekteki ArcanaItems/RPG/Mobs | Kendi içerikleri, tanım kaynakları, özel bileşenleri ve veri/kaynak sağlayıcıları |

Motor standart kataloğa veya gelecekteki eklentilere bağımlı olmaz. Katalog API'ye ve gerektiğinde motorun ortak yardımcılarına bağımlı olabilir; core/NMS implementation sınıflarına bağımlı olmaz. Core her iki modülü derleme sırasında görür; kök dağıtım ikisini bir kez içerir. `SkillsRuntime` katalog kurulumunu dışarıdan alır; motorda `BuiltinComponents` referansı kalmaz.

Her efekt, koşul, tetikleyici, hedefleyici, mutator ve interceptor ayrı sınıf/dosyadır. Merkezi tür switch'i eklenmez. Yeni sınıflar metadata ve derleme sözleşmeleriyle registry üzerinden bulunur.

## Eklenti geliştirme sözleşmesi

Eklentiler Arcana'ya bağımlılık bildirir ve `Arcana.get().getSkillEngine()` üzerinden servis alır. Ayrı bir skills eklentisi yüklenmez. Public API Kotlin fonksiyon tipleri yerine Java arayüzleri, kayıt tanıtıcıları, immutable değerler ve `CompletionStage` kullanır.

Önerilen genişletme noktaları: `SkillActor`, `SkillTargeter`, `SkillMutator`, `SkillInterceptor`, `SkillDefinitionSource`, `SkillHolderProvider`, `SkillResourceProvider`, `SkillStatProvider` ve bileşen argüman metadata'sı. Nihai yardımcı sınıf isimleri uygulama planında kesinleştirilir; görevleri bu belgede sabittir.

- ArcanaItems: eldeki/eşyalardaki aktif beceri kümelerini holder/slot sağlayıcısıyla bildirir; eşyanın gücü gibi değerleri tipli parametre olarak sunar.
- ArcanaRPG: mana/stamina, oyuncu statları ve kendi profil değerlerini sağlar; beceri maliyetini ortak motor yürütür.
- ArcanaMobs: yaşayan entity aktörünü ve kendi olaylarını sağlar; aynı tanım/efekt motoruyla beceri çağırır.

Bu üç kullanım ayrı test eklentisi fixture'ları ve derlenen Java örnekleriyle doğrulanır. Gerçek ArcanaItems/RPG/Mobs ürünleri bu çalışmada oluşturulmaz.

### Kimlikler, tanım kaynakları ve sahiplik

Yeni kayıtlar ve beceriler namespaced kimlik kullanır: `arcanaitems:fire_sword`, `arcanarpg:dash`. Yerleşik kısa adlar `arcana` alanına çözülür. Eklentiler yalnızca kendi kayıt alanını yönetir. Aynı kısa isim farklı alanlarda çakışmaz; açıkça nitelendirilmiş başvurular alanlar arası paylaşımı sağlar.

Eklenti kendi yapılandırma klasörünü veya immutable tanım koleksiyonunu `SkillDefinitionSource` olarak kaydeder. Kayıt sahibi, kaynak kimliği ve namespace derleyicide korunur. Arcana başka eklentinin dosyalarını değiştirmez. Şema hataları kaynak eklenti, dosya, beceri ve alan yoluyla döner.

Her kayıt `AutoCloseable` sahiplik tanıtıcısı verir. Eklenti kapanınca ona ait kaynak, bileşen ve sağlayıcılar kaldırılır; doğrudan ve dolaylı bağımlı işler iptal edilir. Alakasız eklentilerin kayıtları yaşamaya devam eder. Kayıt kabulü ile disable/shutdown geçişleri atomik olur; dış eklenti callback'leri yaşam döngüsü kilidi altında çağrılmaz.

Global reload mevcut sözleşme gibi bütün aday kaynakları doğrular ve başarısız doğrulamada mevcut işleri korur. Yeni kaynak-bazlı reload, yalnızca değişen tanımları ve onlara bağımlı yürütmeleri geçersiz kılar. Eski snapshot içinden dinamik olarak artık geçersiz bir tanım çağrılırsa eski tanım yürütülmez. Kaynak kaldırılması da aynı bağımlılık geçerliliği denetimine girer.

## Aktörler ve eski API'nin korunması

Aktör oyuncu veya yaşayan entity olabilir. Oyuncuya özgü izin, envanter, yemek ve XP işlemleri actor capability kontrolü yapar; mob için sessizce başarı sayılmaz. Konum hedefi ve origin, aktörden bağımsız immutable dünya kimliği/koordinat değerleriyle temsil edilir.

Mevcut `SkillContext.builder(Player)`, `getCaster(): Player`, `SkillEffect`, `SkillCondition`, `SkillTrigger` ve `SkillEngine` kullanım yolları kaynak/binary uyumlu tutulur. `getCaster()` metodu mob döndürmek için yeniden tiplendirilmez veya mevcut oyuncu çağrılarında null dönmez. Genel aktörler için yeni invocation sözleşmesi ve açık bir adaptör sınırı eklenir. Eski Player API'siyle yazılmış bileşenler Player-only olarak işaretlenir; mob yürütmelerinde bu bileşenlere uygun olmayan bağlam verilmez. Yeni katalog genel invocation API'sini kullanır.

API bridge ayrıntıları uygulama planında bir kez kesinleştirilir. Zorunlu kabul ölçütü: değiştirilmemiş eski Java genişletmesi derlenir ve oyuncu çağrıları aynı davranır; mob çağrısı yanlış Player cast'i veya null hatası üretmez.

## Hedef özellik aileleri ve tamamlanma ölçütleri

| Aile | Hedef davranış |
| --- | --- |
| Hedefleme | Self, trigger, victim, origin; entity/oyuncu yarıçapı, bakış/ray, koni, çizgi, kutu; filtre, sıralama, limit, rastgele seçim; entity ve konum hedef kümeleri |
| Mutator | Bağlam kopyasında aktör/hedef değişimi, konum öteleme/döndürme, yön/origin ve sayısal değer dönüşümleri; kardeş adımların verisini değiştirmeme |
| Parametre ve değişken | Tipli varsayılanlı parametreler; invocation, actor, target ve server kapsamları; set/add/remove/compare; süreli değerler ve açıkça seçilen kalıcılık |
| Zincir ve akış | Mevcut akışlara ek parametreli alt beceriler, foreach, weighted-random, cycle/shuffle, wait-until ve sınırlandırılmış paralel dallar |
| Koşullar ve node seçenekleri | Caster/target/trigger koşulları; node bazlı şans, gecikme, tekrar, filtre, fail/skip davranışı; zaman, biyom, mesafe, görüş, hareket, ekipman ve değer kontrolleri |
| Holder ve pasif etki | Ekipman/slot veya dış sağlayıcıya bağlı aktif kümeler; açılma/kapanma/değişim olayları; katkı kimliğiyle kaldırılan stat/potion/pasif lease'leri |
| Maliyet, cooldown, charges | Ortak cooldown, every-N sayacı, yükler ve dolum; dış kaynak sağlayıcısına rezervasyon; yetersiz kaynakta etkisiz ret |
| Event interception | Olay dönmeden önce hasar ekleme/çarpma, uygun olay iptali ve yerel olay verisi değişimi; normal görsel/asenkron zincirden ayrı güvenli aşama |
| Aura | Start/tick/end hook'ları; süre, stack, yenileme/değiştirme, charges ve removal nedenleri; buff/debuff durum sorguları |
| Projectile ve ray | Başlat/tick/hit/end; hız, yön, gravity, homing, hitbox, piercing/bounce ve hedef filtresi; beam/ray ve zincirleme hedef mekanikleri |
| Efekt kataloğu | Hasar/iyileştirme, statlar, hareket/teleport, potion/durum, particle/geometri, ses/metin/title/bossbar, eşya/envanter işlemleri, blok/dünya işlemleri, kaynak/değişken/counter ve zincir çağrıları |
| Tetikleyici kataloğu | Mevcut olayların yanında hareket, zıplama, ekipman/slot, tüketme, ok atma, öldürme, hasar türleri, timer ve dış eklenti olayları; gerekli veri capability'sini açık bildirme |
| Placeholder ve ifadeler | Parametre/değişken/stat/aktör/hedef/origin bağlaması; matematik derleme; tip hataları ve eksik veri için belirli sonuç; metin girdisini yeniden komut/ifade olarak ayrıştırmama |
| Geliştirici araçları | Metadata'dan argüman/schema belgesi, validate/inspect/trace komutları, sahiplik ve hata teşhisi, çağrı maliyeti ve sınır sayaçları; trace varsayılan kapalı |

Uygulama planından önce referans envanterinden bir kapsam matrisi çıkarılır. Her ilgili bileşen/fonksiyon için referans, Arcana karşılığı, gerekli sağlayıcı, durum ve davranış testi kaydedilir. Eşanlamlılar ayrı özellik sayılmaz. Harici ürün gerektiren işlevler kayıt edilebilir entegrasyon sözleşmesi olarak açıkça ayrılır; mevcut olmayan sağlayıcı sessiz no-op oluşturmaz. Skill dışı mob/spawn/loot yönetimi satırları gerekçesiyle kapsam dışı işaretlenir.

Yalnızca registry veya iskelet sınıf eklemek bir satırı tamamlamaz. Planın kabul ettiği her satır çalışır örnek, argüman doğrulama ve davranış testiyle tamamlanır; kalan satırlar görünür tutulur. Bu belge bütün katalog tamamlanmış gibi raporlanamaz.

## Yürütme modeli

Normal yol: olay anında tanım/revision bağlama → immutable olay verisi → actor snapshot → hedef seçimi → admission → sıralı/akışlı node yürütme → sonuç/temizlik. Her callback kendi entity veya konum sahibinin zamanlayıcısında çalışır. Origin, trigger ve seçilmiş hedef ayrı kavramlardır.

Hedefleyiciler entity referanslarını canlı durum okuyarak yabancı Folia bölgelerinde taramaz. Gerekli konum/kimlik snapshot'ları sahip bölgelerinde güncellenen bir indeks veya bölgelere ayrılmış güvenli sorgu yoluyla alınır; seçilen hedef uygulamadan önce kendi bölgesinde tekrar doğrulanır. Desteklenmeyen sorgu güvenli olmayan global taramaya düşmez.

Kök çağrı, alt çağrılar ve paralel dallar aynı iptal/dependency/bütçe ailesini paylaşır. Her dalın yerel değişken çerçevesi ayrıdır; ortak değişken yazımı açık bir işlem üzerinden atomik yapılır. Normal sequence sırasını değiştirmeden paralellik yeni bir açık node ile istenir. Başarısız sequence durur; foreach varsayılan olarak hedef bazlı sonuç toplar, fail-fast seçeneği açıktır.

### Olayı etkileyen eşzamanlı aşama

Event interceptor olay callback'i içinde, olay tamamlanmadan çalışır. Yalnızca interceptor-capable yerel koşul/işlem planları kabul edilir. Delay, async sağlayıcı, çapraz bölge hedefleme veya projectile bu aşamada derleme hatasıdır. Olay referansı bu aşamadan çıkmaz; normal zincire yalnızca snapshot taşınır. Olayın durumu daha sonra okunduğu varsayılmaz.

Interception ve sonraki zincir aynı admission kimliğini kullanır; tek tetiklemede cooldown/maliyet iki kere tüketilmez. İptal edilmiş olaylara ilişkin tetikleyici davranışı metadata ve testlerde belirgindir; mevcut tetikleyicilerin iptal edilmiş olayı yok sayma davranışı korunur. Skill kaynaklı hasarın gerçek uygulanma noktası recursion guard içindedir; gecikmeli callback'ler de nedensellik kimliği ve üst sınırlar taşır.

### Maliyet, değişkenler ve pasif katkılar

Koşul/şans reddi maliyet tüketmez. Kaynak sağlayıcısı rezervasyon/commit/release sözleşmesine uyar. Birden fazla maliyetin rezervasyonunda veya cooldown kabulünde hata olursa henüz uygulanmamış rezervasyonlar bırakılır. Admission tamamlandıktan sonraki efekt hatası harcanmış kaynakları ve cooldown'u otomatik geri almaz. Desteklenmeyen kaynak türü doğrulama hatasıdır.

Invocation değerleri çağrı sonunda kaybolur. Actor/server değişkenleri varsayılan bellektedir; kalıcılık açıkça seçilir ve mevcut Arcana veri altyapısına bağlanır. Oyuncu değerleri UUID ile saklanır. Entity değişkenleri entity yaşam döngüsüne bağlıdır; bu çalışma kalıcı mob kayıt sistemi kurmaz. Server kapsamı sunucuya özeldir; otomatik ağlar arası ortak durum iddiası yoktur. Veri türleri boyut/derinlik sınırlıdır; canlı Bukkit nesneleri depoya konmaz.

Pasif stat ve potion katkıları sahipli lease/modifier kimliğiyle eklenir ve kaldırılır. Kaldırmada bütün değeri eski snapshot'a sıfırlamak, başka eklentinin güncel katkısını silmek veya bütün potion etkilerini temizlemek yasaktır. Slot/holder değişimlerinde fark uygulanır; fallback yeniden değerlendirme periyodu sınırlıdır, her tick bütün oyuncu/katalog taranmaz.

### Yönetilen aura ve projectile işleri

Bu işler owner, tanım revision'ı, actor/target ve iptal nedeni ile izlenir. Reload, owner kaldırılması, entity emekliliği, quit ve shutdown için açık temizleme yolları vardır. Tick callback'leri sınırsız yeni kök çağrı açarak bütçeyi atlayamaz. Kalıcı holder lease'leri de sahiplik kaldırıldığında sökülür.

Aura stack anahtarı tanım/target ve yapılandırılmış caster paylaşım politikasından oluşur. Start/end hook'ları bir kez çalışır; expiry, overwrite, dispel, reload ve owner removal nedenleri ayrı taşınır. Temizlik callback'leri kapatılmış eklenti koduna veya geçersiz eski tanımlara yeniden giriş yapmaz.

Projectile dünya/konum sahipliği değiştikçe zamanlamasını taşır. Hızlı hareket segmentler halinde çarpışma kontrolüne girer; aynı hedefe vuruş sıklığı ve maksimum toplam hit sayısı sınırlıdır. Görsel particle sayısı ile hasar hit sayısı birbirinden bağımsızdır. Event ve entity referansları süresiz tutulmaz.

## Sınırlar ve hata davranışı

Mevcut sınırlar korunur: 16 nested cast, 1.024 normal effect step, 100 repeat, en fazla 24 saat gecikme. Çoklu hedeflerde her uygulama step bütçesine sayılır. Yeni invocation başlatmak veya paralel dal açmak mevcut kökün sınırını sıfırlamaz.

Tasarımcının başlangıç önerisi: seçim başına varsayılan 64, hard limit 256 hedef; invocation başına 16 paralel dal; event interceptor için 64 yerel işlem. Yönetilen aura/projectile başına toplam callback/step ve actor/sunucu eşzamanlı iş limitleri ayrıca yapılandırılır; varsayılanları yük testleriyle kesinleştirmek uygulayıcıya bırakılır. Hard limitler ve sonlu süre zorunludur; pasif lease yalnızca açık sahibi yaşadığı sürece kalabilir.

Geçersiz sayı/ifade/enum, bilinmeyen alan, eksik sağlayıcı ve tür uyuşmazlığı derleme hatasıdır. Admission reddi, desteklenmeyen actor, hedefin kaybolması, budget aşımı, iptal ve çalışma hatası birbirinden ayrılır. Otomatik hatalar kimlik ve alan yoluyla, oran sınırlı raporlanır. Kısmi oyun etkileri geri alınmış gibi gösterilmez.

## YAML ve API geçişi

Şemasız mevcut dosyalar schema 1 olarak aynı şekilde çalışır. Yeni dosyalarda `schema: 2` kullanılır. `type`, `effects` ve okunabilir mapping/list düzeni korunur; MythicMobs'un tek satırlık DSL'i eklenmez. Eski kısa kimlik, numeric/enum/duration davranışı ve cooldown sonuçları regresyon testleriyle sabitlenir.

Yeni şema taslağı:

```yaml
schema: 2
id: arcanarpg:arcane_bolt
parameters:
  power: { type: number, default: 6 }
triggers: [arcana:right_click]
cooldown: 2s
targets:
  type: arcana:ray
  range: 24
effects:
  - type: arcana:projectile
    speed: 16
    lifetime: 3s
    on-hit:
      - type: arcana:damage
        amount: "param_power"
```

Parametreler ifade derleyicisine tipli binding olarak verilir; bir metin yerine koyup yeniden ayrıştırma yapılmaz. Kapsam isimlerinin ve rezerv anahtarların kesin listesi metadata/schema dokümanında yer alır. Eski örnekler değişmeden derlenir; schema 2 için ayrı, kapalı örnekler eklenir.

## Teslim sırası ve doğrulama

Bu bölüm uygulama görev planı değil, bağımlılık sırasıdır:

1. Referans özellik matrisi, API uyumluluk kontratı, modül/katalog ayrımı, sahipli namespaced tanım kaynakları ve addon örnekleri.
2. Genel aktör/invocation, çoklu hedef ve origin modeli, mutator, parametre/değişken ve genişletilmiş akış yürütmesi.
3. Node kabul seçenekleri, maliyet/charges/stat sağlayıcıları, event interceptor, holder/pasif lease'leri.
4. Aura ve projectile/ray yaşam döngüleri ile bağlı mekanikler.
5. Kapsam matrisindeki diğer efekt/koşul/tetikleyiciler, metadata/schema, teşhis ve yük kontrolleri.

Her aşama mevcut schema 1 dosyalarını ve Java genişletme örneğini korur. TDD ve belirleyici scheduler/clock/RNG testleri kullanılır. Özellikle çoklu Folia bölgeleri, actor emekliliği, parent-child dependency graph, kaynak rezervasyonu, eşzamanlı unload/register, reload öncesi olaylar, pasif katkı temizliği ve aura/projectile hit bütçeleri kapsanır.

Tam proje derlemesi ve yedi NMS bağlantı kontrolü sürer. Katalogda kaynak kodu/format kopyası yerine Arcana uygulaması bulunduğu ve tek JAR'ın iki modülü içerdiği doğrulanır. Son aşamada bağımsız bütün değişiklik incelemesi yapılır. Canlı Paper/Folia testi yapıldıysa sürüm ve senaryo raporlanır; yapılmadıysa açıkça belirtilir.

## Uygulayıcıya bırakılan tercihler

Paket alt dizinleri, yardımcı sınıf isimleri, atomik veri yapıları, sorgu indeksinin iç düzeni, yük testinden çıkan varsayılan iş limitleri ve test fixture düzeni uygulayıcıya bırakılır. Bunlar public sözleşmeleri, actor desteğini, eski API uyumunu, sahiplik izolasyonunu veya kapsam matrisini küçültmek için kullanılamaz.

Belge onayından sonra ayrıntılı uygulama planı hazırlanır. Bu tasarım onaylanmadan ürün kodu veya yeni modül iskeleti eklenmez.

## Tasarım incelemesi

2026-11-23 tarihinde bağımsız, yeni bağlamlı uygulayıcı incelemesi yapıldı. İnceleyici, plan hazırlamayı engelleyen önemli bir açık kalmadığını bildirdi. Kullanıcı tasarım onayı hâlen bekleniyor.
