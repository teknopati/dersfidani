package com.dersfidan.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class HavaTuru { ACIK, BULUTLU, YAGMURLU, KARLI, SISLI, FIRTINALI }

data class HavaSonucu(val sehir: String, val tur: HavaTuru, val sicaklikC: Int)

/** Kullanıcının girdiği şehri kalıcı olarak saklar (cihazda). */
object HavaPrefs {
    private const val DOSYA = "hava_prefs"
    private const val ANAHTAR_SEHIR = "sehir"
    private const val ANAHTAR_TUR = "son_tur"
    private const val ANAHTAR_SICAKLIK = "son_sicaklik"

    private fun prefs(context: Context) = context.getSharedPreferences(DOSYA, Context.MODE_PRIVATE)

    fun sehir(context: Context): String = prefs(context).getString(ANAHTAR_SEHIR, "İstanbul") ?: "İstanbul"

    fun sehirKaydet(context: Context, sehir: String) {
        prefs(context).edit().putString(ANAHTAR_SEHIR, sehir).apply()
    }

    fun sonHavaKaydet(context: Context, sonuc: HavaSonucu) {
        prefs(context).edit().putString(ANAHTAR_TUR, sonuc.tur.name)
            .putInt(ANAHTAR_SICAKLIK, sonuc.sicaklikC).apply()
    }

    fun sonHava(context: Context): HavaSonucu? {
        val ad = prefs(context).getString(ANAHTAR_TUR, null) ?: return null
        val tur = runCatching { HavaTuru.valueOf(ad) }.getOrNull() ?: return null
        return HavaSonucu(sehir(context), tur, prefs(context).getInt(ANAHTAR_SICAKLIK, 0))
    }
}

/**
 * OpenWeatherMap'ten güncel hava durumunu çeker. Kullanıcı çiftlikte hangi şehri
 * girdiyse (konum izni istenmez) o şehrin gerçek anlık durumuna göre çiftlikte
 * yağmur/kar/bulut efekti gösterilir.
 */
object WeatherService {
    private const val API_KEY = "fb26d3886a84b1a1973b8c134dcefb6e"

    suspend fun getir(sehir: String): HavaSonucu? = withContext(Dispatchers.IO) {
        if (sehir.isBlank()) return@withContext null
        try {
            val kodlanmisSehir = URLEncoder.encode(sehir, "UTF-8")
            val url = URL("https://api.openweathermap.org/data/2.5/weather?q=$kodlanmisSehir&appid=$API_KEY&units=metric&lang=tr")
            val baglanti = url.openConnection() as HttpURLConnection
            baglanti.connectTimeout = 8000
            baglanti.readTimeout = 8000
            baglanti.requestMethod = "GET"
            if (baglanti.responseCode != 200) {
                baglanti.disconnect()
                return@withContext null
            }
            val metin = baglanti.inputStream.bufferedReader().use { it.readText() }
            baglanti.disconnect()

            val json = JSONObject(metin)
            val havaObj = json.getJSONArray("weather").getJSONObject(0)
            val kodId = havaObj.getInt("id")
            val sicaklik = json.getJSONObject("main").getDouble("temp").toInt()

            val tur = when (kodId) {
                in 200..232 -> HavaTuru.FIRTINALI
                in 300..531 -> HavaTuru.YAGMURLU
                in 600..622 -> HavaTuru.KARLI
                in 701..781 -> HavaTuru.SISLI
                800 -> HavaTuru.ACIK
                in 801..804 -> HavaTuru.BULUTLU
                else -> HavaTuru.ACIK
            }
            HavaSonucu(sehir = sehir, tur = tur, sicaklikC = sicaklik)
        } catch (e: Exception) {
            null
        }
    }
}
