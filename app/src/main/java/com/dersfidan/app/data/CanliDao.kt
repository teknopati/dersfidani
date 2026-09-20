package com.dersfidan.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CanliDao {
    @Query("SELECT * FROM canlilar ORDER BY satinAlmaZamani ASC")
    fun tumCanlilar(): Flow<List<Canli>>

    @Insert
    suspend fun ekle(canli: Canli): Long

    @Query("SELECT * FROM canlilar WHERE id = :id LIMIT 1")
    suspend fun getir(id: Long): Canli?

    @Query("SELECT MAX(seviye) FROM canlilar WHERE esyaId = :esyaId")
    suspend fun habitatSeviyesi(esyaId: String): Int?

    @Query("SELECT COUNT(*) FROM canlilar WHERE esyaId = :esyaId")
    suspend fun sahipMi(esyaId: String): Int

    @Query("UPDATE canlilar SET konumX = :x, konumZ = :z WHERE id = :id")
    suspend fun konumGuncelle(id: Long, x: Float, z: Float)

    @Query("UPDATE canlilar SET konumX = :x, konumZ = :z WHERE esyaId = :esyaId")
    suspend fun habitatKonumGuncelle(esyaId: String, x: Float, z: Float)

    @Query("UPDATE canlilar SET ad = :ad WHERE id = :id")
    suspend fun adGuncelle(id: Long, ad: String)

    @Query("UPDATE canlilar SET yasIlerlemePuan = yasIlerlemePuan + :miktar, yasHarcananPuan = yasHarcananPuan + :miktar WHERE id = :id AND yas < 20")
    suspend fun yasPuaniYatir(id: Long, miktar: Int): Int

    @Query("UPDATE canlilar SET yas = yas + 1, yasIlerlemePuan = 0 WHERE id = :id AND yas < 20")
    suspend fun yasTamamla(id: Long): Int

    /** Beslemeden gelen ücretsiz bakım ilerlemesi, bankadan puan harcamaz. */
    @Query("UPDATE canlilar SET yasIlerlemePuan = yasIlerlemePuan + :miktar WHERE id = :id AND yas < 20")
    suspend fun bakimIlerlemesiEkle(id: Long, miktar: Int): Int

    @Query("UPDATE canlilar SET tokluk = :tokluk, sevgi = :sevgi, sonBeslenmeZamani = :zaman, sonSevilmeZamani = :zaman WHERE id = :id")
    suspend fun beslenmeGuncelle(id: Long, tokluk: Int, sevgi: Int, zaman: Long): Int

    @Query("UPDATE canlilar SET sevgi = :sevgi, sonSevilmeZamani = :zaman WHERE id = :id")
    suspend fun sevgiGuncelle(id: Long, sevgi: Int, zaman: Long): Int

    @Query("UPDATE canlilar SET seviyeIlerlemePuan = seviyeIlerlemePuan + :miktar, seviyeHarcananPuan = seviyeHarcananPuan + :miktar WHERE id = :id AND seviye < 10")
    suspend fun yapiPuaniYatir(id: Long, miktar: Int): Int

    @Query("UPDATE canlilar SET seviye = seviye + 1, seviyeIlerlemePuan = 0 WHERE id = :id AND seviye < 10")
    suspend fun yapiSeviyeTamamla(id: Long): Int

    @Query("UPDATE canlilar SET seviyeIlerlemePuan = seviyeIlerlemePuan + :miktar, seviyeHarcananPuan = seviyeHarcananPuan + :miktar WHERE esyaId = :esyaId AND seviye < 10")
    suspend fun habitatPuaniYatir(esyaId: String, miktar: Int): Int

    @Query("UPDATE canlilar SET seviye = seviye + 1, seviyeIlerlemePuan = 0 WHERE esyaId = :esyaId AND seviye < 10")
    suspend fun habitatSeviyeTamamla(esyaId: String): Int

    @Query("UPDATE canlilar SET satinAlmaFiyati = :fiyat WHERE id = :id")
    suspend fun fiyatGuncelle(id: Long, fiyat: Int)

    @Query("UPDATE canlilar SET yas = yas + 1, yasHarcananPuan = yasHarcananPuan + :maliyet WHERE id = :id AND yas < 20")
    suspend fun yasArtir(id: Long, maliyet: Int): Int

    @Query("UPDATE canlilar SET seviye = seviye + 1, seviyeHarcananPuan = seviyeHarcananPuan + :maliyet WHERE id = :id AND seviye < 10")
    suspend fun yapiSeviyeArtir(id: Long, maliyet: Int): Int

    /** Aynı türün bütün kayıtları tek habitatı temsil ettiği için seviyeyi birlikte yükseltir. */
    @Query("UPDATE canlilar SET seviye = seviye + 1, seviyeHarcananPuan = seviyeHarcananPuan + :maliyet WHERE esyaId = :esyaId AND seviye < 10")
    suspend fun habitatSeviyeArtir(esyaId: String, maliyet: Int): Int

    @Query("SELECT * FROM canlilar ORDER BY satinAlmaZamani ASC")
    suspend fun tumCanlilarListe(): List<Canli>

    /** Her bir eşya türünden kaç adet alındığını döner (Market'te "x3" gibi göstermek için). */
    @Query("SELECT esyaId, COUNT(*) as adet FROM canlilar GROUP BY esyaId")
    fun adetOzetleri(): Flow<List<CanliAdet>>

    @Query("DELETE FROM canlilar")
    suspend fun hepsiniSil()
}

data class CanliAdet(val esyaId: String, val adet: Int)
