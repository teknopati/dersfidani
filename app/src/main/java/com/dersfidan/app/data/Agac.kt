package com.dersfidan.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Çiftlikteki bağımsız bir ağaç. Her ağaç kendi büyüme puanına (yatirilanPuan)
 * sahiptir; kullanıcı bankadaki puanını ağaca dokunarak/uzun basarak yatırır.
 * satinAlmaFiyati: bu ağacı eklerken bankadan bir seferlik düşülen puan (ilk ağaç 0/ücretsizdir).
 */
@Entity(tableName = "agaclar")
data class Agac(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val isim: String = "Ağaç",
    val yatirilanPuan: Int = 0,
    val satinAlmaFiyati: Int = 0,
    /** AgacKatalog içindeki 0..99 tür numarası. */
    val turId: Int = 0,
    val konumX: Float = 0f,
    val konumZ: Float = 0f,
    val olusturmaZamani: Long = System.currentTimeMillis()
)
