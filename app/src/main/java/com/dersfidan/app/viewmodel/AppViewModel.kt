package com.dersfidan.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dersfidan.app.DersFidanApp
import com.dersfidan.app.audio.SesYoneticisi
import com.dersfidan.app.data.*
import com.dersfidan.app.notif.NotificationScheduler
import com.dersfidan.app.util.TarihUtil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: Repository = (app as DersFidanApp).repository
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale("tr"))

    val bugununTarihi: String = dateFmt.format(Date())

    private val _secilenTarih = MutableStateFlow(bugununTarihi)
    val secilenTarih: StateFlow<String> = _secilenTarih

    @OptIn(ExperimentalCoroutinesApi::class)
    val secilenGunBloklari: StateFlow<List<Blok>> = _secilenTarih.flatMapLatest { tarih ->
        repo.gunBloklari(tarih)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gunlukOzetler: StateFlow<List<GunOzet>> = repo.gunlukOzetler()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dersOzetleri: StateFlow<List<DersOzet>> = repo.dersOzetleri()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val denemeOzetleri: StateFlow<List<DersOzet>> = repo.denemeOzetleri()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Çiftlik: ağaçlar, hayvanlar, puan bankası ---

    val agaclar: StateFlow<List<Agac>> = repo.agaclar()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Bir sonraki ağacın fiyatı (mevcut ağaç sayısına göre gittikçe artar). */
    val sonrakiAgacFiyati: StateFlow<Int> = agaclar.map { Repository.sonrakiAgacFiyati(it.size) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val sahipOlunanCanlilar: StateFlow<List<Canli>> = repo.sahipOlunanCanlilar()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val canliAdetleri: StateFlow<Map<String, Int>> = repo.canliAdetOzetleri()
        .map { liste -> liste.associate { it.esyaId to it.adet } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val ciftlikDurumu: StateFlow<CiftlikDurumu?> = repo.ciftlikDurumu()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val ciftlikCalisanlari: StateFlow<List<CiftlikCalisani>> = repo.ciftlikCalisanlari()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Bankadaki puan; satın alma, büyüme, yaş ve yapı/habitat geliştirmeleri tek kez düşülür. */
    val kalanPuan: StateFlow<Int> = combine(
        repo.kazanilanToplamPuan(), sahipOlunanCanlilar, agaclar, ciftlikCalisanlari, ciftlikDurumu
    ) { kazanilan, canlilar, agaclar, calisanlar, durum ->
        val harcananAlimVeYas = canlilar.sumOf { it.satinAlmaFiyati + it.yasHarcananPuan }
        val harcananYapiSeviye = canlilar
            .filter { MarketKatalog.bul(it.esyaId)?.tur == EsyaTuru.YAPI }
            .sumOf { it.seviyeHarcananPuan }
        // Aynı tür hayvanların ortak habitat seviyesi bütün satırlarda aynıdır; yalnız bir kez düşülür.
        val harcananHabitatSeviye = canlilar
            .filter { MarketKatalog.bul(it.esyaId)?.tur == EsyaTuru.HAYVAN }
            .groupBy { it.esyaId }
            .values.sumOf { grup -> grup.maxOfOrNull { it.seviyeHarcananPuan } ?: 0 }
        val harcananAgac = agaclar.sumOf { it.satinAlmaFiyati + it.yatirilanPuan }
        val harcananCalisan = calisanlar.sumOf { it.harcananPuan }
        val harcananEv = durum?.calisanEviHarcananPuan ?: 0
        val harcananYem = durum?.yemeHarcananPuan ?: 0
        (kazanilan - harcananAlimVeYas - harcananYapiSeviye - harcananHabitatSeviye - harcananAgac - harcananCalisan - harcananEv - harcananYem).coerceAtLeast(0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Bugüne kadar kazanılan TOPLAM puan (harcansa bile düşmez) — profil istatistiği için. */
    val toplamKazanilanPuan: StateFlow<Int> = repo.kazanilanToplamPuan()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _marketMesaji = MutableStateFlow<String?>(null)
    val marketMesaji: StateFlow<String?> = _marketMesaji

    // --- Hava durumu: kullanıcı şehrini kendi girer, gerçek anlık duruma göre
    // çiftlikte yağmur/kar/bulut gösterilir. Konum izni istenmez. ---
    private val _sehir = MutableStateFlow(HavaPrefs.sehir(app))
    val sehir: StateFlow<String> = _sehir

    private val _havaDurumu = MutableStateFlow<HavaSonucu?>(null)
    val havaDurumu: StateFlow<HavaSonucu?> = _havaDurumu

    private var havaYenilemeIsi: Job? = null

    init {
        _havaDurumu.value = HavaPrefs.sonHava(app)
        havaGuncelle()
        viewModelScope.launch { repo.calisanVarsayilanAdlariniUygula() }
    }

    fun sehirDegistir(yeniSehir: String) {
        if (yeniSehir.isBlank()) return
        HavaPrefs.sehirKaydet(getApplication(), yeniSehir)
        _sehir.value = yeniSehir
        havaGuncelle()
    }

    fun havaGuncelle() {
        viewModelScope.launch {
            val sonuc = WeatherService.getir(_sehir.value)
            if (sonuc != null) {
                _havaDurumu.value = sonuc
                HavaPrefs.sonHavaKaydet(getApplication(), sonuc)
            }
        }
    }

    fun ciftlikAktif(aktif: Boolean) {
        if (!aktif) { havaYenilemeIsi?.cancel(); havaYenilemeIsi = null; return }
        if (havaYenilemeIsi?.isActive == true) return
        havaYenilemeIsi = viewModelScope.launch {
            while (true) { havaGuncelle(); delay(10 * 60 * 1000L) }
        }
    }

    /** Gün tamamlanınca (tüm bloklar bitince) bir defalık tetiklenir: taşınan değer kazanılan bonus puandır. */
    private val _gunTamamlandiOlayi = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val gunTamamlandiOlayi: SharedFlow<Int> = _gunTamamlandiOlayi

    /** Profilim'den "Çıkış Yap" ile tetiklenir; hem bulut oturumunu hem yerel profili kapatır. */
    private val _cikisYapildiOlayi = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val cikisYapildiOlayi: SharedFlow<Unit> = _cikisYapildiOlayi

    fun cikisYap() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            if (BulutHesap.girisliMi()) {
                var fotoUrl = ProfilPrefs.fotoUrl(context)
                val yerelFoto = ProfilPrefs.foto(context)
                if (fotoUrl == null && yerelFoto != null) {
                    fotoUrl = BulutSenkron.profilFotografiYukle(Uri.parse(yerelFoto))
                    if (fotoUrl != null) ProfilPrefs.fotoKaydet(context, yerelFoto, fotoUrl)
                }
                BulutSenkron.yukle(repo, ProfilPrefs.isim(context), ProfilPrefs.avatar(context), fotoUrl)
            }
            repo.kullaniciVerisiniTemizle()
            yerelFotograflariTemizle(context)
            ProfilPrefs.cikisYap(context)
            BulutHesap.cikisYap()
            _cikisYapildiOlayi.emit(Unit)
        }
    }

    /** Bulut yedeği: giriş yapılmışsa her ilerleme değişikliğinden sonra sessizce (arka planda) çağrılır. */
    private fun bulutaYedekle() {
        if (!BulutHesap.girisliMi()) return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val isim = ProfilPrefs.isim(context)
            val avatar = ProfilPrefs.avatar(context)
            var fotoUrl = ProfilPrefs.fotoUrl(context)
            val yerelFoto = ProfilPrefs.foto(context)
            if (fotoUrl == null && yerelFoto != null) {
                fotoUrl = BulutSenkron.profilFotografiYukle(Uri.parse(yerelFoto))
                if (fotoUrl != null) ProfilPrefs.fotoKaydet(context, yerelFoto, fotoUrl)
            }
            BulutSenkron.yukle(repo, isim, avatar, fotoUrl)
        }
    }

    fun canliSatinAl(esya: MarketEsyasi, ad: String = "") {
        viewModelScope.launch {
            val sonuc = repo.canliSatinAl(esya, ad, kalanPuan.value)
            _marketMesaji.value = when (sonuc) {
                is SatinAlmaSonucu.Basarili -> if (esya.tur == EsyaTuru.HAYVAN) {
                    "${esya.emoji} ${ad.trim()} habitatına katıldı! (${sonuc.odenenFiyat} puan)"
                } else {
                    "${esya.emoji} ${esya.isim} çiftliğine yerleştirildi! (${sonuc.odenenFiyat} puan)"
                }
                is SatinAlmaSonucu.YetersizPuan -> "Bu alım için ${sonuc.gerekenFiyat} puana ihtiyacın var."
                SatinAlmaSonucu.AdetSiniri -> "Aynı hayvandan en fazla ${MarketKatalog.HAYVAN_ADET_SINIRI} tane alabilirsin."
                SatinAlmaSonucu.GecersizAd -> "Hayvanına bir isim vermelisin."
                is SatinAlmaSonucu.HabitatYetersiz -> "Yeni hayvan için habitatı ${sonuc.gerekenSeviye}. seviyeye yükseltmelisin."
                is SatinAlmaSonucu.CalisanEviYetersiz -> "Önce çalışan evini ${sonuc.gerekenSeviye}. seviyeye yükseltmelisin."
            }
            if (sonuc is SatinAlmaSonucu.Basarili) {
                SesYoneticisi.levelAtlamaCal()
                bulutaYedekle()
            }
        }
    }

    fun canliKonumGuncelle(id: Long, x: Float, z: Float) {
        viewModelScope.launch {
            repo.canliKonumGuncelle(id, x, z)
            bulutaYedekle()
        }
    }

    fun habitatKonumGuncelle(esyaId: String, x: Float, z: Float) {
        viewModelScope.launch {
            repo.habitatKonumGuncelle(esyaId, x, z)
            bulutaYedekle()
        }
    }

    fun hayvanYasArtir(canli: Canli) {
        viewModelScope.launch {
            val sonuc = repo.hayvanYasArtir(canli, kalanPuan.value)
            _marketMesaji.value = when (sonuc) {
                is GelistirmeSonucu.Basarili -> "${canli.ad} artık ${sonuc.yeniDeger} yaşında: ${Repository.hayvanUnvani(sonuc.yeniDeger)}!"
                is GelistirmeSonucu.YetersizPuan -> "Yaş yükseltmek için ${sonuc.gerekenPuan} puana ihtiyacın var."
                is GelistirmeSonucu.Ilerliyor -> "${sonuc.harcananPuan} puan yatırıldı; ${sonuc.kalanPuan} puan kaldı."
                GelistirmeSonucu.SonSeviye -> "${canli.ad} en yüksek yaşa ulaştı."
                GelistirmeSonucu.Gecersiz -> "Bu canlı için yaş yükseltilemedi."
            }
            if (sonuc is GelistirmeSonucu.Basarili) {
                SesYoneticisi.levelAtlamaCal()
                bulutaYedekle()
            }
        }
    }

    fun habitatSeviyeArtir(esyaId: String, mevcutSeviye: Int) {
        viewModelScope.launch {
            val sonuc = repo.habitatSeviyeArtir(esyaId, mevcutSeviye, kalanPuan.value)
            _marketMesaji.value = when (sonuc) {
                is GelistirmeSonucu.Basarili -> "${MarketKatalog.habitatAdi(esyaId)} ${sonuc.yeniDeger}. seviyeye yükseldi!"
                is GelistirmeSonucu.YetersizPuan -> "Habitatı geliştirmek için ${sonuc.gerekenPuan} puana ihtiyacın var."
                is GelistirmeSonucu.Ilerliyor -> "Habitat gelişimine ${sonuc.harcananPuan} puan yatırıldı; ${sonuc.kalanPuan} kaldı."
                GelistirmeSonucu.SonSeviye -> "Bu habitat en yüksek seviyede."
                GelistirmeSonucu.Gecersiz -> "Habitat geliştirilemedi."
            }
            if (sonuc is GelistirmeSonucu.Basarili) {
                SesYoneticisi.levelAtlamaCal()
                bulutaYedekle()
            }
        }
    }

    fun yapiSeviyeArtir(canli: Canli) {
        viewModelScope.launch {
            val sonuc = repo.yapiSeviyeArtir(canli, kalanPuan.value)
            _marketMesaji.value = when (sonuc) {
                is GelistirmeSonucu.Basarili -> "${canli.ad} ${sonuc.yeniDeger}. seviyeye yükseldi!"
                is GelistirmeSonucu.YetersizPuan -> "Yapıyı geliştirmek için ${sonuc.gerekenPuan} puana ihtiyacın var."
                is GelistirmeSonucu.Ilerliyor -> "Yapıya ${sonuc.harcananPuan} puan yatırıldı; ${sonuc.kalanPuan} kaldı."
                GelistirmeSonucu.SonSeviye -> "Bu yapı en yüksek seviyede."
                GelistirmeSonucu.Gecersiz -> "Yapı geliştirilemedi."
            }
            if (sonuc is GelistirmeSonucu.Basarili) {
                SesYoneticisi.levelAtlamaCal()
                bulutaYedekle()
            }
        }
    }

    fun hayvanYasPuaniYatir(canli: Canli, miktar: Int) {
        viewModelScope.launch {
            val sonuc = repo.hayvanYasPuanYatir(canli, miktar, kalanPuan.value)
            gelistirmeMesaji(canli.ad, sonuc)
            bulutaYedekle()
        }
    }

    fun habitatPuaniYatir(esyaId: String, miktar: Int) {
        viewModelScope.launch {
            val sonuc = repo.habitatPuaniYatir(esyaId, miktar, kalanPuan.value)
            gelistirmeMesaji(MarketKatalog.habitatAdi(esyaId), sonuc)
            bulutaYedekle()
        }
    }

    fun yapiPuaniYatir(canli: Canli, miktar: Int) {
        viewModelScope.launch {
            val sonuc = repo.yapiPuaniYatir(canli, miktar, kalanPuan.value)
            gelistirmeMesaji(canli.ad, sonuc)
            bulutaYedekle()
        }
    }

    fun ciftlikAdiniDegistir(ad: String) = viewModelScope.launch {
        repo.ciftlikAdiniDegistir(ad); bulutaYedekle()
    }

    fun canliAdiniDegistir(id: Long, ad: String) = viewModelScope.launch {
        repo.canliAdiniDegistir(id, ad); bulutaYedekle()
    }

    fun calisanAdiniDegistir(id: Long, ad: String) = viewModelScope.launch {
        repo.calisanAdiniDegistir(id, ad); bulutaYedekle()
    }

    fun calisanAl(ad: String) = viewModelScope.launch {
        val sonuc = repo.calisanAl(ad, kalanPuan.value)
        _marketMesaji.value = when (sonuc) {
            is SatinAlmaSonucu.Basarili -> "$ad çiftliğin ekibine katıldı!"
            is SatinAlmaSonucu.YetersizPuan -> "Çiftçi almak için ${sonuc.gerekenFiyat} puan gerekli."
            is SatinAlmaSonucu.CalisanEviYetersiz -> "Önce çalışan evini ${sonuc.gerekenSeviye}. seviyeye yükselt."
            SatinAlmaSonucu.AdetSiniri -> "Çiftlikte en fazla 5 çalışan olabilir."
            else -> "Çalışan alınamadı."
        }
        if (sonuc is SatinAlmaSonucu.Basarili) SesYoneticisi.levelAtlamaCal()
        bulutaYedekle()
    }

    fun calisanPuaniYatir(calisan: CiftlikCalisani, miktar: Int) = viewModelScope.launch {
        val sonuc = repo.calisanPuaniYatir(calisan, miktar, kalanPuan.value)
        gelistirmeMesaji(calisan.ad, sonuc); bulutaYedekle()
    }

    fun calisanEviPuaniYatir(miktar: Int) = viewModelScope.launch {
        val sonuc = repo.calisanEviPuaniYatir(miktar, kalanPuan.value)
        gelistirmeMesaji("Çalışan evi", sonuc); bulutaYedekle()
    }

    private fun gelistirmeMesaji(ad: String, sonuc: GelistirmeSonucu) {
        _marketMesaji.value = when (sonuc) {
            is GelistirmeSonucu.Basarili -> "$ad ${sonuc.yeniDeger}. seviyeye ulaştı!"
            is GelistirmeSonucu.Ilerliyor -> "$ad için ${sonuc.harcananPuan} puan yatırıldı; ${sonuc.kalanPuan} kaldı."
            is GelistirmeSonucu.YetersizPuan -> "${sonuc.gerekenPuan} puan daha gerekli."
            GelistirmeSonucu.SonSeviye -> "$ad en yüksek seviyede."
            GelistirmeSonucu.Gecersiz -> "İşlem yapılamadı."
        }
        if (sonuc is GelistirmeSonucu.Basarili) SesYoneticisi.levelAtlamaCal()
    }

    fun agacKonumGuncelle(id: Long, x: Float, z: Float) {
        viewModelScope.launch {
            repo.agacKonumGuncelle(id, x, z)
            bulutaYedekle()
        }
    }

    fun agacEkle() {
        viewModelScope.launch {
            val fiyat = sonrakiAgacFiyati.value
            val basarili = repo.agacEkle(fiyat, kalanPuan.value)
            _marketMesaji.value = if (basarili) {
                "Yeni bir ağaç diktin! 🌱"
            } else {
                "Yeni ağaç için $fiyat puana ihtiyacın var."
            }
            if (basarili) {
                SesYoneticisi.levelAtlamaCal()
                bulutaYedekle()
            }
        }
    }

    fun yemSatinAl(adet: Int, fiyat: Int) {
        viewModelScope.launch {
            val basarili = repo.yemSatinAl(adet, fiyat, kalanPuan.value)
            _marketMesaji.value = if (basarili) "$adet adet hayvan yemi depoya eklendi!" else "Bu yem paketi için $fiyat puan gerekli."
            if (basarili) bulutaYedekle()
        }
    }

    fun hayvanBesle(canli: Canli) {
        viewModelScope.launch {
            when (val sonuc = repo.hayvanBesle(canli.id)) {
                is BakimSonucu.Basarili -> {
                    _marketMesaji.value = if (sonuc.yeniYas != null) {
                        "${canli.ad} beslendi ve ${sonuc.yeniYas} yaşına ulaştı!"
                    } else "${canli.ad} afiyetle yedi! Tokluk ${sonuc.tokluk}, sevgi ${sonuc.sevgi}."
                    if (sonuc.yeniYas != null) SesYoneticisi.levelAtlamaCal()
                    bulutaYedekle()
                }
                BakimSonucu.YemYok -> _marketMesaji.value = "Yemin kalmadı. Market'ten ortak hayvan yemi alabilirsin."
                BakimSonucu.Bulunamadi -> _marketMesaji.value = "Hayvan bulunamadı."
            }
        }
    }

    fun hayvaniSev(canli: Canli) {
        viewModelScope.launch {
            if (repo.hayvaniSev(canli.id) is BakimSonucu.Basarili) bulutaYedekle()
        }
    }

    /** Ağaca dokununca çağrılır: bankadan 1 (ya da uzun basınca tüm bakiye) puan yatırır. */
    fun agacaPuanYatir(agac: Agac, miktar: Int) {
        viewModelScope.launch {
            // Bankada puan yoksa sessizce hiçbir şey olmaz (rahatsız edici uyarı YOK).
            // Oynanış açıklaması Profilim > Nasıl Oynanır bölümünde.
            val oncekiAsama = Repository.hesaplaAgac(agac.yatirilanPuan).asamaIndex
            val yatirilan = repo.agacaPuanYatir(agac.id, kalanPuan.value, miktar)
            val yeniAsama = Repository.hesaplaAgac(agac.yatirilanPuan + yatirilan).asamaIndex
            if (yatirilan > 0 && yeniAsama > oncekiAsama) SesYoneticisi.levelAtlamaCal()
            bulutaYedekle()
        }
    }

    fun marketMesajiniTemizle() { _marketMesaji.value = null }
    fun ciftlikSelami(ad: String) { _marketMesaji.value = "$ad sana selam verdi! ♥" }

    private val _secilenDers = MutableStateFlow<String?>(null)
    val secilenDers: StateFlow<String?> = _secilenDers

    @OptIn(ExperimentalCoroutinesApi::class)
    val secilenDersBloklari: StateFlow<List<Blok>> = _secilenDers.flatMapLatest { ders ->
        if (ders == null) flowOf(emptyList()) else repo.dersBloklari(ders)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val secilenDersNotlari: StateFlow<List<DersNotu>> = _secilenDers.flatMapLatest { ders ->
        if (ders == null) flowOf(emptyList()) else repo.dersNotlari(ders)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun tarihSec(tarih: String) { _secilenTarih.value = tarih }
    fun dersSec(ders: String?) { _secilenDers.value = ders }

    fun isaretle(blok: Blok, yapildi: Boolean) {
        // Güvenlik: henüz gelmemiş (gelecek tarihli) bir günün görevi hiçbir yoldan tamamlanamaz.
        if (yapildi && TarihUtil.gelecekMi(blok.tarih)) return
        viewModelScope.launch {
            repo.isaretle(blok.id, yapildi)
            if (yapildi) {
                SesYoneticisi.onaylaCal()
                NotificationScheduler.iptalEt(getApplication(), blok.id)
                val yeniBonusVerildi = repo.gunTamamlandiKontrolEt(blok.tarih)
                if (yeniBonusVerildi) {
                    _gunTamamlandiOlayi.tryEmit(Repository.GUN_BONUS_PUAN)
                }
                val kartBonusu = repo.kartBonusuKontrolEt(blok)
                if (kartBonusu > 0) _gunTamamlandiOlayi.tryEmit(kartBonusu)
            } else {
                NotificationScheduler.planlaBlok(getApplication(), blok.copy(tamamlandi = false))
            }
            bulutaYedekle()
        }
    }

    fun notEkle(dersAdi: String, metin: String, fotoYolu: String?) {
        viewModelScope.launch {
            val url = fotoYolu?.let {
                val uri = if (it.startsWith("content:") || it.startsWith("file:")) Uri.parse(it) else Uri.fromFile(File(it))
                BulutSenkron.notFotografiYukle(uri)
            }
            repo.notuKaydet(DersNotu(dersAdi = dersAdi, metin = metin, fotoYolu = fotoYolu, bulutFotoUrl = url))
            bulutaYedekle()
        }
    }

    fun notSil(notu: DersNotu) {
        viewModelScope.launch {
            repo.notuSil(notu)
            bulutaYedekle()
        }
    }

    fun blokGuncelle(blok: Blok) {
        viewModelScope.launch {
            repo.blokGuncelle(blok)
            NotificationScheduler.iptalEt(getApplication(), blok.id)
            if (!blok.tamamlandi) NotificationScheduler.planlaBlok(getApplication(), blok)
        }
    }

    fun blokSil(blok: Blok) {
        viewModelScope.launch {
            NotificationScheduler.iptalEt(getApplication(), blok.id)
            repo.blokSil(blok)
        }
    }

    fun blokEkle(tarih: String, gunGirisi: String, saatBaslangic: String, saatBitis: String, kategori: String, metin: String) {
        viewModelScope.launch {
            val dersAdi = SubjectUtil.tahminEt(kategori, metin)
            val gun = gunGirisi.ifBlank { gunAdiHesapla(tarih) }
            val yeniId = repo.blokEkle(
                Blok(
                    tarih = tarih, gun = gun, saatBaslangic = saatBaslangic, saatBitis = saatBitis,
                    kategori = kategori, dersAdi = dersAdi, metin = metin, sureDk = null
                )
            )
            repo.blokGetir(yeniId)?.let { NotificationScheduler.planlaBlok(getApplication(), it) }
        }
    }

    /** Giriş/kayıt ekranından hemen sonra çağrılır: buluttaki yedek varsa bu (taze) cihaza uygular. */
    suspend fun bulutGirisSonrasiYukle(): BulutProfil? {
        val uid = BulutHesap.uid() ?: return null
        val context = getApplication<Application>()
        // Giriş ve kayıt her zaman temiz kullanıcı alanından başlar; program seed'i korunur.
        repo.kullaniciVerisiniTemizle()
        yerelFotograflariTemizle(context)
        ProfilPrefs.cikisYap(context)
        HesapPrefs.uidKaydet(context, uid)
        val profil = BulutSenkron.indirVeUygula(repo)
        repo.eksikKartBonuslariniTamamla()
        repo.varsayilanAgaciEkleGerekirse()
        profil?.profilFotoUrl?.let { ProfilPrefs.fotoKaydet(context, null, it) }
        return profil
    }

    /** Profil fotoğrafı Profilim ekranından değiştirildiğinde bulut yedeğini de günceller. */
    fun avatarDegistiIsaretle() = bulutaYedekle()
    fun profilYedekle() = bulutaYedekle()

    fun profilFotografiDegistir(uri: Uri, tamamlandi: (String?) -> Unit = {}) {
        viewModelScope.launch {
            val url = BulutSenkron.profilFotografiYukle(uri)
            ProfilPrefs.fotoKaydet(getApplication(), uri.toString(), url)
            bulutaYedekle()
            tamamlandi(url)
        }
    }

    private val _sekmeTekrarOlayi = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val sekmeTekrarOlayi: SharedFlow<String> = _sekmeTekrarOlayi
    fun sekmeyeTekrarBasildi(route: String) { _sekmeTekrarOlayi.tryEmit(route) }

    private fun yerelFotograflariTemizle(context: Application) {
        File(context.filesDir, "notlar").deleteRecursively()
        File(context.filesDir, "profil").deleteRecursively()
    }

    private fun gunAdiHesapla(tarih: String): String {
        return try {
            val d = dateFmt.parse(tarih) ?: return ""
            SimpleDateFormat("EEEE", Locale("tr")).format(d)
        } catch (e: Exception) { "" }
    }
}
