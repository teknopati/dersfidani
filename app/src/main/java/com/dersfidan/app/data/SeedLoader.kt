package com.dersfidan.app.data

import android.content.Context
import org.json.JSONArray

object SeedLoader {

    /**
     * assets/program_seed.json dosyasını okuyup Blok listesine çevirir.
     * Veritabanı boşsa (ilk açılış) bu liste toplu olarak eklenir.
     */
    suspend fun ilkYuklemeYapGerekirse(context: Context, db: AppDatabase) {
        val dao = db.blokDao()
        if (dao.sayim() > 0) return

        val json = context.assets.open("program_seed.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = JSONArray(json)
        val liste = ArrayList<Blok>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            liste.add(
                Blok(
                    tarih = o.getString("tarih"),
                    gun = o.optString("gun", ""),
                    saatBaslangic = o.optString("saatBaslangic", ""),
                    saatBitis = o.optString("saatBitis", ""),
                    kategori = o.optString("kategori", ""),
                    dersAdi = if (o.isNull("dersAdi")) null else o.getString("dersAdi"),
                    metin = o.optString("metin", ""),
                    sureDk = if (o.isNull("sureDk")) null else o.getInt("sureDk")
                )
            )
        }
        dao.insertAll(liste)
    }
}
