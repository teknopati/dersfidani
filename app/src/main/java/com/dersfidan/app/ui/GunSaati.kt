package com.dersfidan.app.ui

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import java.util.Calendar

data class GunSaati(val saat: Int, val dakika: Int) {
    val gece: Boolean get() = saat >= 20 || saat < 6
    val sabah: Boolean get() = saat in 6..9
    val aksam: Boolean get() = saat in 17..19
    val gokCismiOrani: Float get() {
        val dk = saat * 60 + dakika
        return if (!gece) ((dk - 360).toFloat() / 780).coerceIn(0f, 1f)
        else ((if (saat >= 20) dk - 1200 else dk + 240).toFloat() / 600).coerceIn(0f, 1f)
    }
}

@Composable
fun rememberGunSaati(): GunSaati {
    var durum by remember {
        val c = Calendar.getInstance()
        mutableStateOf(GunSaati(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)))
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            val c = Calendar.getInstance()
            durum = GunSaati(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
        }
    }
    return durum
}
