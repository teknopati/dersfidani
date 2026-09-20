package com.dersfidan.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dersfidan.app.data.Blok
import com.dersfidan.app.util.TarihUtil
import com.dersfidan.app.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugunScreen(vm: AppViewModel, tarihOverride: String? = null, onGeri: (() -> Unit)? = null) {
    val tarih = tarihOverride ?: vm.bugununTarihi
    val bloklar by vm.secilenGunBloklari.collectAsState()
    val listeDurumu = rememberLazyListState()
    var duzenlenecekBlok by remember { mutableStateOf<Blok?>(null) }
    var yeniBlokAcik by remember { mutableStateOf(false) }

    LaunchedEffect(tarih) { vm.tarihSec(tarih) }
    LaunchedEffect(Unit) {
        vm.sekmeTekrarOlayi.collect { route ->
            if (tarihOverride == null && route == Ekran.Bugun.route) listeDurumu.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            if (onGeri != null) TopAppBar(
                title = { Text(TarihUtil.tamTarih(tarih)) },
                navigationIcon = { IconButton(onClick = onGeri) { Icon(Icons.Default.ArrowBack, "Geçmişe dön") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { yeniBlokAcik = true }) {
                Icon(Icons.Default.Add, contentDescription = "Blok ekle")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(12.dp))
                if (onGeri == null) Text(
                    TarihUtil.tamTarih(tarih),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Onaylamak için bloğu sağa kaydır",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(
                state = listeDurumu,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                if (bloklar.isEmpty()) {
                    item {
                        Text(
                            "Bu güne ait blok yok.",
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }

                items(bloklar, key = { it.id }) { blok ->
                    KaydirilabilirBlok(
                        blok = blok,
                        gelecekGun = TarihUtil.gelecekMi(blok.tarih),
                        onIsaretle = { yapildi -> vm.isaretle(blok, yapildi) },
                        onDuzenle = { duzenlenecekBlok = blok }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    duzenlenecekBlok?.let { blok ->
        BlokDuzenleDialog(
            blok = blok,
            onKapat = { duzenlenecekBlok = null },
            onKaydet = { guncel -> vm.blokGuncelle(guncel); duzenlenecekBlok = null },
            onSil = { vm.blokSil(blok); duzenlenecekBlok = null }
        )
    }

    if (yeniBlokAcik) {
        YeniBlokDialog(
            tarih = tarih,
            onKapat = { yeniBlokAcik = false },
            onEkle = { gun, bas, bit, kategori, metin ->
                vm.blokEkle(tarih, gun, bas, bit, kategori, metin)
                yeniBlokAcik = false
            }
        )
    }
}

/**
 * Tamamlanmamış bir blok sağa kaydırılınca "yapıldı" olarak işaretlenir ve KALICI olarak yeşil kalır
 * (tekrar dokununca geri alınmaz). Henüz gelmemiş (gelecek tarihli) günlerin blokları kaydırılarak
 * tamamlanamaz — sadece bugüne veya geçmişe ait bloklar işaretlenebilir.
 */
@Composable
private fun KaydirilabilirBlok(blok: Blok, gelecekGun: Boolean, onIsaretle: (Boolean) -> Unit, onDuzenle: () -> Unit) {
    val yogunluk = LocalDensity.current
    val esikPx = with(yogunluk) { 88.dp.toPx() }
    val offsetAnim = remember(blok.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val arkaplanRengi by animateColorAsState(
        targetValue = if (blok.tamamlandi) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        label = "blokArkaplan"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
    ) {
        // Arkada, kaydırınca ortaya çıkan "Yapıldı" ipucu
        Row(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.primary)
                .padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(6.dp))
            Text("Yapıldı", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
        }

        Row(
            modifier = Modifier
                .offset { IntOffset(offsetAnim.value.roundToInt(), 0) }
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(arkaplanRengi)
                .pointerInput(blok.id, blok.tamamlandi, gelecekGun) {
                    if (blok.tamamlandi || gelecekGun) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetAnim.value > esikPx) {
                                    onIsaretle(true)
                                }
                                offsetAnim.animateTo(0f, animationSpec = tween(220))
                            }
                        },
                        onDragCancel = {
                            scope.launch { offsetAnim.animateTo(0f, animationSpec = tween(220)) }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                offsetAnim.snapTo((offsetAnim.value + dragAmount).coerceIn(0f, esikPx * 1.4f))
                            }
                        }
                    )
                }
                .clickable(enabled = !blok.tamamlandi) {
                    onDuzenle()
                }
                .alpha(if (gelecekGun && !blok.tamamlandi) 0.55f else 1f)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tamamlanan bir görev artık geri alınamaz; kalıcı olarak yeşil/işaretli kalır.
            if (blok.tamamlandi) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "${blok.saatBaslangic}-${blok.saatBitis}  ${blok.dersAdi ?: blok.kategori}",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(blok.metin, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                if (gelecekGun && !blok.tamamlandi) {
                    Text(
                        "Henüz gelmedi — bu gün gelince tamamlayabilirsin",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
