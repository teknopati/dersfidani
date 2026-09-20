package com.dersfidan.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.math.ceil

class Repository(private val context: Context) {

    private val db by lazy { AppDatabase.getInstance(context) }
    val blokDao by lazy { db.blokDao() }
    val notuDao by lazy { db.dersNotuDao() }
    val canliDao by lazy { db.canliDao() }
    val agacDao by lazy { db.agacDao() }
    val gunBonusuDao by lazy { db.gunBonusuDao() }
    val odulBonusuDao by lazy { db.odulBonusuDao() }
    val ciftlikDurumuDao by lazy { db.ciftlikDurumuDao() }
    val ciftlikCalisaniDao by lazy { db.ciftlikCalisaniDao() }

    suspend fun ilkYuklemeYapGerekirse() {
        SeedLoader.ilkYuklemeYapGerekirse(context, db)
        // Çiftlikte en az bir (ücretsiz) ağaç olsun.
        if (agacDao.sayim() == 0) {
            agacDao.ekle(Agac(isim = AgacKatalog.ad(0), satinAlmaFiyati = 0, turId = 0, konumX = -6f, konumZ = 7f))
        }
        if (ciftlikDurumuDao.getir() == null) ciftlikDurumuDao.kaydet(CiftlikDurumu())
        eskiCiftlikKayitlariniTamamla()
        eksikKartBonuslariniTamamla()
    }

    fun gunBloklari(tarih: String): Flow<List<Blok>> = blokDao.gunBloklari(tarih)

    fun tumTarihler(): Flow<List<String>> = blokDao.tumTarihler()

    fun gunlukOzetler(): Flow<List<GunOzet>> = blokDao.gunlukOzetler()

    fun dersBloklari(dersAdi: String): Flow<List<Blok>> = blokDao.dersBloklari(dersAdi)

    fun dersOzetleri(): Flow<List<DersOzet>> = blokDao.dersOzetleri()
    fun denemeOzetleri(): Flow<List<DersOzet>> = blokDao.denemeOzetleri()

    fun dersNotlari(dersAdi: String): Flow<List<DersNotu>> = notuDao.dersNotlari(dersAdi)

    suspend fun notuKaydet(notu: DersNotu) = notuDao.ekle(notu)
    suspend fun notuSil(notu: DersNotu) = notuDao.sil(notu)

    suspend fun isaretle(id: Long, yapildi: Boolean) =
        blokDao.isaretle(id, yapildi, if (yapildi) System.currentTimeMillis() else null)

    suspend fun blokGuncelle(blok: Blok) = blokDao.guncelle(blok)
    suspend fun blokSil(blok: Blok) = blokDao.sil(blok)
    suspend fun blokEkle(blok: Blok) = blokDao.ekle(blok)
    suspend fun blokGetir(id: Long) = blokDao.getirById(id)
    suspend fun gunBloklariListe(tarih: String) = blokDao.gunBloklariListe(tarih)
    suspend fun bugunGelecekBloklar(tarih: String, suan: String) = blokDao.bugunGelecekBloklar(tarih, suan)

    /** Tamamlanan her blok kendi süresine göre puanlanır; sabit rutinler daima 1 puandır. */
    fun tamamlananPuan(): Flow<Int> = blokDao.tamamlananBloklarAkis()
        .map { bloklar -> bloklar.sumOf(::blokPuani) }

    /** Gün tamamlama bonuslarının toplamı. */
    fun toplamGunBonusu(): Flow<Int> = gunBonusuDao.toplamBonus()

    fun toplamKartBonusu(): Flow<Int> = odulBonusuDao.toplamBonus()

    /** Kullanıcının bugüne kadar biriktirdiği TOPLAM puan (ders/soru puanı + gün bonusları). */
    fun kazanilanToplamPuan(): Flow<Int> = combine(
        tamamlananPuan(), toplamGunBonusu(), toplamKartBonusu()
    ) { blok, gun, kart -> blok + gun + kart }

    fun sahipOlunanCanlilar(): Flow<List<Canli>> = canliDao.tumCanlilar()
    fun canliAdetOzetleri(): Flow<List<CanliAdet>> = canliDao.adetOzetleri()

