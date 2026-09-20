package com.dersfidan.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ciftlik_durumu")
data class CiftlikDurumu(
    @PrimaryKey val id: Int = 1,
    val ad: String = "Çiftliğim",
    val calisanEviSeviye: Int = 1,
    val calisanEviIlerlemePuan: Int = 0,
    val calisanEviHarcananPuan: Int = 0,
    val yemAdedi: Int = 0,
    /** Yem alışverişlerine harcanan toplam puan; bakiye hesabında bir kez düşülür. */
    val yemeHarcananPuan: Int = 0
)
