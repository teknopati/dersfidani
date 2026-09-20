package com.dersfidan.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dersfidan.app.data.Blok
import com.dersfidan.app.data.SubjectUtil

@Composable
fun BlokDuzenleDialog(
    blok: Blok,
    onKapat: () -> Unit,
    onKaydet: (Blok) -> Unit,
    onSil: () -> Unit
) {
    var bas by remember { mutableStateOf(blok.saatBaslangic) }
    var bit by remember { mutableStateOf(blok.saatBitis) }
    var kategori by remember { mutableStateOf(blok.kategori) }
    var metin by remember { mutableStateOf(blok.metin) }

    Dialog(onDismissRequest = onKapat) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(
                Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Bloğu Düzenle", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Row {
                    OutlinedTextField(bas, { bas = it }, label = { Text("Başlangıç (SS:dd)") }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(bit, { bit = it }, label = { Text("Bitiş (SS:dd)") }, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                KategoriSecici(kategori) { kategori = it }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    metin, { metin = it }, label = { Text("Aktivite açıklaması") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onSil) { Text("Sil") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onKapat) { Text("Vazgeç") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        onKaydet(
                            blok.copy(
                                saatBaslangic = bas, saatBitis = bit, kategori = kategori, metin = metin,
                                dersAdi = SubjectUtil.tahminEt(kategori, metin)
                            )
                        )
                    }) { Text("Kaydet") }
                }
            }
        }
    }
}

@Composable
fun YeniBlokDialog(
    tarih: String,
    onKapat: () -> Unit,
    onEkle: (gun: String, bas: String, bit: String, kategori: String, metin: String) -> Unit
) {
    var bas by remember { mutableStateOf("") }
    var bit by remember { mutableStateOf("") }
    var kategori by remember { mutableStateOf(SubjectUtil.onerilenKategoriler.first()) }
    var metin by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onKapat) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(
                Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Yeni Blok Ekle", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Row {
                    OutlinedTextField(bas, { bas = it }, label = { Text("Başlangıç (SS:dd)") }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(bit, { bit = it }, label = { Text("Bitiş (SS:dd)") }, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                KategoriSecici(kategori) { kategori = it }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    metin, { metin = it }, label = { Text("Aktivite açıklaması") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onKapat) { Text("Vazgeç") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val gun = ""
                        onEkle(gun, bas, bit, kategori, metin)
                    }, enabled = bas.isNotBlank() && bit.isNotBlank() && metin.isNotBlank()) { Text("Ekle") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KategoriSecici(secili: String, onSec: (String) -> Unit) {
    var acik by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = acik, onExpandedChange = { acik = it }) {
        OutlinedTextField(
            value = secili,
            onValueChange = {},
            readOnly = true,
            label = { Text("Kategori") },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = acik, onDismissRequest = { acik = false }) {
            SubjectUtil.onerilenKategoriler.forEach { k ->
                DropdownMenuItem(text = { Text(k) }, onClick = { onSec(k); acik = false })
            }
        }
    }
}
