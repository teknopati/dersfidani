package com.dersfidan.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ciftlik_calisanlari")
data class CiftlikCalisani(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tip: Int,
    val ad: String,
    val rol: String,
    val seviye: Int = 1,
    val ilerlemePuan: Int = 0,
    val harcananPuan: Int = 40,
    val alinmaZamani: Long = System.currentTimeMillis()
)
