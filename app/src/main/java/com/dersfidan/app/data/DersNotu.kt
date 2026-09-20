package com.dersfidan.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Bir derse ait "yanlışlarım / dikkat edilmesi gerekenler" notu.
 * fotoYolu: cihazdaki dosya yolu (opsiyonel, çekilen soru fotoğrafı).
 */
@Entity(tableName = "ders_notlari")
data class DersNotu(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dersAdi: String,
    val metin: String,
    val fotoYolu: String? = null,
    /** Firebase Storage'a yüklenmiş fotoğrafın cihazlar arası URL'si. */
    val bulutFotoUrl: String? = null,
    val olusturmaZamani: Long = System.currentTimeMillis()
)
