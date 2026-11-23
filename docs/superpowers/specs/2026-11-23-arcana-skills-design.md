# Arcana Skills tasarımı

## Amaç ve alınan kararlar

Arcana'ya YAML ile düzenlenebilen, başka eklentilerin de genişletebildiği bir beceri/efekt motoru eklemek. Kullanıcı ayrı `arcana-skills` modülünü önerdi; ilk kapsam olarak YAML motorunu ve Arcana'ya özel bağımsız şemayı seçti.

Bu aşamada XP, seviye, sınıf seçimi veya yetenek ağacı yoktur. Libreforge fikir düzeyinde referanstır; kaynak kodu taşınmaz ve mevcut libreforge YAML dosyaları için birebir uyumluluk sözü verilmez.

## Yaklaşımlar

1. **Arcana'ya özel motor ve şema — seçilen yaklaşım.** Mevcut API, ifade motoru, metin işleme ve zamanlayıcı kullanılır. Modüller bağımsız geliştirilebilir; başka biçimlere ait geçmiş davranışları taklit etmek gerekmez.
2. **Libreforge yapılandırma uyumluluğu.** Mevcut dosyaların taşınmasını kolaylaştırır, fakat efekt ve argüman eşlemesi ile çalışma sırası uyumluluğunu ayrıca gerektirir. Kullanıcı bu yolu seçmedi.

## Modüller ve bağımlılıklar

- `arcana-api`: Java ile kullanılabilen `core.skills` sözleşmeleri; motor, bağlam, sonuçlar, kayıt fabrikaları ve genişletme noktaları.
- `arcana-skills`: Kotlin uygulaması; YAML yükleme/derleme, tetikleyici yönlendirme, koşullar, filtreler, efektler, hedefleme ve yürütme durumu. `arcana-api` ve sunucu API'sine bağımlıdır; core implementation veya NMS modüllerine bağımlı olmaz.
- `arcana-core:core-plugin`: motoru başlatır/kapatır, mevcut reload ve komut akışına bağlar. Skills modülünü derleme sırasında görür.
- Kök proje: skills uygulamasını mevcut dağıtım JAR'ına dahil eder. Ayrı bir Skills eklentisi kurmak gerekmez.
- Bağımlılık döngüsü oluşturulmaz. Skills public API'si mevcut `arcana-api` artifact'ından alınır.

Uygulama kodu `com.keopsfenks.arcana.skills` altında, public sözleşmeler `com.keopsfenks.arcana.core.skills` altında bulunur.

## Yapılandırma

Her beceri `plugins/Arcana/skills/` altında ayrı bir `.yml` dosyasıdır; alt klasörler desteklenir. Dosyanın içindeki `id` zorunludur; dosya adı değişse bile kimlik sabit kalır. Kimlik küçük harf, rakam ve `_` içerir. Yinelenen kimlikler yükleme hatasıdır. `_` ile başlayan örnek dosyalar yüklenmez.

```yaml
id: vampiric_strike
enabled: true
triggers: [melee_attack]
permission: arcana.skill.vampiric_strike
chance: 25
cooldown: 3s
cooldown-group: vampiric

filters:
  entities: [ZOMBIE, SKELETON]
  worlds: [world]

conditions:
  - type: health_below
    ratio: 0.75

effects:
  - type: damage
    target: victim
    amount: "2 + damage * 0.1"
  - type: heal
    target: caster
    amount: 1
  - type: message
    text: "&5Can çaldın!"
```

