# DersFidan 9.9 test listesi

## v9.9 gerçek cihaz kontrolü

- Ana çiftlikte ekranı kaydırmadan Hayvanlarım, Yapılarım, Ağaçlarım, Çalışanlar, Market, İşçi işe al ve Kartlarım birlikte görünür.
- Çiftlik alt menüsündeki beş etiket birbirine girmez; Derslerim veya Profilim'e geçince normal uygulama alt menüsü görünür.
- Marketin üç sekmesinde ürün kartları yaklaşık aynı yüksekliktedir; hiçbir kart boş dev parşömen olarak görünmez.
- Hayvan ekranında konuşma balonu yüzü kapatmaz, Yükselt yazısı kesilmez ve besleme mesajı çiftlik temalı görünür.
- 0–49 puanlı ağaçlarda tohum/filiz/fidan görünümü, daha ileri aşamalarda büyüyen ağaç görünümü kullanılır.
- Eski genel çiftçi adları Ayşe, Mehmet, Emre, Fatma ve Ece'ye dönüşür; elle değiştirilmiş ad değişmez.

## v9.8 çiftlik oyun arayüzü

- Çiftliğim > Hayvanlarım: ad tabelası, ortak pati yaş rozeti, ahşap oklar, konuşma balonu ve yükseltme paneli görünür.
- Sağa/sola kaydırma ve ahşap oklar farklı hayvanlara geçirir; her hayvanın adı ve yaşı doğru güncellenir.
- Merhaba de varsa hayvan sesini çalar ve sözü değiştirir; sayfadan çıkınca hayvan sesi kesilir.
- Besle ve puan yatırma işlemlerinde küçük hareket oynar; puan/yem ve ilerleme değerleri güncellenir.
- Habitat tabelası parşömen penceresini açar; genişletme puanı kısmi yatırılabilir.
- Yapılarım, Ağaçlarım, Çalışanlar, İşçi işe al, Kartlarım ve Market aynı ahşap/parşömen/yaprak tasarım dilindedir.
- Aydınlık ve karanlık cihaz temasında çiftlik yazıları açık zemin üzerinde koyu ve okunaklı kalır.
- Çiftlik sekmesinde alt gezinme ahşap oyun temasına geçer; diğer ana sekmeler kendi uygulama temasini korur.

## Derleme

- Android Studio'da JDK 17 seçin ve Gradle Sync çalıştırın.
- Firebase Authentication'da Email/Password, Firestore ve Storage açık olmalı.
- Debug APK için `Build > Build APK(s)` kullanın.

## Hızlı kabul testi

1. Yeni kullanıcı adıyla kayıt olun; Bugün ekranının yalnızca bugünü gösterdiğini doğrulayın.
2. 25 dakikalık normal bir bloğu tamamlayın: toplam puan 3 artmalı.
3. Mola/Uyku gibi sabit bloğu tamamlayın: toplam puan 1 artmalı.
4. Bir günün bütün bloklarını bitirin: +15 yalnızca bir kez verilmeli.
5. Bir ders/deneme kartını tamamen bitirin: kart süresinin %10'u yalnızca bir kez verilmeli.
6. Çıkış yapıp başka hesapla girin: ilk hesabın görevleri, çiftliği, notları ve profili görünmemeli.
7. İlk hesapla tekrar girin: Firestore/Storage yedeği geri gelmeli.
8. Geçmiş'te 297 gün bulunduğunu, Tatil/Mola günü etiketlerini ve ayrı Gün Ekranı'nı kontrol edin.
9. Gelecek gün görevini tamamlamayı deneyin: engellenmeli.
10. Her alt sekmeye ikinci kez basın: liste başa, çiftlik kamerası başlangıca dönmeli.
11. Çiftlik ana ekranındaki altı görselli kartın (Hayvanlar, Yapılar, Ağaçlar, Çalışanlar, Market, İşçi İşe Al) eksiksiz göründüğünü doğrulayın.
12. Şehri değiştirin: hava anında yenilenmeli; açık/bulut/yağmur/fırtına/kar/sis çiftlik arka planına yansımalı.
13. Market > Yapılar'ın başındaki Yeni Ağaç'tan ağaç alın; adının ve resminin değiştiğini doğrulayın.
14. Kamera ve galeriden profil/not fotoğrafı ekleyin; başka cihaz/kurulumda geri geldiğini doğrulayın.
15. Telefonun açık ve koyu temasında bütün yazıların okunabildiğini kontrol edin.
16. Marketten ilk kediyi 50 puana alın ve isim verin. İkinci fiyatın 63, üçüncünün 79 olduğunu doğrulayın.
17. Aynı hayvan türünden 5 tane alın; altıncının `5/5` nedeniyle alınamadığını doğrulayın.
18. Hayvan kartında seçili hayvanın büyük resmini; kart üstünde `Yaş`, üst kısımda `Market` kısayolunu doğrulayın.
19. Aynı tür hayvanların adlarının kart altında göründüğünü; habitatın yalnız ad, seviye, kapasite ve `Habitatı genişlet` akışıyla sunulduğunu doğrulayın.
20. Hava durumuna göre gökyüzü ve sahne katmanlarını kontrol edin: açık, bulutlu, yağmurlu, fırtınalı, karlı ve sisli.
21. Yapı ve ağaç galerilerinde resmin, seviye/aşama etiketi ve Market kısayoluyla gösterildiğini doğrulayın.
22. Çalışan kartında Market yerine `İşçi Al` kısayolunun açıldığını ve çalışan evi yükseltmelerini doğrulayın.
23. Çiftliğe girince müziğin başladığını, başka sekmeye geçince durduğunu kontrol edin.
24. Satın alma/yükseltme, kutlama, genel düğme, habitat ve bildirim seslerini ayrı ayrı test edin.
25. Telefon temasını açık/koyu değiştirin; çiftlik kartları, Market, iletişim kutuları ve yazıların iki temada da rahat okunduğunu doğrulayın.
26. Kuş, arı, kelebek, uğur böceği, fare ve civciv gibi küçük hayvanların kartta büyük ve görünür olduğunu doğrulayın.
