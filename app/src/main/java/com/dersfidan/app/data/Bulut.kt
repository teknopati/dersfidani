package com.dersfidan.app.data

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.Locale
import java.io.File

/** Google Play Services'ın Task<T> tipini coroutine dünyasına taşıyan küçük köprü. */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { sonuc -> cont.resume(sonuc) }
    addOnFailureListener { hata -> cont.resumeWithException(hata) }
}

/**
 * E-posta/şifre ile hesap sistemi. Amaç: kullanıcı uygulamayı silse veya telefon
 * değiştirse bile, aynı e-posta/şifreyle giriş yapınca ilerlemesinin geri gelmesi.
 */
object BulutHesap {
    private val auth by lazy { FirebaseAuth.getInstance() }

    fun girisliMi(): Boolean = auth.currentUser != null
    fun uid(): String? = auth.currentUser?.uid
    fun mevcutEposta(): String? = auth.currentUser?.email

    fun epostayaCevir(kullaniciAdi: String): String {
        val giris = kullaniciAdi.trim().lowercase(Locale.ROOT)
            .replace('ç', 'c').replace('ğ', 'g').replace('ı', 'i')
            .replace('ö', 'o').replace('ş', 's').replace('ü', 'u')
        return if ('@' in giris) giris else "${giris.replace(Regex("[^a-z0-9._-]"), "_")}@dersfidan.app"
    }

    suspend fun kayitOl(kullaniciAdi: String, sifre: String) {
        auth.createUserWithEmailAndPassword(epostayaCevir(kullaniciAdi), sifre).await()
    }

    suspend fun girisYap(kullaniciAdi: String, sifre: String) {
        auth.signInWithEmailAndPassword(epostayaCevir(kullaniciAdi), sifre).await()
    }

    fun cikisYap() {
        auth.signOut()
    }
}

/** Firestore'dan geri yüklenen profil bilgisi (varsa isim/avatar'ı yerel profile uygulamak için). */
data class BulutProfil(val isim: String?, val avatar: String?, val profilFotoUrl: String? = null)

/**
 * İlerlemenin `kullanicilar/{uid}` dokümanındaki tam yedeği.
 *
 * ÖNEMLİ TASARIM NOTU: Blok tamamlama durumu Room'un otomatik (cihaza özel) id'siyle DEĞİL,
 * "tarih__saatBaslangic__kategori" gibi SABİT bir anahtarla saklanır. Program her cihazda aynı
 * seed dosyasından yüklendiği için bu anahtar cihazlar arasında hep aynıdır — böylece uygulama
 * silinip tekrar kurulsa da hangi bloğun tamamlandığı doğru şekilde eşleşir.
 *
 * Basit tutmak için senkronizasyon "son yazan kazanır" mantığıyla çalışır: aynı hesapla aynı anda
 * iki cihazdan senkron bir çakışma-çözümü YAPILMAZ (bu bir okul projesi için yeterlidir). Fotoğraflı
 * ders notları da bulutta sadece metin olarak saklanır; fotoğrafın kendisi (cihaza özel dosya yolu
 * olduğu için) yedeklenmez.
 */
object BulutSenkron {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    private fun blokAnahtari(tarih: String, saatBaslangic: String, kategori: String) =
        "${tarih}__${saatBaslangic}__${kategori}"

