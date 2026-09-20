package com.dersfidan.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import com.dersfidan.app.viewmodel.AppViewModel

@Composable
fun DerslerScreen(vm: AppViewModel, onDersSecildi: () -> Unit) {
    val ozetler by vm.dersOzetleri.collectAsState()
    val listeDurumu = rememberLazyListState()
    LaunchedEffect(Unit) {
        vm.sekmeTekrarOlayi.collect { if (it == Ekran.Dersler.route) listeDurumu.animateScrollToItem(0) }
    }

    LazyColumn(state = listeDurumu, modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Derslerim", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
        }
        items(ozetler, key = { it.dersAdi }) { ozet ->
            val oran = if (ozet.toplam > 0) ozet.yapilan.toFloat() / ozet.toplam else 0f
            val animasyonluOran by animateFloatAsState(oran, label = "dersIlerleme")
            val tamam = ozet.toplam > 0 && ozet.yapilan == ozet.toplam
            val renk = dersRengi(ozet.dersAdi)
            val simge = dersSimgesi(ozet.dersAdi)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(renk.copy(alpha = if (tamam) .20f else .10f))
                    .clickable { vm.dersSec(ozet.dersAdi); onDersSecildi() }
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(38.dp)) {
                        CircularProgressIndicator(progress = { animasyonluOran }, strokeWidth = 4.dp, color = renk)
                        if (tamam) Icon(Icons.Default.Check, contentDescription = "Tamamlandı", tint = renk)
                        else Text(simge)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(ozet.dersAdi, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("${ozet.yapilan}/${ozet.toplam}")
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { animasyonluOran }, modifier = Modifier.fillMaxWidth(), color = renk)
                Spacer(Modifier.height(4.dp))
                Text("${ozet.yapilanDk} / ${ozet.toplamDk} dakika", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

private fun dersSimgesi(ad: String): String = when {
    ad.contains("Matematik", true) -> "∑"
    ad.contains("Tarih", true) -> "🏛"
    ad.contains("Coğrafya", true) -> "🌍"
    ad.contains("Türk", true) || ad.contains("Sözel", true) -> "✍"
    ad.contains("Mevzuat", true) -> "⚖"
    ad.contains("Eğitim", true) -> "🎓"
    else -> "📘"
}

private fun dersRengi(ad: String): Color {
    val renkler = listOf(Color(0xFF2E7D32), Color(0xFF1565C0), Color(0xFF8E24AA), Color(0xFFEF6C00), Color(0xFF00838F), Color(0xFFC62828))
    return renkler[Math.floorMod(ad.hashCode(), renkler.size)]
}
