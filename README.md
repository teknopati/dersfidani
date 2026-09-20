# DersFidan 9.9 — Android Studio projesi

## 9.9 gerçek cihaz düzenlemeleri

- Ana çiftlik menüsü artık kaydırılmaz; yedi bölüm ekranın kullanılabilir alanına sabit olarak yerleşir.
- Çiftlik temalı alt menü yalnızca Çiftliğim sekmesinde kullanılır; diğer sekmeler kendi uygulama temasını korur.
- Market ürünleri sabit yükseklikte kompakt kartlara dönüştürüldü; görsel, açıklama, fiyat ve satın alma düğmesi birlikte görünür.
- Hayvan ekranındaki genel kulübe kaldırıldı; boş ve temiz hayvan çayırı kullanıldı. Konuşma, bakım ve yükseltme alanlarındaki taşmalar azaltıldı.
- Ağaçların Tohum, Filiz ve Küçük Fidan aşamaları artık yetişkin ağaç resmi göstermez; sonraki aşamalarda ağaç kademeli büyür.
- Eski Çiftçi 1–5 adları Ayşe, Mehmet, Emre, Fatma ve Ece olarak güncellenir; özel verilmiş kullanıcı adları korunur.
- Koleksiyon kartlarında kilit görünümü, görsel kırpma ve alt boşluklar düzenlendi.

DersFidan; 297 günlük, 6594 bloklu çalışma programını görev, puan ve gerçek zamanlı
resimli 2B çiftlik ilerlemesiyle birleştiren Kotlin/Jetpack Compose uygulamasıdır.

## 9.8 özel oyun arayüzü

- Standart Material kart görünümü çiftlik bölümünden kaldırıldı. Hayvanlar, Yapılar, Ağaçlar,
  Çalışanlar, İşçi işe al, Kartlarım ve Market aynı yapraklı ahşap/parşömen oyun diliyle yenilendi.
- Hayvanlar artık dikdörtgen fotoğraf çerçevesinde değil, doğrudan çiftlik sahnesinde görünür.
- Bütün hayvanlarda ortak patili yaş rozeti kullanılır; içindeki yaş bilgisi canlı olarak değişir.
- İsim tabelası, yön düğmeleri, konuşma balonu, habitat tabelası ve geliştirme paneli özel çizimdir.
- Yapılar, meyve bahçesi, çalışanlar, açık hava Market'i ve Kartlarım için birbirinden farklı,
  yüksek çözünürlüklü çiftlik arka planları eklendi.
- Açık ve karanlık telefon temasında çiftlik, koyu Material katmanı kullanmadan kendi sıcak
  oyun renklerini korur.

## Android Studio'da APK oluşturma

1. ZIP'i çıkarın ve içindeki `DersFidan_v9_Tam` klasörünü Android Studio ile açın.
2. Gradle senkronizasyonunun tamamlanmasını bekleyin (JDK 17 kullanın).
3. `app/google-services.json` dosyasının doğru Firebase projesine ait olduğunu kontrol edin.
4. `Build > Build Bundle(s) / APK(s) > Build APK(s)` seçeneğini kullanın.

Uygulama Android 8.0+ cihaz ister. İlk Gradle eşitlemesi internet bağlantısı gerektirir.
Bu ortam Gradle 8.7 dağıtımını indiremediği için son Android derlemesi burada çalıştırılamadı.
APK pakete eklenmemiştir; APK'yı Android Studio üzerinden siz oluşturabilirsiniz.

## 9.7 çiftlik görsel yenilemesi

- Çiftlik ana ekranı tam ekran, yüksek çözünürlüklü vadi illüstrasyonuna geçirildi. Eski düz
  zemin ve beyaz belge görünümü kaldırıldı; menüler ahşap oyun kartları olarak sahnenin altına yerleşir.
- Katalogdaki 34 hayvan türünün tamamı şeffaf arka planlı, yüksek çözünürlüklü ve birbirinden
  farklı yüz/siluetlere sahip 2.5B karakter çizimleriyle yenilendi.
- Hayvan görselleri artık kutuya kırpılmadan `Fit` ölçeğiyle gösterilir. Aynı türün sonraki
  üyelerinde yön, duruş ve ölçek küçük farklılıklar gösterir; adları ve gelişimleri ayrı tutulur.
- Hayvana dokunma veya yem verme sırasında zıplama/yalpalama tepkisi; çiftçiye dokunulduğunda
  el sallama pozu eklendi. Ekrandan ayrılınca hayvan sesi durdurulur.
- Hayvan sahnesine habitat görsel katmanı, ahşap isim levhası, yaş rozeti, konuşma alanı,
  besleme ve kısmi puan yatırma kontrolleri birlikte yerleştirildi.
- Açık ve koyu temada çiftlik yazılarının kontrastı ayrı paletlerle korunur.

## 9.2 hayvan kartları ve çalışan akışı

- Habitatlar ana menüden kaldırıldı. Her habitat artık ilgili hayvan kartının içindeki ev
  bölümünden görüntülenir ve genişletilir.
- Hayvan galerisi kuş bakışı harita yerine önden bakışlı tek kart/vitrin düzenine geçirildi.
  Seçili hayvan büyük bir kartta görünür; aynı türün 2–5. hayvanlarının görünüşleri birbirinden farklıdır.