- Tetikleyiciler OR, koşullar AND olarak değerlendirilir. Boş tetikleyici listesi yalnızca API/komut üzerinden çağrılan beceri demektir.
- İzin verilmezse beceri bütün uygun oyuncular için çalışır; izin alanı varsa kontrol oyuncu üzerinde yapılır.
- Şans yüzde olarak 0–100 arasıdır. Cooldown süreleri `ms`, `s`, `m`, `t` birimleriyle yazılabilir; `t` Minecraft tick'idir.
- Sayısal efekt değerleri sabit veya Arcana ifade motoruyla derlenen matematiksel ifadelerdir. İlk değişkenler: `damage`, `caster_health`, `caster_max_health`, `caster_food`, `caster_level` (vanilla XP seviyesi). Olayda bulunmayan `damage` değeri 0'dır.
- Metinler Arcana renk/MiniMessage altyapısını kullanır. Yerleşik `{player}`, `{skill}` ve `{target}` değişkenleri desteklenir; oyuncu adı sadece metin girdisidir, yeni bir ifade veya yapılandırma olarak tekrar ayrıştırılmaz.
- Yapılandırma dosya yolu, beceri kimliği ve hatalı alan yolu ile açıklanır. Bilinmeyen efekt/koşul/tetikleyici ve geçersiz argümanlar sessizce atlanmaz.

## Çalıştırma sırası ve sonuçlar

1. Tetikleyiciye bağlı adayları hazır bir indeksten seç.
2. Beceri etkinliği, izin, filtreler ve koşulları kontrol et.
3. Şansı değerlendir, ardından oyuncu/cooldown-grubu için atomik cooldown rezervasyonu al.
4. Efekt listesini yapılandırmadaki sırayla yürüt; gecikmeli adımlar sonraki adıma tamamlandıktan sonra geçer.
5. Sonucu çağırana bildir; bir becerinin hatası başka becerilerin çalışmasını engellemez.

Cooldown anahtarı oyuncu UUID'si + grup; grup belirtilmezse beceri kimliği. Süre ölçümü duvar saatinden bağımsızdır. Reddedilen çağrı cooldown tüketmez. Başlayan çağrının cooldown'u sonraki efekt başarısız olsa da korunur; kısmen uygulanmış etkiler geri alınmaya çalışılmaz.

Bir efekt başarısız olursa o zincirin kalan adımları çalışmaz; daha önce uygulanan adımlar geri alınmaz. İç içe `cast` çağrısı hedef becerinin izin, koşul, şans ve cooldown kontrollerini yeniden uygular; aynı yürütme bütçesini paylaşır ve reddedilme sonucunu dış zincire taşır. Sayısal oyuncu değişkenleri çağrı başında oyuncunun sahip olduğu bağlamdan alınan kopyadır; gecikme sonrası kendiliğinden güncellenmez.

Public sonuçlar en az başarı, bilinmeyen beceri, kapalı beceri, izin/koşul/filtre reddi, şans reddi, cooldown, eksik hedef, iptal ve yürütme hatasını ayırt eder. Gecikmeli yürütmeler için tamamlanabilir bir sonuç döndürülür.

## İlk sürümün tetikleyicileri

Oyuncu katılma, yeniden doğma, ölme; blok kırma/yerleştirme; yakın dövüş saldırısı, oyuncunun attığı merminin isabeti, hasar alma; sağ/sol tıklama; eğilme ve koşmaya başlama; elle/API üzerinden çağırma. İptal edilmiş oyun olayları otomatik tetiklemez. Mermi hasarında saldırgan olarak oyuncu atıcı çözülür.

Ölüm tetikleyicisi mesaj, komut ve görsel bildirimler için kullanılabilir; ölü bir varlığı iyileştirmek veya hasarlamak başarı sayılmaz. Hareket olayına her tick dinleyici eklenmez; periyodik pasif beceriler ve kalıcı modifier yönetimi sonraki genişleme olabilir.

## İlk sürümün yapı taşları

**Filtreler:** dünya adı, blok materyali, hedef varlık türü ve oyuncunun ana elindeki eşya materyali. Birden fazla filtre alanı AND, tek alanın değerleri OR. Tetikleyicide mevcut olmayan veriye yönelik dolu filtre eşleşmez.

**Koşullar:** izin, dünya, oyuncu sağlığı oranı altı/üstü, açlık altı/üstü, eğiliyor/koşuyor olma, hedef bulunması ve matematiksel ifade. `all`, `any`, `not` birleşimleriyle iç içe koşullar kurulabilir. Bilinmeyen koşul kayıt sırasında hata verir.

**Efektler:** mesaj, action bar, ses, parçacık, iyileştirme, ek hasar, açlık/doygunluk, ateşe verme, iksir etkisi ekleme/kaldırma, hız vektörü, konsol/oyuncu komutu ve başka beceriyi çağırma.