    fun agaclar(): Flow<List<Agac>> = agacDao.tumAgaclar()
    fun ciftlikDurumu(): Flow<CiftlikDurumu?> = ciftlikDurumuDao.akis()
    fun ciftlikCalisanlari(): Flow<List<CiftlikCalisani>> = ciftlikCalisaniDao.akis()

    suspend fun canliSatinAl(esya: MarketEsyasi, ad: String, kalanPuan: Int): SatinAlmaSonucu {
        val mevcutlar = canliDao.tumCanlilarListe()
        val ayniTur = mevcutlar.count { it.esyaId == esya.id }
        if (esya.tur == EsyaTuru.HAYVAN && ayniTur >= MarketKatalog.HAYVAN_ADET_SINIRI) {
            return SatinAlmaSonucu.AdetSiniri
        }
        if (esya.tur == EsyaTuru.HAYVAN && ayniTur > 0) {
            val habitatSeviyesi = mevcutlar.filter { it.esyaId == esya.id }.maxOfOrNull { it.seviye } ?: 1
            val kapasite = (1 + habitatSeviyesi / 2).coerceAtMost(MarketKatalog.HAYVAN_ADET_SINIRI)
            if (ayniTur >= kapasite) return SatinAlmaSonucu.HabitatYetersiz((ayniTur * 2).coerceAtMost(8))
        }
        val temizAd = ad.trim()
        if (esya.tur == EsyaTuru.HAYVAN && temizAd.isBlank()) return SatinAlmaSonucu.GecersizAd
        val fiyat = MarketKatalog.guncelFiyat(esya, ayniTur)
        if (kalanPuan < fiyat) return SatinAlmaSonucu.YetersizPuan(fiyat)

        val ortakHabitat = if (esya.tur == EsyaTuru.HAYVAN) {
            mevcutlar.firstOrNull { it.esyaId == esya.id }
        } else null
        val (x, z) = ortakHabitat?.let { it.konumX to it.konumZ }
            ?: varsayilanKonum(esya.tur, mevcutlar.count { MarketKatalog.bul(it.esyaId)?.tur == esya.tur })
        canliDao.ekle(
            Canli(
                esyaId = esya.id,
                ad = if (esya.tur == EsyaTuru.HAYVAN) temizAd.take(24) else esya.isim,
                satinAlmaFiyati = fiyat,
                seviye = ortakHabitat?.seviye ?: 1,
                seviyeHarcananPuan = ortakHabitat?.seviyeHarcananPuan ?: 0,
                konumX = x,
                konumZ = z
            )
        )
        return SatinAlmaSonucu.Basarili(fiyat)
    }

    /** Çiftliğe yeni bir ağaç ekler (ilk ağaç ücretsizdir, sonrakiler gittikçe pahalanır). */
    suspend fun agacEkle(fiyat: Int, kalanPuan: Int): Boolean {
        val mevcut = agacDao.sayim()
        if (mevcut > 0 && kalanPuan < fiyat) return false
        val tur = AgacKatalog.yeniTur(agacDao.tumAgaclarListe().map { it.turId }.toSet())
        val (x, z) = varsayilanAgacKonumu(mevcut)
        agacDao.ekle(Agac(isim = AgacKatalog.ad(tur), satinAlmaFiyati = fiyat, turId = tur, konumX = x, konumZ = z))
        return true
    }

    suspend fun canliKonumGuncelle(id: Long, x: Float, z: Float) =
        canliDao.konumGuncelle(id, x.coerceIn(-18f, 18f), z.coerceIn(-14f, 14f))

    suspend fun habitatKonumGuncelle(esyaId: String, x: Float, z: Float) =
        canliDao.habitatKonumGuncelle(esyaId, x.coerceIn(-18f, 18f), z.coerceIn(-14f, 14f))

    suspend fun hayvanYasArtir(canli: Canli, kalanPuan: Int): GelistirmeSonucu {
        val kalan = (hayvanYasMaliyeti(canli.yas) - canli.yasIlerlemePuan).coerceAtLeast(1)
        return hayvanYasPuanYatir(canli, kalan, kalanPuan)
    }