- Kartta hayvanın adı, türü, yaşı, unvanı ve sonraki yaşa kalan puan gösterilir. Oklar, kaydırma
  ve habitat sakini düğmeleriyle hayvanlar arasında geçiş yapılır.
- Ana menüye `İşçi işe al` eklendi. Yeni işçi yalnız bu bölümden alınır; çalışan evi ve ev
  yükseltmeleri `Çalışanlar` bölümünde kalır.

## 9.1 arayüz düzeltmesi

- Ana çiftlik ekranındaki eski SceneView zemini tamamen kaldırıldı. Böylece yeşil yüzeyin
  Compose menülerinin önüne çıkmasına neden olan Android SurfaceView katman sorunu giderildi.
- Ana arka plan tek parça, hava ve saate uyum sağlayan çiftlik illüstrasyonu olarak yeniden çizildi.
- Üst çubuk, menü kartları, galeriler, Market ve çiftlik diyalogları açık krem/adaçayı renklerine;
  metinler yüksek kontrastlı koyu kahverengi ve yeşil tonlarına geçirildi.
- SceneView ve bütün eski model bağımlılıkları kaldırıldı; çiftlik ekranları hafif 2B resimler kullanır.

## 9.0 yenilikleri

- Sabit rutinler 1 puan verir. Diğer günlük bloklar artık `ceil(süreDk × 0.20)` puan verir.
  Ders/deneme kartının tek seferlik genel ödülü %10, gün sonu bonusu +15 olarak kalır.
- Her görev tamamlandığında `onayla.mp3`, gün bütünüyle bittiğinde `alkis.mp3` çalar.
  Eski konfeti sesi ve parçacık animasyonu kaldırılmıştır.
- Çiftlik, kuş bakışı harita yerine tam ekran resimli bir yönetim merkezidir. Hayvanlar,
  Yapılar, Ağaçlar, Çalışanlar, Market ve İşçi işe al büyük görsel menüler halinde açılır.
- Listelerde oklarla veya sağa/sola kaydırarak gezinilir. Seçilen nesne büyük resimli kartta görünür.
- Çiftlik başlığına dokunarak çiftliğin adı değiştirilebilir. Hayvan ve çalışan konuşmaları
  çiftlik adını kullanır.
- Her hayvan türü ayrı habitata sahiptir. İlk hayvan habitatıyla birlikte gelir; aynı türün
  2., 3., 4. ve 5. hayvanları sırasıyla habitat seviyesi 2, 4, 6 ve 8 ister. Tür başına sınır 5'tir.
- Hayvanlar ayrı ayrı görüntülenir, adlandırılır ve yeniden adlandırılır. Yaş sınırı 20'dir;
  yaş ilerletme maliyeti 2. yaş için 20 puandan başlar ve her yaşta 15 artar.
- Habitat ve yapılar için yükseltme puanı tek seferde ödenmek zorunda değildir. Kullanıcı
  istediği kadar puan yatırabilir; ilerleme Room ve bulut yedeğinde korunur.
- Habitatlar en fazla 10 seviyedir. Çalışan evi en fazla 5 seviyedir ve her seviye bir çalışan
  kapasitesi sağlar.
- Beş farklı yüz, kıyafet ve ifadeye sahip çalışan resmi vardır. Çalışanlar 40 puanla alınır; seviye maliyetleri
  80, 120, 160 ... 400 olarak devam eder ve en fazla 10 seviyeye çıkar.
- Hayvan sesi olmayan türlerde `Merhaba de` yalnız konuşma balonunu değiştirir.
- Ağaçların büyüme ve puan yatırma döngüsü korunmuştur; 100 adlandırılmış tür resimli galeride yönetilir.
- Hava çiftlik açıkken yenilenir ve resimli arka planı etkiler; internet yoksa son bilinen durum kalır.
- Hesap ayrımı, Firestore/Storage yedeği, 297 günlük Geçmiş, geri tuşu sırası ve aynı sekmeye
  yeniden basınca başa dönme davranışı korunmuştur.

## Firebase

Authentication > Email/Password, Firestore ve Storage açık olmalıdır. Veriler kullanıcı UID'si
altında tutulur. Kullanıcı adı arka planda `kullaniciadi@dersfidan.app` biçimine çevrildiği için
e-posta ile “şifremi unuttum” akışı yoktur.

Firebase Console'da kök dizindeki `firestore.rules` ve `storage.rules` kurallarını yayımlayın.
Bu kurallar her hesabın yalnız kendi UID klasörünü okuyup yazmasına izin verir.

## Önemli dosyalar

- `CiftlikScreen.kt`: yeni çiftlik merkezi ve bütün galeri/detay ekranları.
- `FarmArt.kt`: hayvan, yapı, ağaç ve çalışan resimlerini karta bağlayan hafif görsel katman.
- `Repository.kt`: puan, satın alma, kısmi yatırım, yaş ve seviye kuralları.
- `AppDatabase.kt`: Room 8 şeması ve ilerlemeyi koruyan migration'lar.
- `Bulut.kt`: Authentication, Firestore ve Storage yedekleme.
- `SesYoneticisi.kt`: uygulama, hayvan, alkış ve onay sesleri.
