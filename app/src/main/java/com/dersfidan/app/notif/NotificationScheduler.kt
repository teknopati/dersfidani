package com.dersfidan.app.notif

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.dersfidan.app.data.Blok
import com.dersfidan.app.data.Repository
import java.text.SimpleDateFormat
import java.util.*

/**
 * Sadece "bugün + yarın" için alarm kurar (binlerce alarm birden kurmak yerine).
 * DailyRescheduleWorker her gün bunu tekrar çağırarak ileriyi tazeler.
 */
object NotificationScheduler {

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale("tr"))
    private val timeFmt = SimpleDateFormat("HH:mm", Locale("tr"))

    suspend fun planlaBugunVeYarin(context: Context, repo: Repository) {
        val cal = Calendar.getInstance()
        val bugun = dateFmt.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val yarin = dateFmt.format(cal.time)

        val bugunBloklari = repo.gunBloklariListe(bugun)
        val yarinBloklari = repo.gunBloklariListe(yarin)

        (bugunBloklari + yarinBloklari).forEach { blok ->
            if (!blok.tamamlandi) planlaBlok(context, blok)
        }
    }

    fun planlaBlok(context: Context, blok: Blok) {
        val zaman = zamanMillis(blok.tarih, blok.saatBaslangic) ?: return
        if (zaman < System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("blokId", blok.id)
            putExtra("baslik", blok.dersAdi ?: blok.kategori)
            putExtra("mesaj", "${blok.saatBaslangic} - ${kisalt(blok.metin)}")
        }
        val pi = PendingIntent.getBroadcast(
            context, blok.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zaman, pi)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zaman, pi)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zaman, pi)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zaman, pi)
        }
    }

    fun iptalEt(context: Context, blokId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, blokId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    private fun zamanMillis(tarih: String, saat: String): Long? {
        return try {
            val d = dateFmt.parse(tarih) ?: return null
            val t = timeFmt.parse(saat) ?: return null
            val cal = Calendar.getInstance()
            cal.time = d
            val tCal = Calendar.getInstance()
            tCal.time = t
            cal.set(Calendar.HOUR_OF_DAY, tCal.get(Calendar.HOUR_OF_DAY))
            cal.set(Calendar.MINUTE, tCal.get(Calendar.MINUTE))
            cal.set(Calendar.SECOND, 0)
            cal.timeInMillis
        } catch (e: Exception) {
            null
        }
    }

    private fun kisalt(s: String) = if (s.length > 60) s.substring(0, 60) + "…" else s
}