    suspend fun hayvanYasPuanYatir(canli: Canli, istenen: Int, kalanPuan: Int): GelistirmeSonucu {
        val guncel = canliDao.getir(canli.id) ?: return GelistirmeSonucu.Gecersiz
        val esya = MarketKatalog.bul(guncel.esyaId)
        if (esya?.tur != EsyaTuru.HAYVAN) return GelistirmeSonucu.Gecersiz
        if (guncel.yas >= MAKS_HAYVAN_YASI) return GelistirmeSonucu.SonSeviye
        val maliyet = hayvanYasMaliyeti(guncel.yas)
        val eksik = (maliyet - guncel.yasIlerlemePuan).coerceAtLeast(0)
        val yatirilan = minOf(istenen.coerceAtLeast(0), kalanPuan, eksik)
        if (yatirilan <= 0) return GelistirmeSonucu.YetersizPuan(eksik)
        canliDao.yasPuaniYatir(guncel.id, yatirilan)
        return if (guncel.yasIlerlemePuan + yatirilan >= maliyet) {
            canliDao.yasTamamla(guncel.id)
            GelistirmeSonucu.Basarili(yatirilan, guncel.yas + 1)
        } else GelistirmeSonucu.Ilerliyor(yatirilan, maliyet - guncel.yasIlerlemePuan - yatirilan)
    }

    suspend fun habitatSeviyeArtir(esyaId: String, mevcutSeviye: Int, kalanPuan: Int): GelistirmeSonucu {
        val guncel = canliDao.tumCanlilarListe().firstOrNull { it.esyaId == esyaId }
            ?: return GelistirmeSonucu.Gecersiz
        val kalan = (yapiSeviyeMaliyeti(guncel.seviye) - guncel.seviyeIlerlemePuan).coerceAtLeast(1)
        return habitatPuaniYatir(esyaId, kalan, kalanPuan)
    }

    suspend fun habitatPuaniYatir(esyaId: String, istenen: Int, kalanPuan: Int): GelistirmeSonucu {
        val esya = MarketKatalog.bul(esyaId)
        if (esya?.tur != EsyaTuru.HAYVAN) return GelistirmeSonucu.Gecersiz
        val guncel = canliDao.tumCanlilarListe().firstOrNull { it.esyaId == esyaId }
            ?: return GelistirmeSonucu.Gecersiz
        if (guncel.seviye >= MAKS_YAPI_SEVIYESI) return GelistirmeSonucu.SonSeviye
        val maliyet = yapiSeviyeMaliyeti(guncel.seviye)
        val eksik = (maliyet - guncel.seviyeIlerlemePuan).coerceAtLeast(0)
        val yatirilan = minOf(istenen.coerceAtLeast(0), kalanPuan, eksik)
        if (yatirilan <= 0) return GelistirmeSonucu.YetersizPuan(eksik)
        canliDao.habitatPuaniYatir(esyaId, yatirilan)
        return if (guncel.seviyeIlerlemePuan + yatirilan >= maliyet) {
            canliDao.habitatSeviyeTamamla(esyaId)
            GelistirmeSonucu.Basarili(yatirilan, guncel.seviye + 1)
        } else GelistirmeSonucu.Ilerliyor(yatirilan, maliyet - guncel.seviyeIlerlemePuan - yatirilan)
    }

    suspend fun yapiSeviyeArtir(canli: Canli, kalanPuan: Int): GelistirmeSonucu {
        val kalan = (yapiSeviyeMaliyeti(canli.seviye) - canli.seviyeIlerlemePuan).coerceAtLeast(1)
        return yapiPuaniYatir(canli, kalan, kalanPuan)
    }

    suspend fun yapiPuaniYatir(canli: Canli, istenen: Int, kalanPuan: Int): GelistirmeSonucu {
        val guncel = canliDao.getir(canli.id) ?: return GelistirmeSonucu.Gecersiz
        val esya = MarketKatalog.bul(guncel.esyaId)
        if (esya?.tur != EsyaTuru.YAPI) return GelistirmeSonucu.Gecersiz
        if (guncel.seviye >= MAKS_YAPI_SEVIYESI) return GelistirmeSonucu.SonSeviye
        val maliyet = yapiSeviyeMaliyeti(guncel.seviye)
        val eksik = (maliyet - guncel.seviyeIlerlemePuan).coerceAtLeast(0)
        val yatirilan = minOf(istenen.coerceAtLeast(0), kalanPuan, eksik)
        if (yatirilan <= 0) return GelistirmeSonucu.YetersizPuan(eksik)
        canliDao.yapiPuaniYatir(guncel.id, yatirilan)
        return if (guncel.seviyeIlerlemePuan + yatirilan >= maliyet) {
            canliDao.yapiSeviyeTamamla(guncel.id)
            GelistirmeSonucu.Basarili(yatirilan, guncel.seviye + 1)
        } else GelistirmeSonucu.Ilerliyor(yatirilan, maliyet - guncel.seviyeIlerlemePuan - yatirilan)
    }

