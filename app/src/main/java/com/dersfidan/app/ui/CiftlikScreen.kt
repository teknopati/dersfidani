package com.dersfidan.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dersfidan.app.audio.SesYoneticisi
import com.dersfidan.app.R
import com.dersfidan.app.data.*
import com.dersfidan.app.viewmodel.AppViewModel
import kotlin.math.abs
import kotlinx.coroutines.delay

private enum class CiftlikSayfasi { ANA, HAYVANLAR, YAPILAR, AGACLAR, CALISANLAR, ISCI_AL, KARTLAR }

private data class CiftlikRenkleri(
    val zemin: Color,
    val yuzey: Color,
    val yumusakYuzey: Color,
    val metin: Color,
    val ikincilMetin: Color,
    val vurgu: Color,
    val mor: Color,
    val morUstu: Color,
    val konusma: Color
)

/** Çiftlik ekranları sistem temasını izler ama okunabilirliği kendi paletiyle korur. */
@Composable
private fun ciftlikRenkleri(): CiftlikRenkleri = if (isSystemInDarkTheme()) {
    CiftlikRenkleri(
        zemin = Color(0xFF17271F), yuzey = Color(0xFF2B4034), yumusakYuzey = Color(0xFF36533F),
        metin = Color(0xFFF4E8D3), ikincilMetin = Color(0xFFD3C7B3), vurgu = Color(0xFF80D681),
        mor = Color(0xFF8D6A93), morUstu = Color(0xFFFFF2D8), konusma = Color(0xFF514B34)
    )
} else {
    CiftlikRenkleri(
        zemin = Color(0xFFF3EBDD), yuzey = Color(0xFFFFF8EC), yumusakYuzey = Color(0xFFDDE9D2),
        metin = Color(0xFF3E382D), ikincilMetin = Color(0xFF665E50), vurgu = Color(0xFF78CE73),
        mor = Color(0xFFB86ACB), morUstu = Color(0xFFFFF8EC), konusma = Color(0xFFFFE9B8)
    )
}

/** Büyük bir harita yerine hafif, resimli kategori galerileri kullanan çiftlik merkezi. */
@Composable
fun CiftlikScreen(vm: AppViewModel) {
    val agaclar by vm.agaclar.collectAsState()
    val sonrakiAgacFiyati by vm.sonrakiAgacFiyati.collectAsState()
    val esyalar by vm.sahipOlunanCanlilar.collectAsState()
    val adetler by vm.canliAdetleri.collectAsState()
    val calisanlar by vm.ciftlikCalisanlari.collectAsState()
    val durum by vm.ciftlikDurumu.collectAsState()
    val kalanPuan by vm.kalanPuan.collectAsState()
    val marketMesaji by vm.marketMesaji.collectAsState()
    val sehir by vm.sehir.collectAsState()
    val hava by vm.havaDurumu.collectAsState()
    val gunSaati = rememberGunSaati()
    val snackbar = remember { SnackbarHostState() }
    var sayfa by remember { mutableStateOf(CiftlikSayfasi.ANA) }
    var marketAcik by remember { mutableStateOf(false) }
    var sehirAcik by remember { mutableStateOf(false) }
    var adDuzenle by remember { mutableStateOf(false) }
    var hedefEsyaId by remember { mutableStateOf<String?>(null) }
    var hedefCalisanTip by remember { mutableStateOf<Int?>(null) }

    BackHandler(enabled = marketAcik || sehirAcik || adDuzenle || sayfa != CiftlikSayfasi.ANA) {
        when {
            marketAcik -> marketAcik = false
            sehirAcik -> sehirAcik = false
            adDuzenle -> adDuzenle = false
            else -> sayfa = CiftlikSayfasi.ANA
        }
    }
    DisposableEffect(Unit) {
        vm.ciftlikAktif(true)
        SesYoneticisi.ciftlikMuzigiBaslat()
        onDispose { vm.ciftlikAktif(false); SesYoneticisi.ciftlikMuzigiDurdur() }
    }
    LaunchedEffect(marketMesaji) {
        marketMesaji?.let { snackbar.showSnackbar(it); vm.marketMesajiniTemizle() }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(snackbar) { veri ->
                Surface(
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = Color(0xFFF8E1B5), contentColor = Color(0xFF4A2917),
                    shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFD9A15C)), shadowElevation = 8.dp
                ) { Text(veri.visuals.message, Modifier.padding(horizontal = 16.dp, vertical = 12.dp), fontWeight = FontWeight.Bold) }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (sayfa == CiftlikSayfasi.ANA) {
                // Ana merkez ve galeriler yalnız Compose + 2B resim kullanır; SurfaceView yoktur.
                YeniCiftlikArkaPlan(hava = hava, saat = gunSaati)
                Column(Modifier.fillMaxSize()) {
                    CiftlikUstCubuk(
                        ad = durum?.ad ?: "Çiftliğim", sehir = sehir, hava = hava, puan = kalanPuan,
                        geri = null, onAd = { adDuzenle = true }, onHava = { sehirAcik = true }
                    )
                    CiftlikMenu(
                        hayvan = esyalar.count { MarketKatalog.bul(it.esyaId)?.tur == EsyaTuru.HAYVAN },
                        yapi = esyalar.count { MarketKatalog.bul(it.esyaId)?.tur == EsyaTuru.YAPI },
                        agac = agaclar.size, calisan = calisanlar.size,
                        onSec = { sayfa = it }, onMarket = { marketAcik = true }, modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(Modifier.fillMaxSize().background(ciftlikRenkleri().zemin)) {
                    CiftlikUstCubuk(
                        ad = durum?.ad ?: "Çiftliğim", sehir = sehir, hava = hava, puan = kalanPuan,
                        geri = { sayfa = CiftlikSayfasi.ANA }, onAd = { adDuzenle = true }, onHava = { sehirAcik = true }
                    )
                    when (sayfa) {
                        CiftlikSayfasi.HAYVANLAR -> HayvanGalerisi(esyalar, kalanPuan, durum?.yemAdedi ?: 0, durum?.ad ?: "Çiftliğim", hedefEsyaId, vm) { marketAcik = true }
                        CiftlikSayfasi.YAPILAR -> YapiGalerisi(esyalar, kalanPuan, hedefEsyaId, vm) { marketAcik = true }
                        CiftlikSayfasi.AGACLAR -> AgacGalerisi(agaclar, kalanPuan, vm) { marketAcik = true }
                        CiftlikSayfasi.CALISANLAR -> CalisanGalerisi(calisanlar, durum ?: CiftlikDurumu(), kalanPuan, hedefCalisanTip, vm) { sayfa = CiftlikSayfasi.ISCI_AL }
                        CiftlikSayfasi.ISCI_AL -> IsciIseAlEkrani(calisanlar, durum ?: CiftlikDurumu(), kalanPuan, vm)
                        CiftlikSayfasi.KARTLAR -> KartlarimEkrani(esyalar, calisanlar,
                            onHayvan = { hedefEsyaId = it; sayfa = CiftlikSayfasi.HAYVANLAR },
                            onYapi = { hedefEsyaId = it; sayfa = CiftlikSayfasi.YAPILAR },
                            onCalisan = { hedefCalisanTip = it; sayfa = CiftlikSayfasi.CALISANLAR })
                        else -> Unit
                    }
                }
            }
        }
    }

    if (marketAcik) MarketSheet(kalanPuan, adetler, durum?.yemAdedi ?: 0, sonrakiAgacFiyati, vm::agacEkle,
        { esya, ad -> vm.canliSatinAl(esya, ad) }, vm::yemSatinAl, { marketAcik = false })
    if (sehirAcik) MetinDialog("Hava durumu şehri", sehir, "Şehir", { sehirAcik = false }) {
        vm.sehirDegistir(it); sehirAcik = false
    }
    if (adDuzenle) MetinDialog("Çiftliğinin adı", durum?.ad ?: "Çiftliğim", "Çiftlik adı", { adDuzenle = false }) {
        vm.ciftlikAdiniDegistir(it); adDuzenle = false
    }
}

