package com.dersfidan.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import com.dersfidan.app.data.GunOzet
import com.dersfidan.app.util.TarihUtil
import com.dersfidan.app.viewmodel.AppViewModel

@Composable
fun GecmisScreen(vm: AppViewModel, onGunSecildi: (String) -> Unit) {
    val ozetler by vm.gunlukOzetler.collectAsState()
    val listeDurumu = rememberLazyListState()

    LaunchedEffect(Unit) {
        vm.sekmeTekrarOlayi.collect { if (it == Ekran.Gecmis.route) listeDurumu.animateScrollToItem(0) }
    }

    LazyColumn(state = listeDurumu, modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Gün Gün Geçmiş", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
        }
        gecmisListesi(ozetler) { tarih ->
            onGunSecildi(tarih)
        }
    }
}

/** Geçmiş sekmesi ile Profilim ekranında ortak kullanılan, tam tarihli gün listesi. */
fun LazyListScope.gecmisListesi(ozetler: List<GunOzet>, onGunTiklandi: (String) -> Unit) {
    if (ozetler.isEmpty()) {
        item {
            Text("Henüz geçmiş kaydı yok.", modifier = Modifier.padding(vertical = 16.dp))
        }
    }
    items(ozetler, key = { it.tarih }) { ozet ->
        val oran = if (ozet.toplam > 0) ozet.yapilan.toFloat() / ozet.toplam else 0f
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onGunTiklandi(ozet.tarih) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(TarihUtil.tamTarih(ozet.tarih), fontWeight = FontWeight.SemiBold)
                ozet.etiket?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(progress = { oran }, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.width(12.dp))
            Text("${ozet.yapilan}/${ozet.toplam}")
        }
        Spacer(Modifier.height(8.dp))
    }
}