    suspend fun ciftlikAdiniDegistir(ad: String) {
        val durum = ciftlikDurumuDao.getir() ?: CiftlikDurumu()
        ciftlikDurumuDao.kaydet(durum.copy(ad = ad.trim().take(30).ifBlank { "Çiftliğim" }))
    }

    suspend fun yemSatinAl(adet: Int, fiyat: Int, kalanPuan: Int): Boolean {
        if (adet <= 0 || fiyat < 0 || kalanPuan < fiyat) return false
        val durum = ciftlikDurumuDao.getir() ?: CiftlikDurumu()
        ciftlikDurumuDao.kaydet(durum.copy(
            yemAdedi = durum.yemAdedi + adet,
            yemeHarcananPuan = durum.yemeHarcananPuan + fiyat
        ))
        return true
    }

    /** Ortak yemden bir tane harcar; hayvanın bakımını ve yaş bakım ilerlemesini artırır. */
    suspend fun hayvanBesle(canliId: Long): BakimSonucu {
        val durum = ciftlikDurumuDao.getir() ?: CiftlikDurumu()
        if (durum.yemAdedi <= 0) return BakimSonucu.YemYok
        val canli = canliDao.getir(canliId) ?: return BakimSonucu.Bulunamadi
        if (MarketKatalog.bul(canli.esyaId)?.tur != EsyaTuru.HAYVAN) return BakimSonucu.Bulunamadi
        val simdi = System.currentTimeMillis()
        val tokluk = (guncelTokluk(canli, simdi) + 28).coerceAtMost(100)
        val sevgi = (guncelSevgi(canli, simdi) + 7).coerceAtMost(100)
        ciftlikDurumuDao.kaydet(durum.copy(yemAdedi = durum.yemAdedi - 1))
        canliDao.beslenmeGuncelle(canli.id, tokluk, sevgi, simdi)

        var yeniYas: Int? = null
        if (canli.yas < MAKS_HAYVAN_YASI) {
            val hedef = hayvanYasMaliyeti(canli.yas)
            canliDao.bakimIlerlemesiEkle(canli.id, 1)
            if (canli.yasIlerlemePuan + 1 >= hedef) {
                canliDao.yasTamamla(canli.id)
                yeniYas = canli.yas + 1
            }
        }
        return BakimSonucu.Basarili(tokluk, sevgi, yeniYas)
    }

    /** Hayvan görseline dokunmak sevgiyi artırır; açlık değeri önce zamana göre güncellenir. */
    suspend fun hayvaniSev(canliId: Long): BakimSonucu {
        val canli = canliDao.getir(canliId) ?: return BakimSonucu.Bulunamadi
        if (MarketKatalog.bul(canli.esyaId)?.tur != EsyaTuru.HAYVAN) return BakimSonucu.Bulunamadi
        val simdi = System.currentTimeMillis()
        val tokluk = guncelTokluk(canli, simdi)
        val sevgi = (guncelSevgi(canli, simdi) + 3).coerceAtMost(100)
        canliDao.sevgiGuncelle(canli.id, sevgi, simdi)
        return BakimSonucu.Basarili(tokluk, sevgi, null)
    }

