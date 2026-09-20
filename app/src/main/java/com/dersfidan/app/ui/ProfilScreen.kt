package com.dersfidan.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.dersfidan.app.data.DersOzet
import com.dersfidan.app.data.ProfilPrefs
import com.dersfidan.app.data.Repository
import com.dersfidan.app.viewmodel.AppViewModel
import kotlin.math.min

@Composable
fun ProfilScreen(vm: AppViewModel) {
    val context = LocalContext.current
    var isim by remember { mutableStateOf(ProfilPrefs.isim(context)) }
    var avatar by remember { mutableStateOf(ProfilPrefs.avatar(context)) }
    var profilFoto by remember { mutableStateOf(ProfilPrefs.fotoUrl(context) ?: ProfilPrefs.foto(context)) }
    var avatarSeciciAcik by remember { mutableStateOf(false) }
    var kameraUri by remember { mutableStateOf<Uri?>(null) }
    val listeDurumu = rememberLazyListState()
    val galeri = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { secilen ->
            profilFoto = secilen.toString()
            vm.profilFotografiDegistir(secilen) { url -> if (url != null) profilFoto = url }
        }
        avatarSeciciAcik = false
    }
    val kamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { tamam ->
        if (tamam) kameraUri?.let { secilen ->
            profilFoto = secilen.toString()
            vm.profilFotografiDegistir(secilen) { url -> if (url != null) profilFoto = url }
        }
        avatarSeciciAcik = false
    }
    val toplamPuan by vm.toplamKazanilanPuan.collectAsState()
    val kalanPuan by vm.kalanPuan.collectAsState()
    val canlilar by vm.sahipOlunanCanlilar.collectAsState()
    val agaclar by vm.agaclar.collectAsState()
    val ozetler by vm.gunlukOzetler.collectAsState()
    val dersOzetleri by vm.dersOzetleri.collectAsState()
    val denemeOzetleri by vm.denemeOzetleri.collectAsState()

    LaunchedEffect(Unit) {
        vm.sekmeTekrarOlayi.collect { if (it == Ekran.Profil.route) listeDurumu.animateScrollToItem(0) }
    }

    LazyColumn(state = listeDurumu, modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (profilFoto != null) {
                    AsyncImage(
                        model = profilFoto, contentDescription = "Profil fotoğrafı",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(CircleShape).clickable { avatarSeciciAcik = true }
                    )
                } else Text(avatar, fontSize = 40.sp, modifier = Modifier.clickable { avatarSeciciAcik = true })
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(isim, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${agaclar.size} ağaç · ${canlilar.size} canlı çiftlikte", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Fotoğrafa dokunup değiştirebilirsin",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ProfilIstatistik(baslik = "Toplam Puan", deger = "$toplamPuan")
                ProfilIstatistik(baslik = "Bankadaki Puan", deger = "$kalanPuan")
                ProfilIstatistik(baslik = "Canlı", deger = "${canlilar.size}")
            }

            Spacer(Modifier.height(24.dp))
            NasilOynanirKarti()

            Spacer(Modifier.height(20.dp))
            GenelOzetKarti(
                toplamPuan = toplamPuan,
                tamamlananGunSayisi = ozetler.count { it.toplam > 0 && it.yapilan == it.toplam },
                agacSayisi = agaclar.size,
                canliSayisi = canlilar.size
            )

            Spacer(Modifier.height(24.dp))
            Text("📚 Derslerim", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Her ders kendi ilerlemesini gösterir; kart tamamen bitince toplam süresinin %10'u kadar tek seferlik ödül verir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
        }

        if (dersOzetleri.isEmpty()) {
            item {
                Text("Henüz ders bloğu yok.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
        items(dersOzetleri, key = { "ders-" + it.dersAdi }) { ozet ->
            DersRozetKarti(ozet)
            Spacer(Modifier.height(10.dp))
        }

        item {
            Spacer(Modifier.height(14.dp))
            Text("📝 Denemelerim", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Branş denemeleri ve deneme analizleri, ait oldukları ders/sınavın kendi kartında ayrı izlenir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
        }
        if (denemeOzetleri.isEmpty()) {
            item {
                Text("Henüz deneme bloğu yok.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
        items(denemeOzetleri, key = { "deneme-" + it.dersAdi }) { ozet ->
            DersRozetKarti(ozet, denemeMi = true)
            Spacer(Modifier.height(10.dp))
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "Günlük geçmişinin tamamı aşağıdaki Geçmiş sekmesinde.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            var cikisOnayAcik by remember { mutableStateOf(false) }
            OutlinedButton(
                onClick = { cikisOnayAcik = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Çıkış Yap") }
            if (cikisOnayAcik) {
                AlertDialog(
                    onDismissRequest = { cikisOnayAcik = false },
                    title = { Text("Çıkış yapılsın mı?") },
                    text = { Text("İlerlemen hesabında güvende kalır; tekrar aynı e-posta/şifreyle giriş yapınca geri gelir.") },
                    confirmButton = {
                        TextButton(onClick = { cikisOnayAcik = false; vm.cikisYap() }) { Text("Çıkış Yap") }
                    },
                    dismissButton = { TextButton(onClick = { cikisOnayAcik = false }) { Text("Vazgeç") } }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (avatarSeciciAcik) {
        AvatarSeciciDialog(
            mevcut = avatar,
            onGaleri = { galeri.launch("image/*") },
            onKamera = {
                val (_, uri) = PhotoUtils.yeniProfilFotoUri(context)
                kameraUri = uri
                kamera.launch(uri)
            },
            onSecildi = { yeni ->
                ProfilPrefs.avatarKaydet(context, yeni)
                avatar = yeni
                profilFoto = null
                avatarSeciciAcik = false
                vm.avatarDegistiIsaretle()
            },
            onKapat = { avatarSeciciAcik = false }
        )
    }
}

/** Profil fotoğrafı seçim ekranı: birkaç hazır seçenek arasından dokunarak değiştirilir. */
@Composable
private fun AvatarSeciciDialog(
    mevcut: String,
    onGaleri: () -> Unit,
    onKamera: () -> Unit,
    onSecildi: (String) -> Unit,
    onKapat: () -> Unit
) {
    val secenekler = ProfilPrefs.avatarSecenekleri
    AlertDialog(
        onDismissRequest = onKapat,
        confirmButton = { TextButton(onClick = onKapat) { Text("Kapat") } },
        title = { Text("Profil Fotoğrafını Seç") },
        text = {
            Column {
                Row {
                    OutlinedButton(onClick = onKamera) { Text("📷 Kamera") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = onGaleri) { Text("🖼️ Galeri") }
                }
                Spacer(Modifier.height(12.dp))
                LazyColumnGrid4(secenekler, mevcut, onSecildi)
            }
        }
    )
}

@Composable
private fun LazyColumnGrid4(secenekler: List<String>, mevcut: String, onSecildi: (String) -> Unit) {
    Column {
        secenekler.chunked(4).forEach { satir ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                satir.forEach { emoji ->
                    val secili = emoji == mevcut
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (secili) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onSecildi(emoji) }
                            .padding(10.dp)
                    ) {
                        Text(emoji, fontSize = 26.sp)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun ProfilIstatistik(baslik: String, deger: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(deger, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(baslik, style = MaterialTheme.typography.bodySmall)
    }
}

/** Katlanır "Nasıl Oynanır" açıklama kartı — ağaç/puan uyarısının yerini alır. */
@Composable
private fun NasilOynanirKarti() {
    var acik by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable { acik = !acik }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("🎮", fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text("Nasıl Oynanır?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(if (acik) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
        }
        if (acik) {
            Spacer(Modifier.height(10.dp))
            listOf(
                "Bugün ekranındaki bir ders/soru bloğunu sağa kaydırınca tamamlanmış sayılır.",
                "Uyanma, mola, yemek, serbest zaman, yürüyüş, şekerleme ve uyku blokları 1 puandır.",
                "Diğer günlük bloklar sürelerinin %20'si kadar puan verir; küsurat daima yukarı yuvarlanır.",
                "Bir ders veya deneme kartı tamamen bitince kartın toplam süresinin %10'u ayrıca ve yalnızca bir kez verilir.",
                "Bir günün TÜM blokları bitince +15 bonus puan kazanırsın.",
                "Kazandığın puan bankada birikir; hayvan, habitat, yapı, ağaç ve çalışanlara +1, +3, +5, +10 veya Hedef seçenekleriyle parça parça yatırabilirsin.",
                "Market'te 34 resimli hayvan, 30 resimli yapı ve 100 isimli ağaç türü bulunur.",
                "Hayvanı alırken ona isim verirsin. Aynı türden en fazla 5 tane alınır ve sonraki fiyat önceki fiyattan %25 artıp yukarı yuvarlanır.",
                "İlk hayvan kendi türüne özel habitatını ücretsiz açar. Aynı türden ikinci hayvan için habitat seviye 2, üçüncü için seviye 4, dördüncü için seviye 6 ve beşinci için seviye 8 olmalıdır.",
                "Her hayvan 1 yaşında başlar ve en fazla 20 yaşına ulaşır. 2. yaş 20 puandır; sonraki her yaş 15 puan daha pahalıdır. Yaşa göre Yeni Doğan'dan Çiftliğin Efsanesi'ne kadar unvan kazanır.",
                "Habitatlar ve yapılar en fazla 10. seviyeye çıkar. 2. seviye 30 puandır; sonraki her seviye 20 puan daha pahalıdır.",
                "Çiftlik merkezindeki büyük kartlardan hayvan, yapı, ağaç ve çalışan galerilerine girersin; oklarla veya sağa-sola kaydırarak hepsini tek tek görürsün.",
                "Kartlarım bölümünde Hayvanlar, Yapılar ve Çalışanlar koleksiyonunu görürsün. Alınmamış kartlar karanlık görünür; sahip olduğun karta basınca ilgili ayrıntı doğrudan açılır.",
                "Hayvan kartında seçili hayvanı büyük resimli görünümde ve aynı habitatta yaşayan diğer hayvanları görürsün. Habitat satırına basarak evi genişletebilirsin.",
                "Çiftlik adını üstteki başlığa dokunarak değiştirebilirsin. Hayvanlar ve çalışanlar bu adı kullanarak seninle konuşur.",
                "Çalışan evinin en fazla 5 seviyesi vardır ve her seviye bir çiftçiye yer açar. Evi Çalışanlar bölümünden yükseltir, yeni çiftçiyi İşçi işe al bölümünden alırsın. Çiftçiler en fazla seviye 10 olur.",
                "Bankada puanın yoksa ağaca dokunmak bir şey yapmaz; önce bir görev tamamla.",
                "Aşağıdaki Derslerim / Denemelerim bölümünde her dersin ve her denemenin kendi ayrı ilerlemesini görebilirsin."
            ).forEach { madde ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text("•  ", fontWeight = FontWeight.Bold)
                    Text(madde, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** Genel ilerleme özeti: toplam puan / tam gün / ağaç / canlı sayısı — tek satırlık üst özet. */
@Composable
private fun GenelOzetKarti(toplamPuan: Int, tamamlananGunSayisi: Int, agacSayisi: Int, canliSayisi: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp)
    ) {
        Text("🏆 Genel İlerleme", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        BasariSatiri("🌱 Puan", toplamPuan, maxOf(500, toplamPuan), "puan")
        Spacer(Modifier.height(10.dp))
        BasariSatiri("📅 Tam Gün", tamamlananGunSayisi, maxOf(30, tamamlananGunSayisi), "gün")
        Spacer(Modifier.height(10.dp))
        BasariSatiri("🌳 Ağaç", agacSayisi, maxOf(20, agacSayisi), "ağaç")
        Spacer(Modifier.height(10.dp))
        BasariSatiri("🐄 Canlı", canliSayisi, maxOf(25, canliSayisi), "canlı")
    }
}

/** Tek bir dersin (veya deneme grubunun) kendi kartı: ilerleme, kazanılan puan, süre bilgisi. */
@Composable
private fun DersRozetKarti(ozet: DersOzet, denemeMi: Boolean = false) {
    val oran = if (ozet.toplam > 0) min(1f, ozet.yapilan.toFloat() / ozet.toplam) else 0f
    val bitirmeOdulu = Repository.puanPerAchievement(ozet.toplamDk)
    val toplamSaat = ozet.toplamDk / 60.0

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(if (denemeMi) "📝" else "📘", fontSize = 18.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                ozet.dersAdi,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text("Bitirme ödülü +$bitirmeOdulu", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { oran },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${ozet.yapilan}/${ozet.toplam} tamamlandı · kart bitince toplam %.1f saatin %%10'u bir kez verilir".format(toplamSaat),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BasariSatiri(baslik: String, deger: Int, hedef: Int, birim: String) {
    val oran = if (hedef > 0) min(1f, deger.toFloat() / hedef) else 0f
    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(baslik, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("$deger / $hedef $birim", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { oran },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
        )
    }
}
