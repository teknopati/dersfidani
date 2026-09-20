package com.dersfidan.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dersfidan.app.data.MarketEsyasi
import com.dersfidan.app.data.MarketKatalog
import com.dersfidan.app.data.EsyaTuru
import com.dersfidan.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketSheet(
    kalanPuan: Int,
    adetler: Map<String, Int>,
    yemAdedi: Int,
    yeniAgacFiyati: Int,
    onAgacAl: () -> Unit,
    onSatinAl: (MarketEsyasi, String) -> Unit,
    onYemAl: (Int, Int) -> Unit,
    onKapat: () -> Unit
) {
    var sekme by remember { mutableStateOf(0) }
    var isimVerilecekHayvan by remember { mutableStateOf<MarketEsyasi?>(null) }
    val basliklar = listOf("Hayvanlar", "Yapılar", "Yem")

    ModalBottomSheet(
        onDismissRequest = onKapat,
        containerColor = Color(0xFFF5DDAF),
        contentColor = Color(0xFF4A2917),
        dragHandle = { Box(Modifier.padding(top = 8.dp).width(72.dp).height(6.dp).clip(RoundedCornerShape(50)).background(Color(0xFF8A512D))) }
    ) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(.94f)) {
            Image(painterResource(R.drawable.farm_bg_market), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = .12f)))
            Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(.72f).aspectRatio(3f)) {
                Image(painterResource(R.drawable.ui_farm_name_banner), null, Modifier.fillMaxSize())
                Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                    Text("Market", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color(0xFF4A2917))
                    Spacer(Modifier.width(12.dp)); Text("$kalanPuan puan", fontWeight = FontWeight.Black, color = Color(0xFF4A2917))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                basliklar.forEachIndexed { i, baslik ->
                    Surface(
                        Modifier.weight(1f).clickable { sekme = i },
                        color = if (sekme == i) Color(0xFF69C43F) else Color(0xFF8A512D),
                        contentColor = Color(0xFFFFEAC3), shape = RoundedCornerShape(16.dp), shadowElevation = 5.dp,
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFD9A15C))
                    ) { Text(baslik, Modifier.padding(vertical = 10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Black) }
                }
            }
            Spacer(Modifier.height(10.dp))

            val liste = when (sekme) {
                0 -> MarketKatalog.hayvanlar()
                1 -> MarketKatalog.yapilar()
                else -> emptyList()
            }

            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(top = 2.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (sekme == 2) item {
                    Surface(color = Color(0xEFFFF1D2), contentColor = Color(0xFF4A2917), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFD9A15C))) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text("Depodaki ortak yem: $yemAdedi", fontWeight = FontWeight.Black)
                            Text("Satın aldığın yem bütün hayvanlarda kullanılabilir.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    YemPaketi("Küçük Yem Torbası", 5, 5, kalanPuan, onYemAl)
                    Spacer(Modifier.height(8.dp))
                    YemPaketi("Bereketli Yem Çuvalı", 20, 18, kalanPuan, onYemAl)
                    Spacer(Modifier.height(8.dp))
                    YemPaketi("Çiftlik Yem Sandığı", 50, 40, kalanPuan, onYemAl)
                    Spacer(Modifier.height(16.dp))
                }
                if (sekme == 1) item {
                    YeniAgacMarketSatiri(yeniAgacFiyati, kalanPuan >= yeniAgacFiyati, onAgacAl)
                    Spacer(Modifier.height(8.dp))
                }
                items(liste, key = { it.id }) { esya ->
                    val adet = adetler[esya.id] ?: 0
                    val guncelFiyat = MarketKatalog.guncelFiyat(esya, adet)
                    val sinirda = esya.tur == EsyaTuru.HAYVAN && adet >= MarketKatalog.HAYVAN_ADET_SINIRI
                    MarketSatiri(
                        esya = esya,
                        adet = adet,
                        guncelFiyat = guncelFiyat,
                        yeterliPuan = kalanPuan >= guncelFiyat,
                        sinirda = sinirda
                    ) {
                        if (esya.tur == EsyaTuru.HAYVAN) isimVerilecekHayvan = esya
                        else onSatinAl(esya, "")
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
            }
        }
    }

    isimVerilecekHayvan?.let { hayvan ->
        val adet = adetler[hayvan.id] ?: 0
        HayvanaIsimVerDialog(
            hayvan = hayvan,
            fiyat = MarketKatalog.guncelFiyat(hayvan, adet),
            onKapat = { isimVerilecekHayvan = null },
            onOnayla = { ad ->
                onSatinAl(hayvan, ad)
                isimVerilecekHayvan = null
            }
        )
    }
}