    /** Cihazdaki TÜM ilerlemeyi buluta yükler (yazar/üzerine yazar). Girişli değilse sessizce çıkar. */
    suspend fun yukle(repo: Repository, isim: String, avatar: String, profilFotoUrl: String? = null) {
        val uid = BulutHesap.uid() ?: return
        val tamamlanan = repo.blokDao.tamamlananBloklarListe()
        val canlilar = repo.canliDao.tumCanlilarListe()
        val agaclar = repo.agacDao.tumAgaclarListe()
        val bonuslar = repo.gunBonusuDao.tumBonuslarListe()
        val oduller = repo.odulBonusuDao.tumBonuslarListe()
        val notlar = repo.notuDao.tumNotlarListe().map { not ->
            if (not.bulutFotoUrl == null && not.fotoYolu != null) {
                val uri = if (not.fotoYolu.startsWith("content:") || not.fotoYolu.startsWith("file:")) Uri.parse(not.fotoYolu) else Uri.fromFile(File(not.fotoYolu))
                val url = notFotografiYukle(uri)
                if (url != null) { repo.notuDao.fotoUrlGuncelle(not.id, url); not.copy(bulutFotoUrl = url) } else not
            } else not
        }
        val ciftlik = repo.ciftlikDurumuDao.getir() ?: CiftlikDurumu()
        val calisanlar = repo.ciftlikCalisaniDao.liste()

        val veri = hashMapOf(
            "isim" to isim,
            "avatar" to avatar,
            "profilFotoUrl" to profilFotoUrl,
            "guncellemeZamani" to System.currentTimeMillis(),
            "bloklarTamam" to tamamlanan.associate {
                blokAnahtari(it.tarih, it.saatBaslangic, it.kategori) to (it.tamamlanmaZamani ?: System.currentTimeMillis())
            },
            "canlilar" to canlilar.map {
                mapOf(
                    "esyaId" to it.esyaId, "ad" to it.ad,
                    "satinAlmaFiyati" to it.satinAlmaFiyati,
                    "yas" to it.yas, "yasHarcananPuan" to it.yasHarcananPuan,
                    "yasIlerlemePuan" to it.yasIlerlemePuan,
                    "seviye" to it.seviye, "seviyeHarcananPuan" to it.seviyeHarcananPuan,
                    "seviyeIlerlemePuan" to it.seviyeIlerlemePuan,
                    "konumX" to it.konumX, "konumZ" to it.konumZ,
                    "tokluk" to it.tokluk, "sevgi" to it.sevgi,
                    "sonBeslenmeZamani" to it.sonBeslenmeZamani,
                    "sonSevilmeZamani" to it.sonSevilmeZamani,
                    "satinAlmaZamani" to it.satinAlmaZamani
                )
            },
            "agaclar" to agaclar.map {
                mapOf(
                    "isim" to it.isim, "yatirilanPuan" to it.yatirilanPuan,
                    "satinAlmaFiyati" to it.satinAlmaFiyati, "turId" to it.turId,
                    "konumX" to it.konumX, "konumZ" to it.konumZ,
                    "olusturmaZamani" to it.olusturmaZamani
                )
            },
            "gunBonuslari" to bonuslar.map { mapOf("tarih" to it.tarih, "puan" to it.puan, "zaman" to it.zaman) },
            "odulBonuslari" to oduller.map { mapOf("kartTuru" to it.kartTuru, "kartAdi" to it.kartAdi, "puan" to it.puan, "zaman" to it.zaman) },
            "notlar" to notlar.map {
                mapOf("dersAdi" to it.dersAdi, "metin" to it.metin, "bulutFotoUrl" to it.bulutFotoUrl, "olusturmaZamani" to it.olusturmaZamani)
            },
            "ciftlik" to mapOf("ad" to ciftlik.ad, "calisanEviSeviye" to ciftlik.calisanEviSeviye,
                "calisanEviIlerlemePuan" to ciftlik.calisanEviIlerlemePuan, "calisanEviHarcananPuan" to ciftlik.calisanEviHarcananPuan,
                "yemAdedi" to ciftlik.yemAdedi, "yemeHarcananPuan" to ciftlik.yemeHarcananPuan),
            "calisanlar" to calisanlar.map { mapOf("tip" to it.tip, "ad" to it.ad, "rol" to it.rol,
                "seviye" to it.seviye, "ilerlemePuan" to it.ilerlemePuan, "harcananPuan" to it.harcananPuan,
                "alinmaZamani" to it.alinmaZamani) }
        )
        try {
            db.collection("kullanicilar").document(uid).set(veri).await()
        } catch (e: Exception) {
            // Bağlantı yoksa sessizce vazgeç: ilerleme zaten cihazda güvende, bir sonraki
            // değişiklikte tekrar denenecek.
        }
    }

