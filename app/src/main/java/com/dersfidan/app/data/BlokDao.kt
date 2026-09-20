package com.dersfidan.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BlokDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bloklar: List<Blok>)

    @Query("SELECT COUNT(*) FROM bloklar")
    suspend fun sayim(): Int

    @Query("SELECT * FROM bloklar WHERE tarih = :tarih ORDER BY saatBaslangic ASC")
    fun gunBloklari(tarih: String): Flow<List<Blok>>

    @Query("SELECT DISTINCT tarih FROM bloklar ORDER BY tarih ASC")
    fun tumTarihler(): Flow<List<String>>

    @Query("""
        SELECT tarih, COUNT(*) as toplam,
        SUM(CASE WHEN tamamlandi = 1 THEN 1 ELSE 0 END) as yapilan,
        MAX(CASE WHEN kategori = 'TATİL' THEN 'Tatil'
                 WHEN kategori = 'MOLA GÜNÜ' THEN 'Mola günü' ELSE NULL END) as etiket
        FROM bloklar
        GROUP BY tarih
        ORDER BY tarih ASC
    """)
    fun gunlukOzetler(): Flow<List<GunOzet>>

    @Query("SELECT * FROM bloklar WHERE dersAdi = :dersAdi ORDER BY tarih ASC, saatBaslangic ASC")
    fun dersBloklari(dersAdi: String): Flow<List<Blok>>

    /** Gerçek ders/konu blokları — deneme ve deneme analizi bunun DIŞINDA, ayrı bir grupta izlenir. */
    @Query("""
        SELECT dersAdi as dersAdi, COUNT(*) as toplam,
        SUM(CASE WHEN tamamlandi = 1 THEN 1 ELSE 0 END) as yapilan,
        SUM(CASE WHEN tamamlandi = 1 THEN COALESCE(sureDk,0) ELSE 0 END) as yapilanDk,
        SUM(COALESCE(sureDk,0)) as toplamDk
        FROM bloklar
        WHERE dersAdi IS NOT NULL AND kategori NOT IN ('Deneme', 'Deneme Analizi')
        GROUP BY dersAdi
        ORDER BY toplam DESC
    """)
    fun dersOzetleri(): Flow<List<DersOzet>>

    /** Denemeler (branş denemesi + deneme analizi) — her ders/sınav için kendi ayrı ilerleme kartı. */
    @Query("""
        SELECT dersAdi as dersAdi, COUNT(*) as toplam,
        SUM(CASE WHEN tamamlandi = 1 THEN 1 ELSE 0 END) as yapilan,
        SUM(CASE WHEN tamamlandi = 1 THEN COALESCE(sureDk,0) ELSE 0 END) as yapilanDk,
        SUM(COALESCE(sureDk,0)) as toplamDk
        FROM bloklar
        WHERE dersAdi IS NOT NULL AND kategori IN ('Deneme', 'Deneme Analizi')
        GROUP BY dersAdi
        ORDER BY toplam DESC
    """)
    fun denemeOzetleri(): Flow<List<DersOzet>>

    @Query("""
        SELECT dersAdi as dersAdi, COUNT(*) as toplam,
        SUM(CASE WHEN tamamlandi = 1 THEN 1 ELSE 0 END) as yapilan,
        SUM(CASE WHEN tamamlandi = 1 THEN COALESCE(sureDk,0) ELSE 0 END) as yapilanDk,
        SUM(COALESCE(sureDk,0)) as toplamDk
        FROM bloklar
        WHERE dersAdi = :dersAdi AND kategori NOT IN ('Deneme', 'Deneme Analizi')
        GROUP BY dersAdi LIMIT 1
    """)
    suspend fun dersKartOzeti(dersAdi: String): DersOzet?

    @Query("""
        SELECT dersAdi as dersAdi, COUNT(*) as toplam,
        SUM(CASE WHEN tamamlandi = 1 THEN 1 ELSE 0 END) as yapilan,
        SUM(CASE WHEN tamamlandi = 1 THEN COALESCE(sureDk,0) ELSE 0 END) as yapilanDk,
        SUM(COALESCE(sureDk,0)) as toplamDk
        FROM bloklar
        WHERE dersAdi = :dersAdi AND kategori IN ('Deneme', 'Deneme Analizi')
        GROUP BY dersAdi LIMIT 1
    """)
    suspend fun denemeKartOzeti(dersAdi: String): DersOzet?

    @Query("""
        SELECT dersAdi as dersAdi, COUNT(*) as toplam,
        SUM(CASE WHEN tamamlandi = 1 THEN 1 ELSE 0 END) as yapilan,
        SUM(CASE WHEN tamamlandi = 1 THEN COALESCE(sureDk,0) ELSE 0 END) as yapilanDk,
        SUM(COALESCE(sureDk,0)) as toplamDk
        FROM bloklar WHERE dersAdi IS NOT NULL AND kategori NOT IN ('Deneme', 'Deneme Analizi')
        GROUP BY dersAdi HAVING yapilan = toplam
    """)
    suspend fun tamamlananDersKartlari(): List<DersOzet>

    @Query("""
        SELECT dersAdi as dersAdi, COUNT(*) as toplam,
        SUM(CASE WHEN tamamlandi = 1 THEN 1 ELSE 0 END) as yapilan,
        SUM(CASE WHEN tamamlandi = 1 THEN COALESCE(sureDk,0) ELSE 0 END) as yapilanDk,
        SUM(COALESCE(sureDk,0)) as toplamDk
        FROM bloklar WHERE dersAdi IS NOT NULL AND kategori IN ('Deneme', 'Deneme Analizi')
        GROUP BY dersAdi HAVING yapilan = toplam
    """)
    suspend fun tamamlananDenemeKartlari(): List<DersOzet>

    @Query("SELECT COUNT(*) FROM bloklar WHERE dersAdi IS NOT NULL AND tamamlandi = 1")
    fun tamamlananBlokSayisi(): Flow<Int>

    /** Bulut yedeğine göndermek için: tamamlanmış tüm bloklar (senkron anahtarı: tarih+saat+kategori). */
    @Query("SELECT * FROM bloklar WHERE tamamlandi = 1")
    suspend fun tamamlananBloklarListe(): List<Blok>

    @Query("SELECT * FROM bloklar WHERE tamamlandi = 1")
    fun tamamlananBloklarAkis(): Flow<List<Blok>>

    @Query("UPDATE bloklar SET tamamlandi = 0, tamamlanmaZamani = NULL")
    suspend fun tumTamamlamalariSifirla()

    /** Buluttan geri yüklerken: aynı gün/saat/kategoriye sahip bloğu tamamlandı olarak işaretler. */
    @Query("""
        UPDATE bloklar SET tamamlandi = 1, tamamlanmaZamani = :zaman
        WHERE tarih = :tarih AND saatBaslangic = :saatBaslangic AND kategori = :kategori
    """)
    suspend fun isaretleAnahtarIle(tarih: String, saatBaslangic: String, kategori: String, zaman: Long)

    @Query("SELECT COUNT(*) FROM bloklar WHERE dersAdi IS NOT NULL")
    fun izlenenToplam(): Flow<Int>

    @Update
    suspend fun guncelle(blok: Blok)

    @Query("UPDATE bloklar SET tamamlandi = :yapildi, tamamlanmaZamani = :zaman WHERE id = :id")
    suspend fun isaretle(id: Long, yapildi: Boolean, zaman: Long?)

    @Query("SELECT * FROM bloklar WHERE id = :id")
    suspend fun getirById(id: Long): Blok?

    @Query("""
        SELECT * FROM bloklar
        WHERE tarih = :tarih AND saatBaslangic >= :suan
        ORDER BY saatBaslangic ASC
    """)
    suspend fun bugunGelecekBloklar(tarih: String, suan: String): List<Blok>

    @Query("SELECT * FROM bloklar WHERE tarih = :tarih ORDER BY saatBaslangic ASC")
    suspend fun gunBloklariListe(tarih: String): List<Blok>

    @Delete
    suspend fun sil(blok: Blok)

    @Insert
    suspend fun ekle(blok: Blok): Long
}

data class GunOzet(val tarih: String, val toplam: Int, val yapilan: Int, val etiket: String?)

data class DersOzet(
    val dersAdi: String,
    val toplam: Int,
    val yapilan: Int,
    val yapilanDk: Int,
    val toplamDk: Int
)
