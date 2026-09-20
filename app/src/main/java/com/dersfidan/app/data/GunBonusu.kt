package com.dersfidan.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * O gün için verilmiş "günü tamamlama" bonusu. Bir tarihe sadece bir kez verilir
 * (o günün TÜM blokları -mola/uyku dahil- tamamlandi=true olduğunda).
 */
@Entity(tableName = "gun_bonuslari")
data class GunBonusu(
    @PrimaryKey val tarih: String,
    val puan: Int,
    val zaman: Long = System.currentTimeMillis()
)