    suspend fun calisanEviPuaniYatir(istenen: Int, kalanPuan: Int): GelistirmeSonucu {
        val durum = ciftlikDurumuDao.getir() ?: CiftlikDurumu()
        if (durum.calisanEviSeviye >= 5) return GelistirmeSonucu.SonSeviye
        val hedef = calisanEviMaliyeti(durum.calisanEviSeviye)
        val eksik = (hedef - durum.calisanEviIlerlemePuan).coerceAtLeast(0)
        val miktar = minOf(istenen.coerceAtLeast(0), kalanPuan, eksik)
        if (miktar <= 0) return GelistirmeSonucu.YetersizPuan(eksik)
        val tamam = durum.calisanEviIlerlemePuan + miktar >= hedef
        ciftlikDurumuDao.kaydet(durum.copy(
            calisanEviSeviye = if (tamam) durum.calisanEviSeviye + 1 else durum.calisanEviSeviye,
            calisanEviIlerlemePuan = if (tamam) 0 else durum.calisanEviIlerlemePuan + miktar,
            calisanEviHarcananPuan = durum.calisanEviHarcananPuan + miktar
        ))
        return if (tamam) GelistirmeSonucu.Basarili(miktar, durum.calisanEviSeviye + 1)
        else GelistirmeSonucu.Ilerliyor(miktar, eksik - miktar)
    }

    suspend fun calisanAl(ad: String, kalanPuan: Int): SatinAlmaSonucu {
        val mevcut = ciftlikCalisaniDao.liste()
        val durum = ciftlikDurumuDao.getir() ?: CiftlikDurumu()
        if (mevcut.size >= 5) return SatinAlmaSonucu.AdetSiniri
        if (mevcut.size >= durum.calisanEviSeviye) return SatinAlmaSonucu.CalisanEviYetersiz(durum.calisanEviSeviye + 1)
        if (kalanPuan < 40) return SatinAlmaSonucu.YetersizPuan(40)
        val roller = listOf("Tarla ve ürün sorumlusu", "Hayvan bakıcısı", "Ağaç ve bahçe sorumlusu", "Yapı bakım ustası", "Market ve depo sorumlusu")
        val i = mevcut.size
        ciftlikCalisaniDao.ekle(CiftlikCalisani(tip = i, ad = ad.trim().take(24).ifBlank { varsayilanCalisanAdi(i) }, rol = roller[i]))
        return SatinAlmaSonucu.Basarili(40)
    }

    /** Eski sürümlerdeki "Çiftçi 1" gibi geçici adları Türkçe karakter adlarına çevirir. */
    suspend fun calisanVarsayilanAdlariniUygula() {
        val geciciAd = Regex("^Çiftçi\\s*\\d*$", RegexOption.IGNORE_CASE)
        ciftlikCalisaniDao.liste().forEach { calisan ->
            if (calisan.ad.isBlank() || geciciAd.matches(calisan.ad.trim())) {
                ciftlikCalisaniDao.adGuncelle(calisan.id, varsayilanCalisanAdi(calisan.tip))
            }
        }
    }

    suspend fun calisanPuaniYatir(calisan: CiftlikCalisani, istenen: Int, kalanPuan: Int): GelistirmeSonucu {
        val guncel = ciftlikCalisaniDao.getir(calisan.id) ?: return GelistirmeSonucu.Gecersiz
        if (guncel.seviye >= 10) return GelistirmeSonucu.SonSeviye
        val hedef = calisanMaliyeti(guncel.seviye)
        val eksik = (hedef - guncel.ilerlemePuan).coerceAtLeast(0)
        val miktar = minOf(istenen.coerceAtLeast(0), kalanPuan, eksik)
        if (miktar <= 0) return GelistirmeSonucu.YetersizPuan(eksik)
        ciftlikCalisaniDao.puanYatir(guncel.id, miktar)
        return if (guncel.ilerlemePuan + miktar >= hedef) {
            ciftlikCalisaniDao.seviyeTamamla(guncel.id)
            GelistirmeSonucu.Basarili(miktar, guncel.seviye + 1)
        } else GelistirmeSonucu.Ilerliyor(miktar, eksik - miktar)
    }

    suspend fun canliAdiniDegistir(id: Long, ad: String) = canliDao.adGuncelle(id, ad.trim().take(24))
    suspend fun calisanAdiniDegistir(id: Long, ad: String) = ciftlikCalisaniDao.adGuncelle(id, ad.trim().take(24))

    suspend fun agacKonumGuncelle(id: Long, x: Float, z: Float) =
        agacDao.konumGuncelle(id, x.coerceIn(-18f, 18f), z.coerceIn(-14f, 14f))