**Akış efektleri:** `sequence`, `delay`, `repeat`, koşullu `branch` ve kayıtlı başka bir beceri için `cast`. Bunlar yeni oyun eklentisi yazmadan daha karmaşık davranış kurmaya yeter. Rastgele seçim ve alan hedefleme gibi yeni türler public kayıt mekanizmasıyla eklenebilir.

**Hedefler:** `caster` ve olayın `victim` hedefi. Hedef belirtilmezse `caster`. Hedef isteyen efekt hedef yoksa sessizce yanlış oyuncuyu seçmez; açıklanabilir bir sonuç döndürür.

Örnekler kapalı olarak gelir; ilk kurulumda bütün oyunculara kendiliğinden yetenek dağıtılmaz. Kullanıcı örnekleri `enabled: true` yapabilir ve izin/filtre ekleyebilir.

## Genişletilebilirlik

- Kullanıcının tasarım onayıyla eklediği zorunluluk: her efekt, koşul ve tetikleyici ayrı sınıf ve ayrı dosyadır. Ortak Java sözleşmeleri/ana sınıfları üzerinden kaydedilir. Örneğin `ParticleEffect`, `HealEffect`, `PermissionCondition`, `HealthBelowCondition`, `MeleeAttackTrigger` ve `BlockBreakTrigger`. Yeni bir tür eklemek için merkezi `switch/when` bloğu büyütülmez; kayıt listesine sınıf eklenir veya dış eklenti kayıt API'sini çağırır.
- Java ve Kotlin eklentileri efekt ve koşul fabrikalarını kimlikle kaydedebilir; mevcut kimliği sessizce ezmek yerine açık çakışma hatası döner.
- Dış eklenti kendi oyun olayından motorun tetikleme veya tek beceri çağırma API'sini kullanabilir. Özel tetikleyici kimlikleri kaydedilebilir.
- YAML derlemesinde factory argümanları doğrulanır; olay anında YAML veya matematik ifadesi yeniden derlenmez.
- API, derlenmiş kayıtları değiştirilebilir iç listeler halinde dışarı vermez.
- Dış eklentinin kayıtlarını kapatırken kaldırabilmesi için sahiplik ve kapatılabilir kayıt tutamacı bulunur. Sahibi kapandığında kayıtlar otomatik kapanır.
- Kayıt kaldırılınca ona bağlı beceriler hemen çağrı kabul etmeyi bırakır; bağlı bekleyen yürütmeler iptal edilir. Hâlihazırda çalışmaya başlamış bir callback tamamlanabilir, fakat kapalı kayda yeni bir callback veya sonraki adım başlatılmaz. Kuyruktaki callback çalışacağı bağlamda kaydın hâlâ açık olduğunu tekrar denetler.
- Bu bağımlılıklara sahip tanımlar diskten silinmez; listede bağımlılık hatası açıklanır. Sonraki reload bunları hâlâ içeriyorsa derleme başarısız olur. Bağımlılığı kaldıran geçerli bir reload normal şekilde etkinleşir.

## Reload, görevler ve iş parçacıkları

