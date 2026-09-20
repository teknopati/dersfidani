package com.dersfidan.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DersNotuDao {

    @Insert
    suspend fun ekle(notu: DersNotu): Long

    @Delete
    suspend fun sil(notu: DersNotu)

    @Query("SELECT * FROM ders_notlari WHERE dersAdi = :dersAdi ORDER BY olusturmaZamani DESC")
    fun dersNotlari(dersAdi: String): Flow<List<DersNotu>>

    @Query("SELECT * FROM ders_notlari ORDER BY olusturmaZamani DESC")
    fun tumNotlar(): Flow<List<DersNotu>>

    @Query("SELECT * FROM ders_notlari ORDER BY olusturmaZamani DESC")
    suspend fun tumNotlarListe(): List<DersNotu>

    @Query("DELETE FROM ders_notlari")
    suspend fun hepsiniSil()

    @Query("UPDATE ders_notlari SET bulutFotoUrl = :url WHERE id = :id")
    suspend fun fotoUrlGuncelle(id: Long, url: String)
}
