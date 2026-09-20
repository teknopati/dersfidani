package com.dersfidan.app.data

object SubjectUtil {

    /** Rutin bloklar - "ders" sayılmaz, fidan puanına ve ders paneline dahil edilmez. */
    val rutinKategoriler = setOf(
        "Mola", "Yemek/Mola", "Uyanma", "Uyanma/Hazırlık", "Uyku", "Şekerleme",
        "Dinlenme", "Serbest Zaman", "Canlı Ders", "MOLA GÜNÜ", "TATİL", "Yürüyüş/Hareket"
    )

    /** Kategori adı doğrudan ders adı olan bloklar. */
    val dogrudanDersKategorileri = setOf(
        "Sözel Yetenek", "Matematik", "Eğitim Bilimleri", "Tarih", "Coğrafya", "Mevzuat"
    )

    /** Serbest metin içinde aranacak ders anahtar kelimeleri, öncelik sırasıyla. */
    val anahtarKelimeler = listOf(
        "Eğitim Bilimleri", "Sözel Yetenek", "Coğrafya", "Matematik", "Mevzuat", "Tarih", "ÖABT", "AGS"
    )

    /**
     * Yeni eklenen/düzenlenen bir blok için ders adını tahmin eder.
     * Kullanıcı elle ders adı girmezse bu kullanılır.
     */
    fun tahminEt(kategori: String, metin: String): String? {
        if (kategori in rutinKategoriler) return null
        if (kategori in dogrudanDersKategorileri) return kategori
        for (kw in anahtarKelimeler) {
            if (metin.contains(kw)) return kw
        }
        return kategori.ifBlank { null }
    }

    /** Kullanıcının elle blok eklerken seçebileceği önerilen kategori listesi. */
    val onerilenKategoriler = listOf(
        "Sözel Yetenek", "Matematik", "Eğitim Bilimleri", "Tarih", "Coğrafya", "Mevzuat",
        "Deneme", "Deneme Analizi", "Hata Defteri", "Genel Tekrar", "Konu Tekrarı/Soru Çözümü",
        "Soru Çözümü", "Test/Soru Çözümü", "Çıkmış Sorular", "Okuma/Tekrar",
        "Mola", "Yemek/Mola", "Uyanma", "Uyku", "Şekerleme", "Dinlenme", "Serbest Zaman",
        "Canlı Ders", "Yürüyüş/Hareket", "MOLA GÜNÜ", "TATİL"
    )
}
