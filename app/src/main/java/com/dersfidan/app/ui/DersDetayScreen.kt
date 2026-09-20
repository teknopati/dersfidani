package com.dersfidan.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dersfidan.app.data.DersNotu
import com.dersfidan.app.viewmodel.AppViewModel
import com.dersfidan.app.util.TarihUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DersDetayScreen(vm: AppViewModel, onGeri: () -> Unit) {
    val ders by vm.secilenDers.collectAsState()
    val bloklar by vm.secilenDersBloklari.collectAsState()
    val notlar by vm.secilenDersNotlari.collectAsState()
    var notEkleAcik by remember { mutableStateOf(false) }
    var tamEkranFoto by remember { mutableStateOf<String?>(null) }

    val toplam = bloklar.size
    val yapilan = bloklar.count { it.tamamlandi }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ders ?: "") },
                navigationIcon = {
                    IconButton(onClick = onGeri) { Icon(Icons.Default.ArrowBack, contentDescription = "Geri") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { notEkleAcik = true }) {
                Icon(Icons.Default.Add, contentDescription = "Not ekle")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                Spacer(Modifier.height(8.dp))
                val oran = if (toplam > 0) yapilan.toFloat() / toplam else 0f
                Text("İlerleme: $yapilan / $toplam", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { oran }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp))
                Text("Yanlışlarım / Dikkat Edilmesi Gerekenler", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
            }

            if (notlar.isEmpty()) {
                item { Text("Henüz not yok. Sağ alttaki + ile ekleyebilirsin.") }
            }

            items(notlar, key = { it.id }) { notu ->
                NotKarti(notu = notu, onSil = { vm.notSil(notu) }, onFotoTikla = { tamEkranFoto = it })
                Spacer(Modifier.height(10.dp))
            }

            item {
                Spacer(Modifier.height(20.dp))
                Text("Bu derse ait tüm bloklar (${bloklar.size})", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
            }
            items(bloklar, key = { it.id }) { blok ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = blok.tamamlandi,
                        enabled = !blok.tamamlandi && !TarihUtil.gelecekMi(blok.tarih),
                        onCheckedChange = { if (it) vm.isaretle(blok, true) }
                    )
                    Column {
                        Text("${blok.tarih}  ${blok.saatBaslangic}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text(blok.metin, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (notEkleAcik && ders != null) {
        NotEkleDialog(
            onKapat = { notEkleAcik = false },
            onKaydet = { metin, foto -> vm.notEkle(ders!!, metin, foto); notEkleAcik = false }
        )
    }

    tamEkranFoto?.let { yol ->
        TamEkranFotoGoruntuleyici(yol = yol, onKapat = { tamEkranFoto = null })
    }
}

/**
 * Fotoğrafı tüm ekranı kaplayacak şekilde, siyah zemin üstünde, parmakla
 * yakınlaştırıp kaydırılabilir biçimde gösterir. Yanlışını net görebilmek için.
 */
@Composable
private fun TamEkranFotoGoruntuleyici(yol: String, onKapat: () -> Unit) {
    var olcek by remember { mutableStateOf(1f) }
    var kaydirmaX by remember { mutableStateOf(0f) }
    var kaydirmaY by remember { mutableStateOf(0f) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onKapat,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        olcek = (olcek * zoom).coerceIn(1f, 6f)
                        kaydirmaX += pan.x
                        kaydirmaY += pan.y
                    }
                }
        ) {
            AsyncImage(
                model = yol,
                contentDescription = "Fotoğraf, tam ekran",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = olcek
                        scaleY = olcek
                        translationX = kaydirmaX
                        translationY = kaydirmaY
                    }
            )
            IconButton(
                onClick = onKapat,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.4f), shape = androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
            }
        }
    }
}

@Composable
private fun NotKarti(notu: DersNotu, onSil: () -> Unit, onFotoTikla: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(notu.metin, modifier = Modifier.weight(1f))
            IconButton(onClick = onSil, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Sil")
            }
        }
        (notu.bulutFotoUrl ?: notu.fotoYolu)?.let { yol ->
            Spacer(Modifier.height(8.dp))
            AsyncImage(
                model = yol,
                contentDescription = "Not fotoğrafı — büyütmek için dokun",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onFotoTikla(yol) }
            )
        }
    }
}

@Composable
private fun NotEkleDialog(
    onKapat: () -> Unit,
    onKaydet: (metin: String, fotoYolu: String?) -> Unit
) {
    val context = LocalContext.current
    var metin by remember { mutableStateOf("") }
    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    var fotoDosyaYolu by remember { mutableStateOf<String?>(null) }

    val kameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { basarili ->
        if (!basarili) { fotoUri = null; fotoDosyaYolu = null }
    }
    val galeriLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        fotoUri = uri
        fotoDosyaYolu = uri?.toString()
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onKapat) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(20.dp)) {
                Text("Not Ekle", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    metin, { metin = it },
                    label = { Text("Yanlışın / dikkat noktasının açıklaması") },
                    minLines = 3, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                fotoUri?.let {
                    AsyncImage(
                        model = it, contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Row {
                    OutlinedButton(onClick = {
                        val (dosya, uri) = PhotoUtils.yeniFotoUri(context)
                        fotoDosyaYolu = dosya.absolutePath
                        fotoUri = uri
                        kameraLauncher.launch(uri)
                    }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Kamera")
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { galeriLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Galeri")
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onKapat) { Text("Vazgeç") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onKaydet(metin, fotoDosyaYolu) },
                        enabled = metin.isNotBlank()
                    ) { Text("Kaydet") }
                }
            }
        }
    }
}
