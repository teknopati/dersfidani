package com.dersfidan.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * tarih (yyyy-MM-dd) -> "13 Ocak 2027 Pazartesi" gibi tam, okunabilir tarih.
 */
object TarihUtil {
    private val isoFmt = SimpleDateFormat("yyyy-MM-dd", Locale("tr"))
    private val tamFmt = SimpleDateFormat("d MMMM yyyy, EEEE", Locale("tr"))

    fun tamTarih(tarihIso: String): String {
        val d: Date = try { isoFmt.parse(tarihIso) ?: return tarihIso } catch (e: Exception) { return tarihIso }
        return tamFmt.format(d).replaceFirstChar { it.uppercase(Locale("tr")) }
    }

    fun bugunIso(): String = isoFmt.format(Date())

    /** tarih, bugünden SONRAKİ bir gün mü (henüz gelmemiş)? */
    fun gelecekMi(tarihIso: String): Boolean {
        val bugun = isoFmt.parse(bugunIso()) ?: return false
        val d = try { isoFmt.parse(tarihIso) ?: return false } catch (e: Exception) { return false }
        return d.after(bugun)
    }
}
