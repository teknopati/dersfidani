package com.dersfidan.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dersfidan.app.data.BulutHesap
import com.dersfidan.app.data.BulutProfil
import com.dersfidan.app.data.ProfilPrefs
import kotlinx.coroutines.launch

/**
 * Uygulama ilk açıldığında (ya da hesap çıkışından sonra) gösterilen ekran.
 * İki sekme var: "Giriş Yap" (var olan hesap) ve "Kayıt Ol" (yeni hesap).
 * Hesap e-posta/şifre ile Firebase'de tutulur; böylece uygulama silinse/telefon
 * değişse bile aynı hesapla giriş yapınca ilerleme geri yüklenir.
 *
 * onBulutRestoreDene: giriş/kayıt başarılı olduktan hemen sonra çağrılır, buluttaki
 * yedeği (varsa) bu cihaza uygular ve varsa kayıtlı isim/avatar'ı döner.
 * onGirisYapildi: tüm akış bittikten sonra çağrılır; nihai isim/avatar'ı iletir.
 */
@Composable
fun GirisEkrani(
    onBulutRestoreDene: suspend () -> BulutProfil?,
    onGirisYapildi: (isim: String, avatar: String) -> Unit
) {
    var sekme by remember { mutableStateOf(0) } // 0 = Giriş Yap, 1 = Kayıt Ol
    val kapsam = rememberCoroutineScope()

    var kullaniciAdi by remember { mutableStateOf("") }
    var sifre by remember { mutableStateOf("") }
    var isim by remember { mutableStateOf("") }
    var seciliAvatar by remember { mutableStateOf(ProfilPrefs.avatarSecenekleri.first()) }
    var yukleniyor by remember { mutableStateOf(false) }
    var hataMesaji by remember { mutableStateOf<String?>(null) }

    suspend fun girisAkisiTamamla(varsayilanIsim: String, varsayilanAvatar: String) {
        val bulutProfil = onBulutRestoreDene()
        onGirisYapildi(
            bulutProfil?.isim?.takeIf { it.isNotBlank() } ?: varsayilanIsim,
            bulutProfil?.avatar?.takeIf { it.isNotBlank() } ?: varsayilanAvatar
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(if (sekme == 0) "🌱" else seciliAvatar, fontSize = 56.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Ders Fidanı",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "İlerlemen hesabınla güvende — sil, telefon değiştir, fark etmez.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))

        TabRow(selectedTabIndex = sekme, modifier = Modifier.clip(RoundedCornerShape(10.dp))) {
            Tab(selected = sekme == 0, onClick = { sekme = 0; hataMesaji = null }, text = { Text("Giriş Yap") })
            Tab(selected = sekme == 1, onClick = { sekme = 1; hataMesaji = null }, text = { Text("Kayıt Ol") })
        }
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = kullaniciAdi,
            onValueChange = { kullaniciAdi = it.trimStart() },
            label = { Text("Kullanıcı adı") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = sifre,
            onValueChange = { sifre = it },
            label = { Text("Şifre") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (sekme == 1) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = isim,
                onValueChange = { isim = it },
                label = { Text("Adın") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Text("Bir profil fotoğrafı seç", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.height(160.dp)
            ) {
                items(ProfilPrefs.avatarSecenekleri) { avatar ->
                    val secili = avatar == seciliAvatar
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (secili) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { seciliAvatar = avatar },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(avatar, fontSize = 20.sp)
                    }
                }
            }
        }

        if (hataMesaji != null) {
            Spacer(Modifier.height(12.dp))
            Text(hataMesaji ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            enabled = !yukleniyor && kullaniciAdi.length >= 3 && sifre.length >= 6,
            onClick = {
                hataMesaji = null
                yukleniyor = true
                kapsam.launch {
                    try {
                        if (sekme == 0) {
                            BulutHesap.girisYap(kullaniciAdi, sifre)
                            girisAkisiTamamla(varsayilanIsim = kullaniciAdi.trim().ifBlank { "Öğrenci" }, varsayilanAvatar = ProfilPrefs.avatarSecenekleri.first())
                        } else {
                            BulutHesap.kayitOl(kullaniciAdi, sifre)
                            girisAkisiTamamla(
                                varsayilanIsim = isim.ifBlank { "Öğrenci" },
                                varsayilanAvatar = seciliAvatar
                            )
                        }
                    } catch (e: Exception) {
                        hataMesaji = when {
                            e.message?.contains("password", true) == true -> "Şifre en az 6 karakter olmalı."
                            e.message?.contains("email address is already", true) == true -> "Bu kullanıcı adı zaten kayıtlı. Giriş Yap'ı dene."
                            e.message?.contains("no user record", true) == true ||
                                e.message?.contains("password is invalid", true) == true ||
                                e.message?.contains("INVALID_LOGIN", true) == true -> "Kullanıcı adı veya şifre hatalı."
                            e.message?.contains("badly formatted", true) == true -> "Kullanıcı adı geçersiz görünüyor."
                            e.message?.contains("network", true) == true -> "İnternet bağlantısı yok — tekrar dene."
                            else -> "Bir şeyler ters gitti: ${e.message ?: "bilinmeyen hata"}"
                        }
                    } finally {
                        yukleniyor = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (yukleniyor) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(if (sekme == 0) "Giriş Yap" else "Kayıt Ol ve Başla")
            }
        }

        if (sifre.isNotEmpty() && sifre.length < 6) {
            Spacer(Modifier.height(6.dp))
            Text("Şifre en az 6 karakter olmalı.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Şifre unutulursa gerçek e-posta kullanılmadığı için e-posta ile kurtarma yapılamaz.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
