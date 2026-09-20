package com.dersfidan.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GunBonusuDao {
    @Query("SELECT * FROM gun_bonuslari WHERE tarih = :tarih LIMIT 1")
    suspend fun getir(tarih: String): GunBonusu?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ekle(bonus: GunBonusu): Long

    @Query("SELECT COALESCE(SUM(puan), 0) FROM gun_bonuslari")
    fun toplamBonus(): Flow<Int>

    @Query("SELECT * FROM gun_bonuslari")
    suspend fun tumBonuslarListe(): List<GunBonusu>

    @Query("DELETE FROM gun_bonuslari")
    suspend fun hepsiniSil()
}
