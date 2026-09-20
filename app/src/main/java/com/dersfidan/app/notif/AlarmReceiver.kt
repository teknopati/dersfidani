package com.dersfidan.app.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dersfidan.app.MainActivity
import com.dersfidan.app.R

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val blokId = intent.getLongExtra("blokId", -1L)
        val baslik = intent.getStringExtra("baslik") ?: "Ders Fidanı"
        val mesaj = intent.getStringExtra("mesaj") ?: "Programındaki yeni blok başladı"

        val sesUri = Uri.parse("android.resource://${context.packageName}/${R.raw.bildirim}")

        // Kanal ID'si değiştirildi (v3): Android mevcut kanalın sesini sonradan
        // değiştirmediği için özel bildirim sesi yeni kanalla etkinleşir.
        // oluşmuş kanal varsa Android onu değiştirtmez; yeni ID ile sesli kanal
        // sıfırdan oluşturulur.
        val channelId = "ders_fidan_kanal_v3_ozel_ses"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val sesOzellikleri = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(
                channelId, "Program Hatırlatmaları", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Programdaki her bloğun başlangıcında hatırlatma"
                enableVibration(true)
                setSound(sesUri, sesOzellikleri)
            }
            nm.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openBlokId", blokId)
        }
        val contentPi = PendingIntent.getActivity(
            context, blokId.toInt(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(baslik)
            .setContentText(mesaj)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mesaj))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .setSound(sesUri) // Android 7 ve altı için; O+ üzerinde ses kanaldan gelir
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .build()

        nm.notify(blokId.toInt(), notification)
    }
}