    private suspend fun eskiCiftlikKayitlariniTamamla() {
        val sayaclar = mutableMapOf<String, Int>()
        val habitatKonumlari = mutableMapOf<String, Pair<Float, Float>>()
        canliDao.tumCanlilarListe().forEach { canli ->
            val esya = MarketKatalog.bul(canli.esyaId) ?: return@forEach
            val sira = sayaclar.getOrDefault(canli.esyaId, 0)
            if (canli.ad.isBlank()) canliDao.adGuncelle(
                canli.id,
                if (esya.tur == EsyaTuru.HAYVAN) "${esya.isim} ${sira + 1}" else esya.isim
            )
            if (canli.satinAlmaFiyati <= 0) {
                canliDao.fiyatGuncelle(canli.id, MarketKatalog.guncelFiyat(esya, sira))
            }
            if (esya.tur == EsyaTuru.HAYVAN) {
                val ortak = habitatKonumlari.getOrPut(canli.esyaId) { canli.konumX to canli.konumZ }
                if (canli.konumX != ortak.first || canli.konumZ != ortak.second) {
                    canliDao.konumGuncelle(canli.id, ortak.first, ortak.second)
                }
            }
            sayaclar[canli.esyaId] = sira + 1
        }
    }

    private fun varsayilanKonum(tur: EsyaTuru, index: Int): Pair<Float, Float> {
        val sutun = index % 6
        val satir = index / 6
        return if (tur == EsyaTuru.HAYVAN) {
            (-10f + sutun * 3.5f) to (-5.5f + (satir % 4) * 3.2f)
        } else {
            (-10f + sutun * 4f) to (5.5f - (satir % 3) * 4f)
        }
    }

    private fun varsayilanAgacKonumu(index: Int): Pair<Float, Float> =
        (-11f + (index % 7) * 3.6f) to (9f - ((index / 7) % 3) * 3.5f)

    /** Ders/deneme kartı ilk kez tamamen bittiğinde kart süresinin %10'unu tek sefer verir. */
    suspend fun kartBonusuKontrolEt(blok: Blok): Int {
        val ad = blok.dersAdi ?: return 0
        val deneme = blok.kategori in setOf("Deneme", "Deneme Analizi")
        val tur = if (deneme) "deneme" else "ders"
        val ozet = if (deneme) blokDao.denemeKartOzeti(ad) else blokDao.dersKartOzeti(ad)
        if (ozet == null || ozet.toplam <= 0 || ozet.yapilan != ozet.toplam) return 0
        if (odulBonusuDao.getir(tur, ad) != null) return 0
        val puan = ceil(ozet.toplamDk * 0.10).toInt().coerceAtLeast(1)
        return if (odulBonusuDao.ekle(OdulBonusu(tur, ad, puan)) != -1L) puan else 0
    }

    /** Migration sonrası önceden bitmiş kartlara da ödülü bir kez yazar. */
    suspend fun eksikKartBonuslariniTamamla() {
        val kartlar = blokDao.tamamlananDersKartlari().map { "ders" to it } +
            blokDao.tamamlananDenemeKartlari().map { "deneme" to it }
        kartlar.forEach { (tur, ozet) ->
            if (odulBonusuDao.getir(tur, ozet.dersAdi) == null) {
                odulBonusuDao.ekle(OdulBonusu(tur, ozet.dersAdi, ceil(ozet.toplamDk * .10).toInt().coerceAtLeast(1)))
            }
        }
    }

    /** Hesap değişiminde programı korur, yalnızca kullanıcıya ait yerel ilerlemeyi temizler. */
    suspend fun kullaniciVerisiniTemizle() {
        blokDao.tumTamamlamalariSifirla()
        canliDao.hepsiniSil()
        agacDao.hepsiniSil()
        gunBonusuDao.hepsiniSil()
        odulBonusuDao.hepsiniSil()
        notuDao.hepsiniSil()
        ciftlikCalisaniDao.temizle()
        ciftlikDurumuDao.temizle()
        ciftlikDurumuDao.kaydet(CiftlikDurumu())
    }

    suspend fun varsayilanAgaciEkleGerekirse() {
        if (agacDao.sayim() == 0) agacDao.ekle(Agac(isim = AgacKatalog.ad(0), turId = 0, konumX = -6f, konumZ = 7f))
    }

