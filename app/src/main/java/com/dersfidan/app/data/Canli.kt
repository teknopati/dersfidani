package com.dersfidan.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Market'ten satın alınıp fidanın etrafında yaşayan hareketli canlı.
 * esyaId, MarketKatalog içindeki sabit bir MarketEsyasi.id'sine karşılık gelir.
 */
@Entity(tableName = "canlilar")
data class Canli(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val esyaId: String,
    /** Hayvanlara satın alırken verilen zorunlu ad; yapılarda yapı adı tutulur. */
    val ad: String = "",
    /** Bu örnek için gerçekten ödenen puan. */
    val satinAlmaFiyati: Int = 0,
    /** Hayvanlarda bireysel yaş; yapılarda kullanılmaz. */
    val yas: Int = 1,
    /** Bu hayvanın yaş yükseltmeleri için toplam harcanan puan. */
    val yasHarcananPuan: Int = 0,
    /** Sıradaki yaş için biriken, henüz yaş atlatmamış kısmi yatırım. */
    val yasIlerlemePuan: Int = 0,
    /** Yapı seviyesi; hayvan kayıtlarında ortak habitat seviyesini taşır. */
    val seviye: Int = 1,
    /** Yapı/habitat geliştirmeleri için toplam harcanan puan. */
    val seviyeHarcananPuan: Int = 0,
    /** Sıradaki yapı/habitat seviyesi için biriken kısmi yatırım. */
    val seviyeIlerlemePuan: Int = 0,
    /** Yapının veya ortak hayvan habitatının kalıcı merkezi. */
    val konumX: Float = 0f,
    val konumZ: Float = 0f,
    /** 0..100 arası bakım değerleri; zaman geçtikçe ekranda yavaşça azalır. */
    val tokluk: Int = 70,
    val sevgi: Int = 60,
    val sonBeslenmeZamani: Long = System.currentTimeMillis(),
    val sonSevilmeZamani: Long = System.currentTimeMillis(),
    val satinAlmaZamani: Long = System.currentTimeMillis()
)