@Composable
private fun YemPaketi(ad: String, adet: Int, fiyat: Int, bakiye: Int, onAl: (Int, Int) -> Unit) {
    OyunMarketSatiri {
        Text("🌾", fontSize = 32.sp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(ad, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4A2917), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$adet öğün ortak yem", color = Color(0xFF6B442A), style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        OyunSatinAlDugmesi("$fiyat puan", bakiye >= fiyat) { onAl(adet, fiyat) }
    }
}

@Composable
private fun YeniAgacMarketSatiri(fiyat: Int, yeterli: Boolean, onAl: () -> Unit) {
    OyunMarketSatiri {
        TreePortrait(0, Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text("Yeni Ağaç", fontWeight = FontWeight.Black, color = Color(0xFF4A2917))
            Text("Sahip olmadığın rastgele bir tür.", color = Color(0xFF6B442A), style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
        OyunSatinAlDugmesi(if (fiyat == 0) "Ücretsiz" else "$fiyat puan", yeterli || fiyat == 0, onAl)
    }
}

@Composable
private fun MarketSatiri(
    esya: MarketEsyasi,
    adet: Int,
    guncelFiyat: Int,
    yeterliPuan: Boolean,
    sinirda: Boolean,
    onSatinAl: () -> Unit
) {
    OyunMarketSatiri {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(14.dp))) {
            if (esya.tur == EsyaTuru.HAYVAN) AnimalPortrait(esya.id, (adet + 1).coerceIn(1, 5), Modifier.fillMaxSize())
            else BuildingPortrait(esya.id, Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(esya.isim, color = Color(0xFF4A2917), fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (adet > 0) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF69C43F))
                            .padding(horizontal = 7.dp, vertical = 1.dp)
                    ) {
                        Text("x$adet", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (esya.geceCanlisi) {
                    Spacer(Modifier.width(6.dp))
                    Text("🌙", fontSize = 12.sp)
                }
            }
            Text(esya.aciklama, color = Color(0xFF6B442A), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(6.dp))
        OyunSatinAlDugmesi(if (sinirda) "5/5" else "$guncelFiyat puan", yeterliPuan && !sinirda, onSatinAl)
    }
}

@Composable
private fun OyunMarketSatiri(content: @Composable RowScope.() -> Unit) {
    Box(Modifier.fillMaxWidth().height(112.dp)) {
        Image(painterResource(R.drawable.ui_farm_upgrade_panel), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        Row(
            Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
private fun OyunSatinAlDugmesi(metin: String, etkin: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = etkin,
        modifier = Modifier.widthIn(min = 76.dp, max = 88.dp).height(42.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF69C43F), contentColor = Color.White),
        shape = RoundedCornerShape(15.dp), contentPadding = PaddingValues(horizontal = 6.dp)
    ) { Text(metin, fontWeight = FontWeight.Black, maxLines = 1, fontSize = 11.sp) }
}

@Composable
private fun HayvanaIsimVerDialog(
    hayvan: MarketEsyasi,
    fiyat: Int,
    onKapat: () -> Unit,
    onOnayla: (String) -> Unit
) {
    var ad by remember(hayvan.id) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onKapat,
        containerColor = Color(0xFFF5DDAF),
        titleContentColor = Color(0xFF4A2917),
        textContentColor = Color(0xFF6B442A),
        shape = RoundedCornerShape(26.dp),
        title = { Text("${hayvan.isim} için isim", fontWeight = FontWeight.Black) },
        text = {
            Column {
                Text("Bu hayvan ortak habitatında kendi adıyla yaşayacak.")
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = ad,
                    onValueChange = { if (it.length <= 24) ad = it },
                    label = { Text("Hayvanın adı") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text("Ödenecek: $fiyat puan", fontWeight = FontWeight.SemiBold)
                Text("Sonraki ${hayvan.isim.lowercase()} fiyatı %25 artar.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = { onOnayla(ad.trim()) }, enabled = ad.isNotBlank(), colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF69C43F))) { Text("Satın al",fontWeight=FontWeight.Black) }
        },
        dismissButton = { TextButton(onClick = onKapat) { Text("Vazgeç",color=Color(0xFF744124),fontWeight=FontWeight.Bold) } }
    )
}