@Composable
private fun YeniCiftlikArkaPlan(hava: HavaSonucu?, saat: GunSaati) {
    val gece = saat.gece
    val yagmur = hava?.tur == HavaTuru.YAGMURLU || hava?.tur == HavaTuru.FIRTINALI
    val kar = hava?.tur == HavaTuru.KARLI
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.farm_valley_hd), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (gece) Box(Modifier.fillMaxSize().background(Color(0x88203A61)))
        else if (hava?.tur == HavaTuru.BULUTLU) Box(Modifier.fillMaxSize().background(Color(0x33748182)))
        if (yagmur || kar) Canvas(Modifier.fillMaxSize()) {
            repeat(70) { i ->
                val x = ((i * 79) % 100) / 100f * size.width
                val y = ((i * 47) % 100) / 100f * size.height
                if (kar) drawCircle(Color.White.copy(alpha = .85f), 4f + (i % 3), Offset(x, y))
                else drawLine(Color(0xAAE0F2FF), Offset(x, y), Offset(x - 5f, y + 20f), 2.3f)
            }
        }
        if (hava?.tur == HavaTuru.FIRTINALI) Box(Modifier.fillMaxSize().background(Color(0x332A3444)))
    }
}

/** Ana menü için SurfaceView kullanmayan, katman sorunu çıkarmayan sıcak çiftlik manzarası. */
@Composable
private fun CiftlikArkaPlan(hava: HavaSonucu?, saat: GunSaati) {
    val koyuTema = isSystemInDarkTheme()
    val gece = saat.gece
    val firtina = hava?.tur == HavaTuru.FIRTINALI
    val yagisli = hava?.tur == HavaTuru.YAGMURLU || firtina
    val karli = hava?.tur == HavaTuru.KARLI
    val gokUst = when {
        gece -> Color(0xFF243653)
        firtina -> Color(0xFF63717A)
        yagisli -> Color(0xFF8499A2)
        hava?.tur == HavaTuru.BULUTLU -> Color(0xFFB6C7CB)
        else -> Color(0xFF8FD0E8)
    }
    val gokAlt = when {
        gece -> Color(0xFF526477)
        yagisli -> Color(0xFFCCD3CF)
        else -> Color(0xFFFFE2A8)
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(gokUst, gokAlt)))) {
        Image(
            painter = painterResource(if (gece) R.drawable.garden_background_blue else R.drawable.garden_background_green),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                if (koyuTema) Color(0x3310231E)
                else Color(0x2298DFB0)
            )
        )
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Uzak tepeler ve bütün ekranı dolduran tek parça arazi.
            val uzakTepe = Path().apply {
                moveTo(0f, h * .25f)
                quadraticBezierTo(w * .18f, h * .12f, w * .38f, h * .27f)
                quadraticBezierTo(w * .62f, h * .10f, w, h * .24f)
                lineTo(w, h * .55f); lineTo(0f, h * .55f); close()
            }
            drawPath(uzakTepe, if (gece) Color(0xFF49634F) else Color(0xFF85A96F))

            val arazi = Path().apply {
                moveTo(0f, h * .36f); lineTo(w, h * .29f)
                lineTo(w, h); lineTo(0f, h); close()
            }
            drawPath(arazi, if (karli) Color(0xFFE9EEE8) else if (gece) Color(0xFF35583F) else Color(0xFF83B85C))

            // İzometrik tarla şeritleri ve kıvrılan toprak yol.
            val tarla = Path().apply {
                moveTo(w * .50f, h * .43f); lineTo(w * .96f, h * .35f)
                lineTo(w, h * .63f); lineTo(w * .59f, h * .70f); close()
            }
            drawPath(tarla, if (karli) Color(0xFFD8E3D9) else Color(0xFFA9C96A))
            repeat(5) { i ->
                val y = h * (.43f + i * .045f)
                drawLine(Color(0x553E6B38), Offset(w * (.50f + i * .018f), y), Offset(w, y - h * .08f), 3f)
            }
            val yol = Path().apply {
                moveTo(w * .42f, h); cubicTo(w * .32f, h * .80f, w * .58f, h * .69f, w * .47f, h * .49f)
                lineTo(w * .57f, h * .46f); cubicTo(w * .70f, h * .69f, w * .48f, h * .83f, w * .58f, h)
                close()
            }
            drawPath(yol, Color(0xFFD7B77E))

            // Ahır, silo ve ağaçlar: fotoğraf yerine uygulamayla uyumlu sabit illüstrasyon.
            ciftlikAgaci(w * .13f, h * .42f, w * .075f, gece, karli)
            ciftlikAgaci(w * .87f, h * .42f, w * .065f, gece, karli)
            ciftlikAgaci(w * .78f, h * .56f, w * .055f, gece, karli)
            ciftlikAhiri(w * .28f, h * .42f, w * .28f, gece)
            ciftlikSilosu(w * .70f, h * .39f, w * .10f, gece)

            if (!gece && hava?.tur == HavaTuru.ACIK) {
                drawCircle(Color(0xFFFFD35D), w * .07f, Offset(w * .82f, h * .13f))
                drawCircle(Color(0x55FFF1A6), w * .105f, Offset(w * .82f, h * .13f))
            }
            if (gece) {
                drawCircle(Color(0xFFFFF1C4), w * .055f, Offset(w * .82f, h * .13f))
                repeat(18) { i ->
                    val x = ((i * 71) % 100) / 100f * w
                    val y = (.04f + ((i * 37) % 22) / 100f) * h
                    drawCircle(Color(0xCCFFF4CF), 2.2f, Offset(x, y))
                }
            }
            if (hava?.tur == HavaTuru.BULUTLU || yagisli || karli) {
                repeat(if (yagisli) 5 else 3) { i ->
                    val x = w * (.08f + i * .22f)
                    val y = h * (.10f + (i % 2) * .045f)
                    val renk = if (yagisli) Color(0xCC596773) else Color(0xDDE9ECE8)
                    drawCircle(renk, w * .07f, Offset(x, y))
                    drawCircle(renk, w * .09f, Offset(x + w * .07f, y + 5f))
                    drawCircle(renk, w * .06f, Offset(x + w * .14f, y + 8f))
                }
            }
            if (yagisli) repeat(32) { i ->
                val x = ((i * 83) % 100) / 100f * w
                val y = h * (.18f + ((i * 47) % 42) / 100f)
                drawLine(Color(0xAAE1EFF4), Offset(x, y), Offset(x - 5f, y + 19f), 2f)
            }

            // Menüye yaklaşırken manzarayı yumuşatır; keskin yatay ayrımı ortadan kaldırır.
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0x55F7F1E5))), topLeft = Offset(0f, h * .50f))
        }
    }
}

