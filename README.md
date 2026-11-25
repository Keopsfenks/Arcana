# Arcana

[![Build](https://github.com/Keopsfenks/Arcana/actions/workflows/build.yml/badge.svg)](https://github.com/Keopsfenks/Arcana/actions/workflows/build.yml)

Arcana, **Semih Gürbüz (Keopsfenks)** tarafından sürdürülen bir Minecraft eklenti geliştirme çatısıdır. Java API, Kotlin uzantıları, yapılandırma, veri depolama, komutlar, menüler, özel eşyalar, tarifler, zamanlayıcılar ve sürüme özel sunucu entegrasyonları içerir.

- Eklenti adı: `Arcana`
- Paket: `com.keopsfenks.arcana`
- API: `com.keopsfenks:arcana-api`
- Gelişmiş YAML beceri motoru ve bileşen kataloğu: ayrı modüller, aynı kurulum JAR'ı
- Ana sınıf: `com.keopsfenks.arcana.internal.spigot.ArcanaImpl`
- Geliştirici: [Keopsfenks](https://github.com/Keopsfenks)
- [Belgeler](docs/wiki/home.md) · [Sürümler](https://github.com/Keopsfenks/Arcana/releases) · [Hata bildir](https://github.com/Keopsfenks/Arcana/issues)

## Derleme ve kurulum

Derleme için JDK 25, Git ve internet bağlantısı gerekir. Gradle Wrapper, Java 21 gerektiren modüller için uygun araç zincirini otomatik bulur veya indirir.

```powershell
git clone https://github.com/Keopsfenks/Arcana.git
cd Arcana
.\gradlew.bat build
```

Linux/macOS: `./gradlew build`.

Sunucuya `build/libs/arcana-<sürüm>-all.jar` dosyasını kur. Normal JAR bağımlılıkları içermez. Sunucu API tabanı 1.21.8'dir; sürüme özel modüller `settings.gradle.kts` içinde listelenir. Hedef sunucu sürümünü kullandığın eklentilerle birlikte doğrula.

## API kullanımı

`arcana-api` ortak Java sözleşmelerini, `arcana-core` sunucu uygulamasını, `arcana-skills` beceri motorunu ve `arcana-skills-components` yerleşik bileşen kataloğunu barındırır. Efektler, koşullar, hedefleyiciler ve tetikleyiciler ayrı sınıflardır. Oyuncu ve mob aktörleri; aura, mermi, kaynak maliyeti ve sahipli yaşam döngüsü aynı motoru kullanır. [YAML kılavuzu](docs/wiki/skills.md), [Java API](docs/wiki/skills-api.md) ve [125 bileşenlik katalog](docs/wiki/skills-components.md) yapılandırmayı ve genişletmeyi anlatır.

Önce `.\gradlew.bat :arcana-api:publishToMavenLocal` komutunu çalıştır; ardından kendi eklentinde:

```kotlin
repositories {
    mavenLocal()
}

dependencies {
    compileOnly("com.keopsfenks:arcana-api:2026.47")
}
```

`plugin.yml`:

```yaml
depend: [Arcana]
```

Eklenti tabanı `com.keopsfenks.arcana.core.ArcanaPlugin` sınıfıdır. Çatı seçenekleri `arcana.yml` dosyasından okunur; API sürümü anahtarı `arcana-api-version` olarak adlandırılır.

## Mevcut projelerden geçiş

Bu fork kendi paket ve API kimliğini kullanır. Eski paketlere karşı derlenmiş eklentiler otomatik uyumlu değildir. Tüketici eklentilerini Arcana API'sine göre yeniden derle; bağımlılık, import, sınıf, yapılandırma ve ad alanlarını da güncelle. Veri dizini `plugins/Arcana`, çatı komutu `/arcana`, izin öneki `arcana` olarak kullanılır. Mevcut kalıcı veri anahtarları ve SQL tablo adları otomatik taşınmaz.

Üçüncü taraf ürünlerin gerçek adları ve entegrasyon kimlikleri korunur. Arcana'nın Polymart ve bStats kimlikleri `0` olduğundan özgün projeye ait güncelleme veya istatistik kayıtları kullanılmaz.

## Yayınlama

`publishArcana` görevi API ve eklentiyi bu deponun GitHub Packages alanına yayımlar. Yerel yayınlama için `GITHUB_ACTOR` ve paket yazma yetkili `GITHUB_TOKEN` gerekir. GitHub Release yayımlandığında iş akışı sürüm etiketini kullanır, paketleri yayımlar ve kurulum JAR'ını sürüme ekler.

## Lisans

[MIT](LICENSE.md). Kaynak projenin ve katkıda bulunanların bilgileri [NOTICE.md](NOTICE.md) içinde korunur.
