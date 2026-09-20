package com.dersfidan.app.data

import androidx.room.Entity

/** Bir ders/deneme kartı ilk kez tamamen bittiğinde verilen tek seferlik ödül. */
@Entity(tableName = "odul_bonuslari", primaryKeys = ["kartTuru", "kartAdi"])
data class OdulBonusu(
    val kartTuru: String,
    val kartAdi: String,
    val puan: Int,
    val zaman: Long = System.currentTimeMillis()
)