private fun DrawScope.ciftlikAgaci(x: Float, y: Float, r: Float, gece: Boolean, karli: Boolean) {
    drawRoundRect(Color(0xFF745237), Offset(x - r * .13f, y), androidx.compose.ui.geometry.Size(r * .26f, r * 1.25f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f))
    val yesil = when { karli -> Color(0xFFE8EFE9); gece -> Color(0xFF31523C); else -> Color(0xFF4F8A45) }
    drawCircle(yesil, r, Offset(x, y - r * .35f))
    drawCircle(yesil.copy(alpha = .92f), r * .78f, Offset(x - r * .58f, y - r * .12f))
    drawCircle(yesil.copy(alpha = .96f), r * .72f, Offset(x + r * .60f, y - r * .08f))
}

private fun DrawScope.ciftlikAhiri(x: Float, y: Float, genislik: Float, gece: Boolean) {
    val yukseklik = genislik * .55f
    drawRoundRect(if (gece) Color(0xFF7B4D42) else Color(0xFFC85C4E), Offset(x, y), androidx.compose.ui.geometry.Size(genislik, yukseklik), cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f))
    val cati = Path().apply { moveTo(x - genislik * .08f, y); lineTo(x + genislik * .50f, y - yukseklik * .55f); lineTo(x + genislik * 1.08f, y); close() }
    drawPath(cati, if (gece) Color(0xFF473B38) else Color(0xFF6B4A3A))
    drawRoundRect(Color(0xFFF4D49B), Offset(x + genislik * .37f, y + yukseklik * .25f), androidx.compose.ui.geometry.Size(genislik * .26f, yukseklik * .75f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f))
    drawLine(Color(0xFFC85C4E), Offset(x + genislik * .50f, y + yukseklik * .25f), Offset(x + genislik * .50f, y + yukseklik), 5f)
}

private fun DrawScope.ciftlikSilosu(x: Float, y: Float, genislik: Float, gece: Boolean) {
    val govde = if (gece) Color(0xFF8C9693) else Color(0xFFD8DDD4)
    drawRoundRect(govde, Offset(x, y), androidx.compose.ui.geometry.Size(genislik, genislik * 1.65f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(genislik * .35f))
    val cati = Path().apply { moveTo(x - genislik * .08f, y + 4f); lineTo(x + genislik * .5f, y - genislik * .55f); lineTo(x + genislik * 1.08f, y + 4f); close() }
    drawPath(cati, Color(0xFF765244))
}

@Composable
private fun CiftlikUstCubuk(ad: String, sehir: String, hava: HavaSonucu?, puan: Int, geri: (() -> Unit)?, onAd: () -> Unit, onHava: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 7.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (geri != null) OyunYonButonu(false, true, Modifier.size(54.dp), geri)
        Box(Modifier.weight(1f).height(58.dp).clickable(onClick = onAd)) {
            Image(painterResource(R.drawable.ui_farm_habitat_sign), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                Text(ad, color = Color(0xFF4A2917), fontWeight = FontWeight.Black, maxLines = 1)
                Spacer(Modifier.width(4.dp)); Icon(Icons.Default.Edit, null, tint = Color(0xFF6B3D22), modifier = Modifier.size(15.dp))
            }
        }
        Spacer(Modifier.width(5.dp))
        Surface(
            Modifier.clickable(onClick = onHava), color = Color(0xFF744124), contentColor = Color(0xFFFFE7B8),
            shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFD9A15C)), shadowElevation = 5.dp
        ) { Text("${havaSimgesi(hava)} ${hava?.sicaklikC?.let { "$it°" } ?: sehir}", Modifier.padding(horizontal = 8.dp, vertical = 9.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) }
        Spacer(Modifier.width(5.dp))
        Surface(color = Color(0xFF744124), contentColor = Color(0xFFFFE7B8), shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFD9A15C)), shadowElevation = 5.dp) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.garden_coin), null, Modifier.size(22.dp)); Text(" $puan", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun CiftlikMenu(hayvan: Int, yapi: Int, agac: Int, calisan: Int, onSec: (CiftlikSayfasi) -> Unit, onMarket: () -> Unit, modifier: Modifier = Modifier) {
    val r = ciftlikRenkleri()
    BoxWithConstraints(
        modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.Transparent, r.zemin.copy(alpha = .28f), r.zemin.copy(alpha = .82f)))
        )
    ) {
        val bosluk = 8.dp
        val kartYuksekligi = ((maxHeight - bosluk * 3f - 16.dp) / 4f).coerceIn(86.dp, 124.dp)
        Column(
            Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(bosluk)
        ) {
            Row(Modifier.height(kartYuksekligi), horizontalArrangement = Arrangement.spacedBy(bosluk)) {
                MenuKarti("🐾", "Hayvanlarım", "$hayvan hayvan", Modifier.weight(1f).fillMaxHeight()) { onSec(CiftlikSayfasi.HAYVANLAR) }
                MenuKarti("🏠", "Yapılarım", "$yapi yapı", Modifier.weight(1f).fillMaxHeight()) { onSec(CiftlikSayfasi.YAPILAR) }
            }
            Row(Modifier.height(kartYuksekligi), horizontalArrangement = Arrangement.spacedBy(bosluk)) {
                MenuKarti("🌳", "Ağaçlarım", "$agac ağaç", Modifier.weight(1f).fillMaxHeight()) { onSec(CiftlikSayfasi.AGACLAR) }
                MenuKarti("🧑‍🌾", "Çalışanlar", "$calisan/5 çiftçi", Modifier.weight(1f).fillMaxHeight()) { onSec(CiftlikSayfasi.CALISANLAR) }
            }
            Row(Modifier.height(kartYuksekligi), horizontalArrangement = Arrangement.spacedBy(bosluk)) {
                MenuKarti("🛒", "Market", "Hayvan, yapı, ağaç", Modifier.weight(1f).fillMaxHeight(), onMarket)
                MenuKarti("🤝", "İşçi işe al", "Yeni çiftçi", Modifier.weight(1f).fillMaxHeight()) { onSec(CiftlikSayfasi.ISCI_AL) }
            }
            MenuKarti("🎴", "Kartlarım", "Sahip oldukların ve koleksiyon", Modifier.fillMaxWidth().height(kartYuksekligi)) { onSec(CiftlikSayfasi.KARTLAR) }
        }
    }
}