    /**
     * Girişten hemen sonra çağrılır. Bu cihaz "taze" (henüz hiç ilerleme yapılmamış) ise
     * buluttaki en son yedeği bu cihaza uygular ve profil bilgisini döner. Cihazda zaten
     * ilerleme varsa (aynı hesapla aynı cihazda tekrar giriş gibi) dokunmadan null döner.
     */
    suspend fun indirVeUygula(repo: Repository): BulutProfil? {
        val uid = BulutHesap.uid() ?: return null
        val snap = try {
            db.collection("kullanicilar").document(uid).get().await()
        } catch (e: Exception) {
            // "Yedek yok" ile "sunucuya ulaşılamadı" aynı şey değildir. Hata yukarı
            // taşınırsa giriş ekranı yerel boş veriyi yanlışlıkla bulutun üstüne yazmaz.
            throw IllegalStateException("Bulut yedeğine ulaşılamadı. İnternet bağlantısını kontrol et.", e)
        }
        if (!snap.exists()) return null

        // Blok tamamlama durumunu uygula
        @Suppress("UNCHECKED_CAST")
        val bloklarTamam = snap.get("bloklarTamam") as? Map<String, Any?> ?: emptyMap()
        for ((anahtar, zamanRaw) in bloklarTamam) {
            val parcalar = anahtar.split("__", limit = 3)
            if (parcalar.size != 3) continue
            val zaman = (zamanRaw as? Number)?.toLong() ?: System.currentTimeMillis()
            repo.blokDao.isaretleAnahtarIle(parcalar[0], parcalar[1], parcalar[2], zaman)
        }

        // Ağaçlar: taze cihazdaki tek (ücretsiz) ağacı sil, buluttakileri aynen ekle.
        @Suppress("UNCHECKED_CAST")
        val agaclar = snap.get("agaclar") as? List<Map<String, Any?>> ?: emptyList()
        if (agaclar.isNotEmpty()) {
            repo.agacDao.hepsiniSil()
            agaclar.forEachIndexed { index, a ->
                val varsayilanX = -11f + (index % 7) * 3.6f
                val varsayilanZ = 9f - ((index / 7) % 3) * 3.5f
                repo.agacDao.ekle(
                    Agac(
                        isim = a["isim"] as? String ?: "Ağaç",
                        yatirilanPuan = (a["yatirilanPuan"] as? Number)?.toInt() ?: 0,
                        satinAlmaFiyati = (a["satinAlmaFiyati"] as? Number)?.toInt() ?: 0,
                        turId = (a["turId"] as? Number)?.toInt() ?: 0,
                        konumX = (a["konumX"] as? Number)?.toFloat() ?: varsayilanX,
                        konumZ = (a["konumZ"] as? Number)?.toFloat() ?: varsayilanZ,
                        olusturmaZamani = (a["olusturmaZamani"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                )
            }
        }

        @Suppress("UNCHECKED_CAST")
        val canlilar = snap.get("canlilar") as? List<Map<String, Any?>> ?: emptyList()
        val turSirasi = mutableMapOf<String, Int>()
        val habitatKonumlari = mutableMapOf<String, Pair<Float, Float>>()
        var habitatIndex = 0
        var yapiIndex = 0
        canlilar.forEach { c ->
            val hamId = c["esyaId"] as? String ?: return@forEach
            val esyaId = if (hamId == "kurbağa") "kurbaga" else hamId
            val esya = MarketKatalog.bul(esyaId) ?: return@forEach
            val sira = turSirasi.getOrDefault(esyaId, 0)
            val varsayilanKonum = if (esya.tur == EsyaTuru.HAYVAN) {
                habitatKonumlari.getOrPut(esyaId) {
                    val i = habitatIndex++
                    (-10f + (i % 6) * 3.5f) to (-5.5f + ((i / 6) % 4) * 3.2f)
                }
            } else {
                val i = yapiIndex++
                (-10f + (i % 6) * 4f) to (5.5f - ((i / 6) % 3) * 4f)
            }
            repo.canliDao.ekle(
                Canli(
                    esyaId = esyaId,
                    ad = c["ad"] as? String
                        ?: if (esya.tur == EsyaTuru.HAYVAN) "${esya.isim} ${sira + 1}" else esya.isim,
                    satinAlmaFiyati = (c["satinAlmaFiyati"] as? Number)?.toInt()
                        ?: MarketKatalog.guncelFiyat(esya, sira),
                    yas = (c["yas"] as? Number)?.toInt()?.coerceIn(1, Repository.MAKS_HAYVAN_YASI) ?: 1,
                    yasHarcananPuan = (c["yasHarcananPuan"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                    yasIlerlemePuan = (c["yasIlerlemePuan"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                    seviye = (c["seviye"] as? Number)?.toInt()?.coerceIn(1, Repository.MAKS_YAPI_SEVIYESI) ?: 1,
                    seviyeHarcananPuan = (c["seviyeHarcananPuan"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                    seviyeIlerlemePuan = (c["seviyeIlerlemePuan"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                    konumX = (c["konumX"] as? Number)?.toFloat() ?: varsayilanKonum.first,
                    konumZ = (c["konumZ"] as? Number)?.toFloat() ?: varsayilanKonum.second,
                    tokluk = (c["tokluk"] as? Number)?.toInt()?.coerceIn(0, 100) ?: 70,
                    sevgi = (c["sevgi"] as? Number)?.toInt()?.coerceIn(0, 100) ?: 60,
                    sonBeslenmeZamani = (c["sonBeslenmeZamani"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    sonSevilmeZamani = (c["sonSevilmeZamani"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    satinAlmaZamani = (c["satinAlmaZamani"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            )
            turSirasi[esyaId] = sira + 1
        }

        @Suppress("UNCHECKED_CAST")
        val bonuslar = snap.get("gunBonuslari") as? List<Map<String, Any?>> ?: emptyList()
        bonuslar.forEach { b ->
            repo.gunBonusuDao.ekle(
                GunBonusu(
                    tarih = b["tarih"] as? String ?: return@forEach,
                    puan = (b["puan"] as? Number)?.toInt() ?: 0,
                    zaman = (b["zaman"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            )
        }

        @Suppress("UNCHECKED_CAST")
        val oduller = snap.get("odulBonuslari") as? List<Map<String, Any?>> ?: emptyList()
        oduller.forEach { o ->
            repo.odulBonusuDao.ekle(
                OdulBonusu(
                    kartTuru = o["kartTuru"] as? String ?: return@forEach,
                    kartAdi = o["kartAdi"] as? String ?: return@forEach,
                    puan = (o["puan"] as? Number)?.toInt() ?: 0,
                    zaman = (o["zaman"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            )
        }

        @Suppress("UNCHECKED_CAST")
        val notlar = snap.get("notlar") as? List<Map<String, Any?>> ?: emptyList()
        notlar.forEach { n ->
            repo.notuDao.ekle(
                DersNotu(
                    dersAdi = n["dersAdi"] as? String ?: return@forEach,
                    metin = n["metin"] as? String ?: "",
                    fotoYolu = null,
                    bulutFotoUrl = n["bulutFotoUrl"] as? String,
                    olusturmaZamani = (n["olusturmaZamani"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            )
        }

        @Suppress("UNCHECKED_CAST")
        val ciftlik = snap.get("ciftlik") as? Map<String, Any?>
        if (ciftlik != null) repo.ciftlikDurumuDao.kaydet(
            CiftlikDurumu(
                ad = ciftlik["ad"] as? String ?: "Çiftliğim",
                calisanEviSeviye = (ciftlik["calisanEviSeviye"] as? Number)?.toInt()?.coerceIn(1, 5) ?: 1,
                calisanEviIlerlemePuan = (ciftlik["calisanEviIlerlemePuan"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                calisanEviHarcananPuan = (ciftlik["calisanEviHarcananPuan"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                yemAdedi = (ciftlik["yemAdedi"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                yemeHarcananPuan = (ciftlik["yemeHarcananPuan"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0
            )
        )
        @Suppress("UNCHECKED_CAST")
        val calisanlar = snap.get("calisanlar") as? List<Map<String, Any?>> ?: emptyList()
        calisanlar.take(5).forEach { c ->
            val tip = (c["tip"] as? Number)?.toInt()?.coerceIn(0, 4) ?: 0
            val gelenAd = (c["ad"] as? String)?.trim().orEmpty()
            val geciciAd = gelenAd.isBlank() || Regex("^Çiftçi\\s*\\d*$", RegexOption.IGNORE_CASE).matches(gelenAd)
            repo.ciftlikCalisaniDao.ekle(CiftlikCalisani(
                tip = tip,
                ad = if (geciciAd) Repository.varsayilanCalisanAdi(tip) else gelenAd,
                rol = c["rol"] as? String ?: "Çiftlik çalışanı",
                seviye = (c["seviye"] as? Number)?.toInt()?.coerceIn(1, 10) ?: 1,
                ilerlemePuan = (c["ilerlemePuan"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                harcananPuan = (c["harcananPuan"] as? Number)?.toInt()?.coerceAtLeast(40) ?: 40,
                alinmaZamani = (c["alinmaZamani"] as? Number)?.toLong() ?: System.currentTimeMillis()
            ))
        }

        val isim = snap.getString("isim")
        val avatar = snap.getString("avatar")
        return BulutProfil(isim, avatar, snap.getString("profilFotoUrl"))
    }

    suspend fun profilFotografiYukle(uri: Uri): String? {
        val uid = BulutHesap.uid() ?: return null
        return try {
            val ref = storage.reference.child("kullanicilar/$uid/profil.jpg")
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        } catch (_: Exception) { null }
    }

    suspend fun notFotografiYukle(uri: Uri): String? {
        val uid = BulutHesap.uid() ?: return null
        return try {
            val ref = storage.reference.child("kullanicilar/$uid/notlar/${System.currentTimeMillis()}.jpg")
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        } catch (_: Exception) { null }
    }
}