    /** Bankadaki puandan ağaca büyüme puanı yatırır (kullanıcı ağaca dokunarak/basılı tutarak büyütür). */
    suspend fun agacaPuanYatir(agacId: Long, kalanPuan: Int, miktar: Int): Int {
        val yatirilacak = miktar.coerceAtMost(kalanPuan)
        if (yatirilacak <= 0) return 0
        agacDao.puanYatir(agacId, yatirilacak)
        return yatirilacak
    }

    /**
     * O günün TÜM blokları (molalar dahil) tamamlandıysa ve bonus daha önce verilmediyse
     * GUN_BONUS_PUAN kadar bonus ekler. Yeni bonus verildiyse true döner (alkışlı kutlama için).
     */
    suspend fun gunTamamlandiKontrolEt(tarih: String): Boolean {
        val bloklar = blokDao.gunBloklariListe(tarih)
        if (bloklar.isEmpty()) return false
        val hepsiTamam = bloklar.all { it.tamamlandi }
        if (!hepsiTamam) return false
        if (gunBonusuDao.getir(tarih) != null) return false
        gunBonusuDao.ekle(GunBonusu(tarih = tarih, puan = GUN_BONUS_PUAN))
        return true
    }

    companion object {
        const val GUN_BONUS_PUAN = 15
        const val MAKS_HAYVAN_YASI = 20
        const val MAKS_YAPI_SEVIYESI = 10

        /** 1->2: 20, 2->3: 35 ... 19->20: 290 puan. */
        fun hayvanYasMaliyeti(mevcutYas: Int): Int = 20 + (mevcutYas.coerceAtLeast(1) - 1) * 15

        /** 1->2: 30, 2->3: 50 ... 9->10: 190 puan. */
        fun yapiSeviyeMaliyeti(mevcutSeviye: Int): Int = 30 + (mevcutSeviye.coerceAtLeast(1) - 1) * 20

        fun calisanMaliyeti(mevcutSeviye: Int): Int = ((mevcutSeviye + 1).coerceIn(1, 10)) * 40
        fun calisanEviMaliyeti(mevcutSeviye: Int): Int = mevcutSeviye.coerceIn(1, 5) * 40
        fun varsayilanCalisanAdi(tip: Int): String = listOf("Ayşe", "Mehmet", "Emre", "Fatma", "Ece")[tip.coerceIn(0, 4)]

        /** Her 6 saatte 4 tokluk; her 12 saatte 2 sevgi azalır. Değerler uygulama kapalıyken de işler. */
        fun guncelTokluk(canli: Canli, simdi: Long = System.currentTimeMillis()): Int {
            val gecen = ((simdi - canli.sonBeslenmeZamani).coerceAtLeast(0L) / (6 * 60 * 60 * 1000L)).toInt()
            return (canli.tokluk - gecen * 4).coerceIn(0, 100)
        }

        fun guncelSevgi(canli: Canli, simdi: Long = System.currentTimeMillis()): Int {
            val gecen = ((simdi - canli.sonSevilmeZamani).coerceAtLeast(0L) / (12 * 60 * 60 * 1000L)).toInt()
            return (canli.sevgi - gecen * 2).coerceIn(0, 100)
        }

        fun ruhHali(sevgi: Int, tokluk: Int): String = when {
            tokluk < 20 -> "Çok aç"
            sevgi < 25 -> "Üzgün"
            sevgi < 55 -> "Sakin"
            sevgi < 85 -> "Mutlu"
            else -> "Çok mutlu"
        }

        fun hayvanUnvani(yas: Int): String = when (yas.coerceIn(1, MAKS_HAYVAN_YASI)) {
            1 -> "Yeni Doğan"
            2 -> "Bebek"
            in 3..4 -> "Yavru"
            in 5..7 -> "Meraklı Küçük"
            in 8..10 -> "Genç"
            in 11..13 -> "Ergen"
            in 14..16 -> "Yetişkin"
            in 17..19 -> "Deneyimli"
            else -> "Çiftliğin Efsanesi"
        }

        /**
         * Günlük sabit rutinler 1, diğer günlük bloklar sürenin %20'si kadar puan verir.
         * Kart tamamen bittiğinde verilen ayrı ödül ise kart süresinin %10'u olarak kalır.
         */
        private val SABIT_BLOKLAR = setOf(
            "Uyanma/Hazırlık", "Uyanma", "Yemek/Mola", "Mola", "Serbest Zaman",
            "Yürüyüş/Hareket", "Şekerleme", "Uyku"
        )

        fun blokPuani(blok: Blok): Int = if (blok.kategori in SABIT_BLOKLAR) {
            1
        } else {
            ceil((blok.sureDk ?: 0) * 0.20).toInt().coerceAtLeast(1)
        }

        /** Eski ekranların uyumluluğu: toplam kart süresinin tek seferlik ödülü. */
        fun puanPerAchievement(toplamDk: Int): Int = ceil(toplamDk * 0.10).toInt().coerceAtLeast(1)

        // (eşik, aşama adı) — bir ağacın kendi yatirilanPuan'ına göre büyüme aşaması
        private val asamalar = listOf(
            0 to "Tohum",
            5 to "Filiz",
            20 to "Küçük Fidan",
            50 to "Gelişen Fidan",
            100 to "Genç Ağaç",
            200 to "Olgun Ağaç"
        )
        private const val MEYVE_BASLANGIC = 200
        private const val MEYVE_ARALIK = 50

        /** Bir sonraki ağacın kaç puana mal olacağını hesaplar. İlk ağaç (mevcutSayisi=0) ücretsizdir. */
        fun sonrakiAgacFiyati(mevcutSayisi: Int): Int =
            if (mevcutSayisi <= 0) 0 else 60 + (mevcutSayisi - 1) * 50

        fun hesaplaAgac(yatirilanPuan: Int): AgacDurumu {
            var asamaIndex = 0
            for ((i, esikAd) in asamalar.withIndex()) {
                if (yatirilanPuan >= esikAd.first) asamaIndex = i
            }
            val asamaAdi = asamalar[asamaIndex].second
            val sonrakiEsik = asamalar.getOrNull(asamaIndex + 1)?.first
            val meyveSayisi = if (yatirilanPuan >= MEYVE_BASLANGIC) {
                1 + (yatirilanPuan - MEYVE_BASLANGIC) / MEYVE_ARALIK
            } else 0
            val sonrakiMeyveIcin = if (yatirilanPuan >= MEYVE_BASLANGIC) {
                MEYVE_BASLANGIC + (meyveSayisi) * MEYVE_ARALIK
            } else MEYVE_BASLANGIC
            return AgacDurumu(
                yatirilanPuan = yatirilanPuan,
                asamaIndex = asamaIndex,
                asamaAdi = asamaAdi,
                sonrakiEsik = sonrakiEsik,
                meyveSayisi = meyveSayisi,
                sonrakiMeyveEsigi = sonrakiMeyveIcin
            )
        }
    }
}