@Composable
private fun MenuKarti(simge: String, baslik: String, alt: String, modifier: Modifier, onClick: () -> Unit) {
    val ahsap = Color(0xE66A3D24)
    Surface(
        modifier.clickable(onClick = onClick), color = ahsap, contentColor = Color(0xFFFFE9C3),
        shape = RoundedCornerShape(22.dp), shadowElevation = 9.dp,
        border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFD9A15C))
    ) {
        Box {
            Box(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp).padding(bottom = 36.dp).clip(RoundedCornerShape(16.dp))) {
                when (baslik) {
                    "Hayvanlarım" -> AnimalPortrait("kedi", 1, Modifier.fillMaxSize())
                    "Yapılarım" -> BuildingPortrait("ahir", Modifier.fillMaxSize())
                    "Ağaçlarım" -> TreePortrait(0, Modifier.fillMaxSize())
                    "Çalışanlar" -> FarmerPortrait(0, Modifier.fillMaxSize())
                    "İşçi işe al" -> FarmerPortrait(1, Modifier.fillMaxSize())
                    "Market" -> BuildingPortrait("saat_kulesi", Modifier.fillMaxSize())
                    else -> AnimalPortrait("kus", 1, Modifier.fillMaxSize())
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x22000000)))))
            }
            Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(horizontal = 6.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(baslik, color = Color(0xFFFFF1D2), fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 1, style = MaterialTheme.typography.titleSmall)
                Text(alt, style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD9A0), textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HayvanGalerisi(hepsi: List<Canli>, puan: Int, yemAdedi: Int, ciftlikAdi: String, hedefEsyaId: String?, vm: AppViewModel, onMarket: () -> Unit) {
    val hayvanlar = hepsi.filter { MarketKatalog.bul(it.esyaId)?.tur == EsyaTuru.HAYVAN }
    if (hayvanlar.isEmpty()) return BosKategori("Henüz hayvanın yok. Market'ten ilk hayvanını alabilirsin.")
    val yazi = Color(0xFF4A2917)
    val yesil = Color(0xFF69C43F)
    var index by remember(hayvanlar.size) { mutableIntStateOf(0) }
    var duzenle by remember { mutableStateOf(false) }
    var habitatAcik by remember { mutableStateOf(false) }
    var soz by remember { mutableIntStateOf(0) }
    var kalpGoster by remember { mutableStateOf(false) }
    LaunchedEffect(hedefEsyaId, hayvanlar.size) { hedefEsyaId?.let { id -> hayvanlar.indexOfFirst { it.esyaId == id }.takeIf { it >= 0 }?.let { index = it } } }
    val hayvan = hayvanlar[index.coerceIn(0, hayvanlar.lastIndex)]; val item = MarketKatalog.bul(hayvan.esyaId) ?: return
    val sakinler = hayvanlar.filter { it.esyaId == hayvan.esyaId }
    val habitatSeviye = sakinler.firstOrNull()?.seviye ?: 1
    val habitatHedef = Repository.yapiSeviyeMaliyeti(habitatSeviye)
    val habitatIlerleme = sakinler.firstOrNull()?.seviyeIlerlemePuan ?: 0
    val kapasite = (1 + habitatSeviye / 2).coerceAtMost(5)
    val tokluk = Repository.guncelTokluk(hayvan)
    val sevgi = Repository.guncelSevgi(hayvan)
    val ruhHali = Repository.ruhHali(sevgi, tokluk)
    val sozler = when {
        tokluk < 20 -> listOf("Karnım çok acıktı patron...", "$ciftlikAdi güzel ama mama kabım boş.", "Bana biraz ${hayvanYemGorunumu(hayvan.esyaId)} verir misin?")
        sevgi < 25 -> listOf("Biraz yalnız hissediyorum...", "Beni sever misin patron?", "$ciftlikAdi içinde seni özledim.")
        sevgi < 60 -> listOf("Bugün çiftliğimiz sakin.", "Yanımda olman güzel patron.", "$ciftlikAdi her gün güzelleşiyor!")
        else -> listOf("$ciftlikAdi çok güzel patron!", "Beni ziyaret ettiğin için çok mutluyum!", "Bugün oyun oynamaya hazırım!", "Sen en iyi çiftlik sahibisin!")
    }
    val hedef = Repository.hayvanYasMaliyeti(hayvan.yas)

    DisposableEffect(Unit) { onDispose { SesYoneticisi.hayvanSesiDurdur() } }
    LaunchedEffect(index) { SesYoneticisi.hayvanSesiDurdur() }
    LaunchedEffect(kalpGoster) { if (kalpGoster) { delay(850); kalpGoster = false } }
    val hayvanYukseklik by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (kalpGoster) -22f else 0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ), label = "hayvan ziplama"
    )
    val hayvanDonus by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (kalpGoster) 4f else 0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
        ), label = "hayvan tepki"
    )

    Box(Modifier.fillMaxSize().pointerInput(index, hayvanlar.size) {
            var toplam = 0f
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (abs(toplam) > 70f) {
                        index = if (toplam < 0) (index + 1).coerceAtMost(hayvanlar.lastIndex)
                        else (index - 1).coerceAtLeast(0)
                    }
                    toplam = 0f; soz = 0
                },
                onHorizontalDrag = { _, d -> toplam += d }
            )
        }) {
        Image(painterResource(R.drawable.farm_bg_animals_clean), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Column(Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(.76f).aspectRatio(3f)) {
                Image(painterResource(R.drawable.ui_farm_name_banner), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                Column(Modifier.align(Alignment.Center).padding(top = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(hayvan.ad, color = yazi, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.isim, color = yazi, fontWeight = FontWeight.ExtraBold)
                        IconButton({ duzenle = true }, Modifier.size(28.dp)) { Icon(Icons.Default.Edit, "Adını değiştir", tint = yazi, modifier = Modifier.size(18.dp)) }
                    }
                }
            }

            Box(Modifier.fillMaxWidth().weight(1f)) {
                val varyant = sakinler.indexOfFirst { it.id == hayvan.id }.coerceAtLeast(0) + 1
                AnimalPortrait(
                    hayvan.esyaId, varyant,
                    Modifier.align(Alignment.Center).fillMaxWidth(.80f).fillMaxHeight(.92f)
                        .clickable { vm.hayvaniSev(hayvan); kalpGoster = true; soz++ }
                        .graphicsLayer {
                            translationY = hayvanYukseklik; rotationZ = hayvanDonus
                            scaleX = if (kalpGoster) 1.035f else 1f; scaleY = if (kalpGoster) 1.035f else 1f
                        },
                    age = hayvan.yas
                )

                Box(Modifier.align(Alignment.TopEnd).size(98.dp)) {
                    Image(painterResource(R.drawable.ui_farm_age_paw), null, Modifier.fillMaxSize())
                    Column(Modifier.align(Alignment.Center).padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Yaş", color = yazi, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium)
                        Text("${hayvan.yas}", color = yazi, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
                    }
                }

                OyunYonButonu(false, index > 0, Modifier.align(Alignment.CenterStart)) { index--; soz = 0 }
                OyunYonButonu(true, index < hayvanlar.lastIndex, Modifier.align(Alignment.CenterEnd)) { index++; soz = 0 }

                Box(
                    Modifier.align(Alignment.TopEnd).padding(top = 104.dp, end = 8.dp).width(112.dp).aspectRatio(1.55f)
                        .clickable { SesYoneticisi.hayvanSesiCal(hayvan.esyaId); soz++ }
                ) {
                    Image(painterResource(R.drawable.ui_farm_speech), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                    Column(Modifier.align(Alignment.Center).padding(horizontal = 14.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Merhaba de", color = yazi, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
                        Text("♥", color = Color(0xFFE55555), style = MaterialTheme.typography.titleMedium)
                    }
                }

                Box(
                    Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 4.dp).width(190.dp).aspectRatio(3f)
                        .clickable { habitatAcik = true }
                ) {
                    Image(painterResource(R.drawable.ui_farm_habitat_sign), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(MarketKatalog.habitatAdi(hayvan.esyaId), color = yazi, fontWeight = FontWeight.Black, maxLines = 1)
                        Text("Seviye $habitatSeviye · ${sakinler.size}/$kapasite", color = yazi, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
                if (kalpGoster) Text("♥", Modifier.align(Alignment.Center).graphicsLayer { translationY = -42f }, color = Color(0xFFE94F71), fontSize = 46.sp)
            }

            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                color = Color(0xEFFFF0CE), contentColor = yazi, shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFD09A55)), shadowElevation = 4.dp
            ) {
                Text(sozler[soz % sozler.size], Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, maxLines = 2)
            }
            Box(Modifier.fillMaxWidth().height(210.dp)) {
                Image(painterResource(R.drawable.ui_farm_upgrade_panel), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$ruhHali", Modifier.weight(1f), color = yazi, fontWeight = FontWeight.ExtraBold)
                        Text("${hayvan.yasIlerlemePuan}/$hedef", color = yazi, fontWeight = FontWeight.Black)
                    }
                    LinearProgressIndicator(
                        progress = (hayvan.yasIlerlemePuan.toFloat() / hedef.coerceAtLeast(1)).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth().height(13.dp).clip(RoundedCornerShape(50)), color = yesil, trackColor = Color(0xFF6D3E22)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OyunBakimCubugu("Tokluk", tokluk, Color(0xFFF0AC37), Modifier.weight(1f))
                        OyunBakimCubugu("Sevgi", sevgi, Color(0xFFEB5278), Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            { vm.hayvanBesle(hayvan); kalpGoster = true; soz++ }, Modifier.weight(.72f).height(42.dp), enabled = yemAdedi > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8B85C), contentColor = yazi), shape = RoundedCornerShape(18.dp)
                        ) { Text("${hayvanYemGorunumu(hayvan.esyaId)} Besle · $yemAdedi", fontWeight = FontWeight.Black, maxLines = 1, fontSize = 11.sp) }
                        if (hayvan.yas < 20) OyunPuanKontrolu(puan, hedef - hayvan.yasIlerlemePuan, Modifier.weight(1.65f)) { vm.hayvanYasPuaniYatir(hayvan, it) }
                        else Text("En yüksek yaş", Modifier.weight(1.65f), color = yazi, textAlign = TextAlign.Center, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        Surface(
            Modifier.align(Alignment.TopEnd).padding(8.dp).clickable(onClick = onMarket),
            color = Color(0xFF744124), contentColor = Color(0xFFFFE7B8), shape = RoundedCornerShape(18.dp), shadowElevation = 8.dp
        ) { Text("Market", Modifier.padding(horizontal = 14.dp, vertical = 9.dp), fontWeight = FontWeight.Black) }
    }
    if (duzenle) MetinDialog("Hayvanın adı", hayvan.ad, "Ad", { duzenle = false }) { vm.canliAdiniDegistir(hayvan.id, it); duzenle = false }
    if (habitatAcik) {
        ModalBottomSheet(
            onDismissRequest = { habitatAcik = false },
            containerColor = Color(0xFFF5DDAF), contentColor = Color(0xFF4A2917),
            dragHandle = { Box(Modifier.padding(top = 8.dp).width(72.dp).height(6.dp).clip(RoundedCornerShape(50)).background(Color(0xFF8A512D))) }
        ) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(.78f).aspectRatio(3f)) {
                    Image(painterResource(R.drawable.ui_farm_name_banner), null, Modifier.fillMaxSize())
                    Text(MarketKatalog.habitatAdi(hayvan.esyaId), Modifier.align(Alignment.Center), color = Color(0xFF4A2917), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
                Text("Bu evde ${sakinler.size} hayvan yaşıyor: ${sakinler.joinToString { it.ad }}", color = Color(0xFF4A2917))
                Text("Habitat seviyesi $habitatSeviye/10 · Kapasite ${sakinler.size}/$kapasite", color = Color(0xFF4A2917), fontWeight = FontWeight.Bold)
                if (habitatSeviye < 10) {
                    Ilerleme(habitatIlerleme, habitatHedef, "Habitat genişletme")
                    PuanButonlari(puan, habitatHedef - habitatIlerleme) { vm.habitatPuaniYatir(hayvan.esyaId, it) }
                    Text("Her iki habitat seviyesinde bir yeni hayvan yeri açılır.", color = Color(0xFF6B442A), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun OyunYonButonu(sag: Boolean, etkin: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Image(
        painterResource(R.drawable.ui_farm_arrow_left), if (sag) "Sonraki" else "Önceki",
        modifier.size(66.dp).graphicsLayer { rotationZ = if (sag) 180f else 0f; alpha = if (etkin) 1f else .35f }
            .clickable(enabled = etkin, onClick = onClick), contentScale = ContentScale.Fit
    )
}

@Composable
private fun OyunBakimCubugu(ad: String, deger: Int, renk: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row { Text(ad, Modifier.weight(1f), color = Color(0xFF4A2917), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall); Text("$deger", color = Color(0xFF4A2917), fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall) }
        LinearProgressIndicator(deger / 100f, Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)), color = renk, trackColor = Color(0x55A77745))
    }
}

@Composable
private fun OyunPuanKontrolu(bakiye: Int, kalan: Int, modifier: Modifier = Modifier, onYatir: (Int) -> Unit) {
    val ust = minOf(bakiye.coerceAtLeast(0), kalan.coerceAtLeast(0))
    var miktar by remember(ust) { mutableIntStateOf(if (ust > 0) 1 else 0) }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        OyunYuvarlakDugme("−", miktar > 1) { miktar-- }
        Text("$miktar", Modifier.width(28.dp), color = Color(0xFF4A2917), textAlign = TextAlign.Center, fontWeight = FontWeight.Black)
        OyunYuvarlakDugme("+", miktar < ust) { miktar++ }
        Button(
            { if (miktar > 0) onYatir(miktar) }, enabled = miktar in 1..ust,
            modifier = Modifier.weight(1f).height(40.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF69C43F)), shape = RoundedCornerShape(15.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) { Text("Yükselt", fontWeight = FontWeight.Black, maxLines = 1, fontSize = 11.sp) }
    }
}

@Composable
private fun OyunYuvarlakDugme(metin: String, etkin: Boolean, onClick: () -> Unit) {
    Surface(
        Modifier.size(34.dp).clickable(enabled = etkin, onClick = onClick), shape = androidx.compose.foundation.shape.CircleShape,
        color = if (etkin) Color(0xFF8A512D) else Color(0x668A512D), contentColor = Color(0xFFFFE9C4),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFD9A15C)), shadowElevation = 3.dp
    ) { Box(contentAlignment = Alignment.Center) { Text(metin, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge) } }
}

@Composable
private fun BakimGostergesi(ad: String, deger: Int, renk: Color, r: CiftlikRenkleri) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(ad, color = r.ikincilMetin, modifier = Modifier.width(58.dp), style = MaterialTheme.typography.labelMedium)
        LinearProgressIndicator(
            progress = deger.coerceIn(0, 100) / 100f,
            modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(50)),
            color = renk,
            trackColor = r.yumusakYuzey
        )
        Text(" $deger", color = r.metin, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
    }
}

/** Market tek ortak yem satar; hayvan ekranında türe uygun sunum gösterilir. */
private fun hayvanYemGorunumu(esyaId: String): String = when (esyaId) {
    "kedi", "kopek", "tilki", "aslan" -> "🥣"
    "kus", "karga", "kartal", "baykus", "papagan", "tavuk", "civciv", "hindi", "ordek", "flamingo" -> "🌾"
    "tavsan", "fare", "sincap", "kaplumbaga" -> "🥕"
    "panda" -> "🎋"
    "maymun" -> "🍌"
    "ari", "kelebek", "ugurbocegi" -> "🌼"
    "kurbaga" -> "🪰"
    else -> "🌿"
}

@Composable
private fun YapiGalerisi(hepsi: List<Canli>, puan: Int, hedefEsyaId: String?, vm: AppViewModel, onMarket: () -> Unit) {
    val yapilar = hepsi.filter { MarketKatalog.bul(it.esyaId)?.tur == EsyaTuru.YAPI }
    if (yapilar.isEmpty()) return BosKategori("Henüz yapın yok. Market'ten bir yapı satın alabilirsin.")
    var index by remember(yapilar.size) { mutableIntStateOf(0) }
    LaunchedEffect(hedefEsyaId, yapilar.size) { hedefEsyaId?.let { id -> yapilar.indexOfFirst { it.esyaId == id }.takeIf { it >= 0 }?.let { index = it } } }
    val yapi = yapilar[index.coerceIn(0, yapilar.lastIndex)]; val item = MarketKatalog.bul(yapi.esyaId) ?: return
    val hedef = Repository.yapiSeviyeMaliyeti(yapi.seviye)
    GaleriIskeleti(index, yapilar.size, { index = it }, item.isim, "Market", onMarket, R.drawable.farm_bg_buildings,
        sahne = { BuildingPortrait(item.id, Modifier.fillMaxSize()) },
        detay = { Text("Seviye ${yapi.seviye}/10 · ${item.aciklama}", color=Color(0xFF4A2917), fontWeight = FontWeight.SemiBold); if (yapi.seviye < 10) { Ilerleme(yapi.seviyeIlerlemePuan, hedef, "Sonraki seviye"); PuanButonlari(puan, hedef-yapi.seviyeIlerlemePuan) { vm.yapiPuaniYatir(yapi,it) } } })
}

@Composable
private fun AgacGalerisi(agaclar: List<Agac>, puan: Int, vm: AppViewModel, onMarket: () -> Unit) {
    if (agaclar.isEmpty()) return BosKategori("Henüz ağacın yok.")
    var index by remember(agaclar.size) { mutableIntStateOf(0) }; val agac = agaclar[index.coerceIn(0, agaclar.lastIndex)]; val d = Repository.hesaplaAgac(agac.yatirilanPuan); val hedef = d.sonrakiEsik ?: d.sonrakiMeyveEsigi
    GaleriIskeleti(index, agaclar.size, { index = it }, agac.isim, "Market", onMarket, R.drawable.farm_bg_orchard,
        sahne = { TreePortrait(agac.turId, Modifier.fillMaxSize(), d.asamaIndex) },
        detay = { Text("${d.asamaAdi} · ${d.meyveSayisi} meyve", color=Color(0xFF4A2917), fontWeight = FontWeight.SemiBold); Ilerleme(agac.yatirilanPuan, hedef, "Büyüme hedefi"); PuanButonlari(puan, hedef-agac.yatirilanPuan) { vm.agacaPuanYatir(agac,it) } })
}

@Composable
private fun CalisanGalerisi(calisanlar: List<CiftlikCalisani>, durum: CiftlikDurumu, puan: Int, hedefTip: Int?, vm: AppViewModel, onIsciAl: () -> Unit) {
    var index by remember(calisanlar.size) { mutableIntStateOf(0) }; var duzenle by remember { mutableStateOf(false) }; var soz by remember { mutableIntStateOf(0) }
    var elSalliyor by remember { mutableStateOf(false) }
    LaunchedEffect(elSalliyor) { if (elSalliyor) { delay(900); elSalliyor = false } }
    LaunchedEffect(hedefTip, calisanlar.size) { hedefTip?.let { tip -> calisanlar.indexOfFirst { it.tip == tip }.takeIf { it >= 0 }?.let { index = it } } }
    val evHedef = Repository.calisanEviMaliyeti(durum.calisanEviSeviye)
    if (calisanlar.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            Image(painterResource(R.drawable.farm_bg_workers),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
            Column(Modifier.fillMaxSize().padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally){
                Box(Modifier.fillMaxWidth(.72f).aspectRatio(3f)){Image(painterResource(R.drawable.ui_farm_name_banner),null,Modifier.fillMaxSize());Text("Çalışan Evi",Modifier.align(Alignment.Center),color=Color(0xFF4A2917),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)}
                BuildingPortrait("misafir_evi",Modifier.fillMaxWidth(.75f).weight(1f))
                Box(Modifier.fillMaxWidth().height(230.dp)){Image(painterResource(R.drawable.ui_farm_upgrade_panel),null,Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds);Column(Modifier.fillMaxSize().padding(horizontal=36.dp,vertical=30.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Seviye ${durum.calisanEviSeviye}/5",color=Color(0xFF4A2917),fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);Text("Her ev seviyesi bir çiftçiye yer açar.",color=Color(0xFF4A2917));if(durum.calisanEviSeviye<5){Ilerleme(durum.calisanEviIlerlemePuan,evHedef,"Ev seviyesi");PuanButonlari(puan,evHedef-durum.calisanEviIlerlemePuan){vm.calisanEviPuaniYatir(it)}};Button(onIsciAl,Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF69C43F)),shape=RoundedCornerShape(18.dp)){Text("İşçi işe al",fontWeight=FontWeight.Black)}}}
            }
        }
    } else {
        val c = calisanlar[index.coerceIn(0, calisanlar.lastIndex)]; val hedef = Repository.calisanMaliyeti(c.seviye)
        val sozler = listOf("${durum.ad} için işler yolunda patron!", "Hayvanlarla ben ilgilenirim!", "Bugün çiftlik çok canlı görünüyor!", "Biraz çalışıp sonra dinleneceğim.")
        GaleriIskeleti(index, calisanlar.size, { index = it; soz = 0 }, c.ad, "İşçi Al", onIsciAl, R.drawable.farm_bg_workers,
            sahne = {
                FarmerPortrait(
                    c.tip,
                    Modifier.fillMaxSize().clickable { soz++; elSalliyor = true },
                    waving = elSalliyor
                )
            },
            detay = {
                Row(verticalAlignment = Alignment.CenterVertically) { Text("${c.rol} · Seviye ${c.seviye}/10",color=Color(0xFF4A2917), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); TextButton({duzenle=true}) { Icon(Icons.Default.Edit,"Ad",tint=Color(0xFF4A2917),modifier=Modifier.size(17.dp)); Spacer(Modifier.width(3.dp)); Text("Adı değiştir",color=Color(0xFF6B442A),style=MaterialTheme.typography.labelSmall) } }
                Text(sozler[soz % sozler.size],Modifier.fillMaxWidth().clickable{soz++},color=Color(0xFF4A2917),textAlign=TextAlign.Center,fontWeight=FontWeight.Bold)
                if (c.seviye < 10) { Ilerleme(c.ilerlemePuan, hedef, "Çalışan seviyesi"); PuanButonlari(puan, hedef-c.ilerlemePuan) { vm.calisanPuaniYatir(c,it) } }
                HorizontalDivider(color=Color(0x55905C34)); Text("Çalışan Evi · Seviye ${durum.calisanEviSeviye}/5", color=Color(0xFF4A2917),fontWeight = FontWeight.Black)
                if (durum.calisanEviSeviye < 5) { Ilerleme(durum.calisanEviIlerlemePuan, evHedef, "Ev seviyesi"); PuanButonlari(puan, evHedef-durum.calisanEviIlerlemePuan) { vm.calisanEviPuaniYatir(it) } }
                Text("Yeni çalışan alımı İşçi işe al bölümündedir.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B442A))
            })
        if (duzenle) MetinDialog("Çalışanın adı", c.ad, "Ad", {duzenle=false}) { vm.calisanAdiniDegistir(c.id,it); duzenle=false }
    }
}

@Composable
private fun IsciIseAlEkrani(calisanlar: List<CiftlikCalisani>, durum: CiftlikDurumu, puan: Int, vm: AppViewModel) {
    var adDialog by remember { mutableStateOf(false) }
    val siradaki = calisanlar.size.coerceIn(0, 4)
    val kapasiteVar = calisanlar.size < durum.calisanEviSeviye && calisanlar.size < 5
    val evHedef = Repository.calisanEviMaliyeti(durum.calisanEviSeviye)
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.farm_bg_workers), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth(.72f).aspectRatio(3f)) {
                Image(painterResource(R.drawable.ui_farm_name_banner), null, Modifier.fillMaxSize())
                Text("İşçi işe al", Modifier.align(Alignment.Center), color = Color(0xFF4A2917), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }
                if (calisanlar.size < 5) {
                    FarmerPortrait(siradaki, Modifier.fillMaxWidth(.78f).weight(1f))
                    Box(Modifier.fillMaxWidth().height(if (kapasiteVar) 230.dp else 282.dp)) {
                        Image(painterResource(R.drawable.ui_farm_upgrade_panel), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 36.dp, end = 36.dp, top = 38.dp, bottom = 30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(ciftciVarsayilanAdi(calisanlar.size), color = Color(0xFF4A2917), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Text("Her çiftçinin görünümü ve görevi farklıdır.", color = Color(0xFF4A2917), textAlign = TextAlign.Center)
                            Text(if (kapasiteVar) "Ev kapasitesi: ${calisanlar.size}/${durum.calisanEviSeviye}" else "Çalışan evi dolu; önce evi yükselt.", color = if (kapasiteVar) Color(0xFF3D6F2E) else Color(0xFF9A3328), fontWeight = FontWeight.Bold)
                            if (!kapasiteVar && durum.calisanEviSeviye < 5) {
                                Ilerleme(durum.calisanEviIlerlemePuan, evHedef, "Çalışan evi")
                                PuanButonlari(puan, evHedef - durum.calisanEviIlerlemePuan) { vm.calisanEviPuaniYatir(it) }
                            }
                            Button({ adDialog = true }, enabled = kapasiteVar && puan >= 40, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF69C43F)), shape = RoundedCornerShape(18.dp)) { Text("İşe al · 40 puan", fontWeight = FontWeight.Black) }
                        }
                    }
                } else {
                    BosOyunTabelasi("Beş çiftçinin de işe alındı. Çalışanlar bölümünden hepsini yönetebilirsin.")
                }
        }
    }
    if (adDialog) MetinDialog("Yeni çiftçi", ciftciVarsayilanAdi(calisanlar.size), "Ad", { adDialog = false }) {
        vm.calisanAl(it); adDialog = false
    }
}

@Composable
private fun KartlarimEkrani(esyalar: List<Canli>, calisanlar: List<CiftlikCalisani>, onHayvan: (String) -> Unit, onYapi: (String) -> Unit, onCalisan: (Int) -> Unit) {
    var sekme by remember { mutableIntStateOf(0) }
    val adetler = remember(esyalar) { esyalar.groupingBy { it.esyaId }.eachCount() }
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.farm_bg_cards), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Column(Modifier.fillMaxSize()) {
        Box(Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(.65f).aspectRatio(3f)) { Image(painterResource(R.drawable.ui_farm_name_banner), null, Modifier.fillMaxSize()); Text("Kartlarım", Modifier.align(Alignment.Center), color=Color(0xFF4A2917),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black) }
        Row(Modifier.fillMaxWidth().padding(horizontal=10.dp), horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            listOf("Hayvanlar", "Yapılar", "Çalışanlar").forEachIndexed { i, ad ->
                Surface(Modifier.weight(1f).clickable{sekme=i},color=if(sekme==i) Color(0xFF69C43F) else Color(0xFF744124),contentColor=Color(0xFFFFE7B8),shape=RoundedCornerShape(15.dp),border=androidx.compose.foundation.BorderStroke(2.dp,Color(0xFFD9A15C))){Text(ad,Modifier.padding(vertical=9.dp),textAlign=TextAlign.Center,fontWeight=FontWeight.Black)}
            }
        }
        when (sekme) {
            0 -> KoleksiyonGridi(MarketKatalog.hayvanlar(), adetler, onHayvan)
            1 -> KoleksiyonGridi(MarketKatalog.yapilar(), adetler, onYapi)
            else -> LazyVerticalGrid(modifier = Modifier.fillMaxSize(), columns = GridCells.Fixed(2), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 28.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items((0..4).sortedByDescending { tip -> calisanlar.any { it.tip == tip } }) { tip ->
                    val c = calisanlar.firstOrNull { it.tip == tip }
                    KoleksiyonKarti(c?.ad ?: ciftciVarsayilanAdi(tip), c?.let { "${it.rol} · Seviye ${it.seviye}" } ?: "Henüz alınmadı", c != null, { if (c != null) onCalisan(tip) }) {
                        FarmerPortrait(tip, Modifier.fillMaxSize())
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun KoleksiyonGridi(liste: List<MarketEsyasi>, adetler: Map<String, Int>, onSec: (String) -> Unit) {
    LazyVerticalGrid(modifier = Modifier.fillMaxSize(), columns = GridCells.Fixed(2), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 28.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(liste.sortedByDescending { (adetler[it.id] ?: 0) > 0 }, key = { it.id }) { esya ->
            val adet = adetler[esya.id] ?: 0
            KoleksiyonKarti(esya.isim, if (adet > 0) "×$adet sahip" else "Henüz alınmadı", adet > 0, { if (adet > 0) onSec(esya.id) }) {
                if (esya.tur == EsyaTuru.HAYVAN) AnimalPortrait(esya.id, 1, Modifier.fillMaxSize()) else BuildingPortrait(esya.id, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun KoleksiyonKarti(baslik: String, alt: String, sahip: Boolean, onClick: () -> Unit, gorsel: @Composable BoxScope.() -> Unit) {
    Surface(Modifier.height(202.dp).clickable(enabled = sahip, onClick = onClick), color = Color(0xFF744124), contentColor = Color(0xFFFFE7B8), shape = RoundedCornerShape(22.dp), shadowElevation = if (sahip) 8.dp else 2.dp, border=androidx.compose.foundation.BorderStroke(3.dp,Color(0xFFD9A15C))) {
        Box {
            Box(Modifier.fillMaxSize().padding(7.dp).padding(bottom=51.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xFFF2D39C))) {
                gorsel()
                if (!sahip) Box(Modifier.fillMaxSize().background(Color(0xB36A422B)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFFE7B8), modifier = Modifier.size(28.dp))
                        Text("Kilitli", color = Color(0xFFFFE7B8), fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(9.dp)) {
                Text(baslik, maxLines = 1, fontWeight = FontWeight.Black, color = Color(0xFFFFF0CE))
                Text(alt, maxLines = 1, color = if (sahip) Color(0xFFFFD69A) else Color(0xFFFFB2A2), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable private fun BosOyunTabelasi(metin:String){Box(Modifier.fillMaxWidth().aspectRatio(3f)){Image(painterResource(R.drawable.ui_farm_name_banner),null,Modifier.fillMaxSize());Text(metin,Modifier.align(Alignment.Center).padding(horizontal=36.dp),color=Color(0xFF4A2917),textAlign=TextAlign.Center,fontWeight=FontWeight.Black)}}

private fun ciftciVarsayilanAdi(tip: Int): String = Repository.varsayilanCalisanAdi(tip)

@Composable
private fun GaleriIskeleti(
    index: Int,
    adet: Int,
    onIndex: (Int) -> Unit,
    etiket: String,
    kisayolMetni: String,
    onKisayol: () -> Unit,
    arkaPlan: Int,
    sahne: @Composable BoxScope.() -> Unit,
    detay: @Composable ColumnScope.() -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(arkaPlan), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Column(Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 5.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(.72f).aspectRatio(3f)) {
                Image(painterResource(R.drawable.ui_farm_name_banner), null, Modifier.fillMaxSize())
                Text(etiket, Modifier.align(Alignment.Center), color = Color(0xFF4A2917), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }
            Box(Modifier.fillMaxWidth().weight(1f).pointerInput(index, adet) {
                var toplam = 0f
                detectHorizontalDragGestures(onDragEnd = { if (abs(toplam) > 70f) onIndex(if (toplam < 0) (index + 1).coerceAtMost(adet - 1) else (index - 1).coerceAtLeast(0)); toplam = 0f }, onHorizontalDrag = { _, d -> toplam += d })
            }) {
                Box(Modifier.align(Alignment.Center).fillMaxWidth(.78f).fillMaxHeight(.92f), content = sahne)
                OyunYonButonu(false, index > 0, Modifier.align(Alignment.CenterStart)) { onIndex(index - 1) }
                OyunYonButonu(true, index < adet - 1, Modifier.align(Alignment.CenterEnd)) { onIndex(index + 1) }
                Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) { CiftlikKisayolButonu(kisayolMetni, if (kisayolMetni == "İşçi Al") "🤝" else "🛒", onKisayol) }
                Box(Modifier.align(Alignment.BottomCenter).width(120.dp).aspectRatio(3f)) {
                    Image(painterResource(R.drawable.ui_farm_habitat_sign), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                    Text("${index + 1} / $adet", Modifier.align(Alignment.Center), color = Color(0xFF4A2917), fontWeight = FontWeight.Black)
                }
            }
            Box(Modifier.fillMaxWidth().heightIn(min = 225.dp, max = 275.dp)) {
                Image(painterResource(R.drawable.ui_farm_upgrade_panel), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 34.dp, end = 34.dp, top = 38.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp), content = detay
                )
            }
        }
    }
}

@Composable
private fun CiftlikKisayolButonu(metin: String, simge: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(15.dp)).clickable(onClick = onClick),
        color = Color(0xFF744124), contentColor = Color(0xFFFFE7B8), shape = RoundedCornerShape(15.dp), shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFD9A15C))
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (metin == "Market") {
                Image(painterResource(R.drawable.garden_store), null, Modifier.size(22.dp), contentScale = ContentScale.Fit)
            } else {
                Text(simge)
            }
            Spacer(Modifier.width(4.dp))
            Text(metin, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable private fun Ilerleme(mevcut: Int, hedef: Int, baslik: String) { val h=hedef.coerceAtLeast(1); Row { Text(baslik, Modifier.weight(1f), color=Color(0xFF4A2917), fontWeight=FontWeight.Bold); Text("$mevcut/$h", color=Color(0xFF4A2917), fontWeight=FontWeight.Black) }; LinearProgressIndicator(progress=(mevcut.toFloat()/h).coerceIn(0f,1f), modifier=Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(50)), color=Color(0xFF69C43F), trackColor=Color(0xFF6D3E22)) }

@Composable
private fun PuanButonlari(bakiye: Int, kalan: Int, onYatir: (Int)->Unit) {
    OyunPuanKontrolu(bakiye, kalan, Modifier.fillMaxWidth(), onYatir)
}

@Composable private fun BosKategori(metin:String) { Box(Modifier.fillMaxSize()) { Image(painterResource(R.drawable.farm_valley_hd),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop); Box(Modifier.align(Alignment.Center).fillMaxWidth(.88f).aspectRatio(3f)){ Image(painterResource(R.drawable.ui_farm_name_banner),null,Modifier.fillMaxSize()); Text(metin,Modifier.align(Alignment.Center).padding(horizontal=36.dp),color=Color(0xFF4A2917),textAlign=TextAlign.Center,fontWeight=FontWeight.Black) } } }

@Composable
private fun MetinDialog(baslik:String, ilk:String, etiket:String, kapat:()->Unit, kaydet:(String)->Unit) { var yazi by remember(ilk){ mutableStateOf(ilk) }; AlertDialog(onDismissRequest=kapat,containerColor=Color(0xFFF5DDAF),titleContentColor=Color(0xFF4A2917),textContentColor=Color(0xFF6B442A),shape=RoundedCornerShape(26.dp),title={Text(baslik,fontWeight=FontWeight.Black)},text={OutlinedTextField(yazi,{yazi=it},label={Text(etiket)},singleLine=true)},confirmButton={Button({kaydet(yazi.trim())},enabled=yazi.isNotBlank(),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF69C43F))){Text("Kaydet",fontWeight=FontWeight.Black)}},dismissButton={TextButton(kapat){Text("Vazgeç",color=Color(0xFF744124),fontWeight=FontWeight.Bold)}}) }

private fun havaSimgesi(hava:HavaSonucu?)=when(hava?.tur){HavaTuru.ACIK->"☀️";HavaTuru.BULUTLU->"☁️";HavaTuru.YAGMURLU->"🌧️";HavaTuru.FIRTINALI->"⛈️";HavaTuru.KARLI->"❄️";HavaTuru.SISLI->"🌫️";null->"📍"}
