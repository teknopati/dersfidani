package com.dersfidan.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OdulBonusuDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ekle(odul: OdulBonusu): Long
    @Query("SELECT * FROM odul_bonuslari WHERE kartTuru = :tur AND kartAdi = :ad LIMIT 1")
    suspend fun getir(tur: String, ad: String): OdulBonusu?
    @Query("SELECT COALESCE(SUM(puan), 0) FROM odul_bonuslari")
    fun toplamBonus(): Flow<Int>
    @Query("SELECT * FROM odul_bonuslari")
    suspend fun tumBonuslarListe(): List<OdulBonusu>
    @Query("DELETE FROM odul_bonuslari")
    suspend fun hepsiniSil()
}
