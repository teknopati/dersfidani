package com.dersfidan.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CiftlikCalisaniDao {
    @Query("SELECT * FROM ciftlik_calisanlari ORDER BY alinmaZamani")
    fun akis(): Flow<List<CiftlikCalisani>>

    @Query("SELECT * FROM ciftlik_calisanlari ORDER BY alinmaZamani")
    suspend fun liste(): List<CiftlikCalisani>

    @Query("SELECT * FROM ciftlik_calisanlari WHERE id = :id")
    suspend fun getir(id: Long): CiftlikCalisani?

    @Insert
    suspend fun ekle(calisan: CiftlikCalisani): Long

    @Query("UPDATE ciftlik_calisanlari SET ad = :ad WHERE id = :id")
    suspend fun adGuncelle(id: Long, ad: String)

    @Query("UPDATE ciftlik_calisanlari SET ilerlemePuan = ilerlemePuan + :miktar, harcananPuan = harcananPuan + :miktar WHERE id = :id AND seviye < 10")
    suspend fun puanYatir(id: Long, miktar: Int): Int

    @Query("UPDATE ciftlik_calisanlari SET seviye = seviye + 1, ilerlemePuan = 0 WHERE id = :id AND seviye < 10")
    suspend fun seviyeTamamla(id: Long): Int

    @Query("DELETE FROM ciftlik_calisanlari")
    suspend fun temizle()
}
