package com.dersfidan.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Programdaki tek bir zaman bloğu (ör: "09:00-09:24 Sözel Yetenek video 6").
 * tarih formatı: yyyy-MM-dd
 */
@Entity(tableName = "bloklar")
data class Blok(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tarih: String,
    val gun: String,
    val saatBaslangic: String,
    val saatBitis: String,
    val kategori: String,
    // Trackable bir derse ait değilse null (Mola, Yemek, Uyku vb. rutin bloklar)
    val dersAdi: String?,
    val metin: String,
    val sureDk: Int?,
    val tamamlandi: Boolean = false,
    val tamamlanmaZamani: Long? = null
)
