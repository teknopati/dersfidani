package com.dersfidan.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CiftlikDurumuDao {
    @Query("SELECT * FROM ciftlik_durumu WHERE id = 1")
    fun akis(): Flow<CiftlikDurumu?>

    @Query("SELECT * FROM ciftlik_durumu WHERE id = 1")
    suspend fun getir(): CiftlikDurumu?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun kaydet(durum: CiftlikDurumu)

    @Query("DELETE FROM ciftlik_durumu")
    suspend fun temizle()
}
