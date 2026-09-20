package com.dersfidan.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AgacDao {
    @Query("SELECT * FROM agaclar ORDER BY olusturmaZamani ASC")
    fun tumAgaclar(): Flow<List<Agac>>

    @Insert
    suspend fun ekle(agac: Agac): Long

    @Query("SELECT COUNT(*) FROM agaclar")
    suspend fun sayim(): Int

    @Query("SELECT * FROM agaclar ORDER BY olusturmaZamani ASC")
    suspend fun tumAgaclarListe(): List<Agac>

    @Query("UPDATE agaclar SET yatirilanPuan = yatirilanPuan + :miktar WHERE id = :id")
    suspend fun puanYatir(id: Long, miktar: Int)

    @Query("UPDATE agaclar SET konumX = :x, konumZ = :z WHERE id = :id")
    suspend fun konumGuncelle(id: Long, x: Float, z: Float)

    /** Sadece bulut yedeğinden ilk kez geri yükleme yaparken kullanılır. */
    @Query("DELETE FROM agaclar")
    suspend fun hepsiniSil()
}