- Bütün YAML dosyaları önce yeni bir kayıt görüntüsüne derlenir. Herhangi biri hatalıysa mevcut motor ve çalışan görevler korunur; kısmi reload uygulanmaz.
- Mevcut genel `ArcanaPlugin.reload` akışı görevleri lifecycle çağrılarından önce iptal ediyor. Bu yüzden genel reload'a da görev iptalinden ve config güncellemesinden önce çalışan bir doğrulama aşaması eklenir. Yeni bir başarısızlık sonucu döndüren reload yolu komutlar tarafından kullanılabilir; mevcut `void reload()` girişleri kaynak/binary uyumluluğu için korunur ve bu yolu çağırır. Varsayılan doğrulama diğer eklentilerde davranışı değiştirmez. Skills doğrulaması başarısızsa genel reload iptal edilir ve komut başarı mesajı göndermez.
- Başarılı reload yeni görüntüye tek seferde geçer, önceki görüntünün bekleyen zincirlerini iptal eder ve oyuncu cooldown'larını korur. Değişen tanımlar eski gecikmeli adımlarla karışmaz.
- Motor kapanırken görevler ve dinleyiciler kaldırılır. Oyuncu ayrılırken oyuncuya ait cooldown ve bekleyen yürütmeler temizlenir.
- Gecikmeler BukkitScheduler üzerinden doğrudan kurulmaz; mevcut Arcana Scheduler kullanılır. Canlı varlık veya konum üzerinde işlem o varlık/konumun sahip olduğu bağlamda yapılır.
- Tetikleyici verileri anlık kopyalar olarak taşınır. Yabancı Folia bölgesindeki oyuncu veya hedefin canlı durumu başka bölgeden okunmaz. Oyuncu durumu oyuncu zamanlayıcısında, hedef durumu hedef zamanlayıcısında doğrulanır.
- Oyun olayının kendisi gecikmeli adımlara taşınmaz. Hasar efekti yeni hasar uygular; kapanmış bir olayı sonradan değiştirmeye çalışmaz.
- Beceri kaynaklı hasar aynı motoru sonsuz tekrar tetiklemez. İç içe `cast` derinliği ve toplam adım sayısı sınırlandırılır.
- Her yürütme en fazla 16 iç içe çağrı, 1.024 efekt adımı, tekrar başına en fazla 100 döngü kullanabilir. Gecikme en fazla 24 saat; sayıların NaN/sonsuz olması geçersizdir. Bu sınırlar hata durumlarında açıklanır.

## Yönetim ve belgeler

Çekirdek komut API'si kullanılarak bir `/arcana` yönetim komutu eklenir; mevcut standalone çekirdek bu kök komutu henüz kaydetmiyor. Altında `/arcana skills list`, `/arcana skills cast <id> [oyuncu]`, `/arcana skills reload` bulunur. Yönetim izni `arcana.command.skills`; isteğe bağlı beceri izni bunun yerine geçmez. Konsol çağrısında oyuncu açıkça belirtilir. Oyuncu ve beceri adları için sekme tamamlama yapılır.

`/arcana reload` skills yüklemesini de çalıştırır; skills YAML hatası etkin kayıtları silmez. Reload sonucu komutu çalıştırana ve loga sayılarla bildirilir.

Türkçe başlangıç kılavuzu; tam YAML şeması, yerleşik kimlikler, hedef/ifade değişkenleri, Java genişletme örneği ve ayrı dosyalarda kapalı örnek beceriler hazırlanır.

## Doğrulama

- YAML şeması: eksik/yanlış alanlar, bilinmeyen kimlik, yinelenen kimlik, hatalı ifade ve iç içe akış sınırları.
- Yürütme: tetikleyici indeksi, AND/OR/NOT, eksik olay verisi, hedef seçimi, şans sınırları, cooldown yarışları ve gecikme/sıra.
- Lifecycle: hatalı reload'da eski görüntünün kalması, başarılı reload'da eski görevlerin iptali, logout/disable temizliği, extension sahipliği.
- Gerçek kod yollarını kullanan kontrollü saat, rastgelelik ve zamanlayıcı ile testler; bölgeye bağlı işlemlerin doğru zamanlayıcıdan geçmesi kontrol edilir.
- Yeni motorun testleri ve mevcut bütün testler, modül derlemeleri/NMS kontrolleri ve son JAR'ın public API + skills uygulaması + örnek kaynakları içermesi doğrulanır.
- Canlı Paper/Folia sunucusu çalıştırılmazsa bu sınır teslimatta açıkça belirtilir.

## Uygulayıcıya bırakılan kararlar

Sınıfların kesin dosya bölünmesi, API imzaları, factory argüman erişim yardımcıları ve test doubles mevcut Java API/Kotlin backend geleneğine göre uygulama planında kesinleştirilir. Üçüncü taraf oyun eklentilerine özel efektler, periyodik/pasif modifier motoru, XP/seviye/ağaç, GUI editörü, veritabanı ve birebir libreforge uyumluluğu bu teslimatın parçası değildir.