sealed interface SatinAlmaSonucu {
    data class Basarili(val odenenFiyat: Int) : SatinAlmaSonucu
    data class YetersizPuan(val gerekenFiyat: Int) : SatinAlmaSonucu
    data object AdetSiniri : SatinAlmaSonucu
    data object GecersizAd : SatinAlmaSonucu
    data class HabitatYetersiz(val gerekenSeviye: Int) : SatinAlmaSonucu
    data class CalisanEviYetersiz(val gerekenSeviye: Int) : SatinAlmaSonucu
}

sealed interface GelistirmeSonucu {
    data class Basarili(val harcananPuan: Int, val yeniDeger: Int) : GelistirmeSonucu
    data class YetersizPuan(val gerekenPuan: Int) : GelistirmeSonucu
    data class Ilerliyor(val harcananPuan: Int, val kalanPuan: Int) : GelistirmeSonucu
    data object SonSeviye : GelistirmeSonucu
    data object Gecersiz : GelistirmeSonucu
}

sealed interface BakimSonucu {
    data class Basarili(val tokluk: Int, val sevgi: Int, val yeniYas: Int?) : BakimSonucu
    data object YemYok : BakimSonucu
    data object Bulunamadi : BakimSonucu
}

data class AgacDurumu(
    val yatirilanPuan: Int,
    val asamaIndex: Int,
    val asamaAdi: String,
    val sonrakiEsik: Int?,
    val meyveSayisi: Int,
    val sonrakiMeyveEsigi: Int
)
