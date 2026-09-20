package com.dersfidan.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Blok::class, DersNotu::class, Canli::class, Agac::class, GunBonusu::class,
        OdulBonusu::class, CiftlikDurumu::class, CiftlikCalisani::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blokDao(): BlokDao
    abstract fun dersNotuDao(): DersNotuDao
    abstract fun canliDao(): CanliDao
    abstract fun agacDao(): AgacDao
    abstract fun gunBonusuDao(): GunBonusuDao
    abstract fun odulBonusuDao(): OdulBonusuDao
    abstract fun ciftlikDurumuDao(): CiftlikDurumuDao
    abstract fun ciftlikCalisaniDao(): CiftlikCalisaniDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dersfidan.db"
                )
                    // v1 -> v2: sadece yeni "canlilar" tablosu eklendi.
                    // v2 -> v3: Çiftlik dönüşümü - çoklu ağaç ("agaclar") ve gün bonusu
                    // ("gun_bonuslari") tabloları eklendi. Mevcut ilerleme/hayvan verisi korunur.
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `canlilar` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `esyaId` TEXT NOT NULL,
                        `satinAlmaZamani` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `agaclar` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `isim` TEXT NOT NULL,
                        `yatirilanPuan` INTEGER NOT NULL,
                        `satinAlmaFiyati` INTEGER NOT NULL,
                        `olusturmaZamani` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `gun_bonuslari` (
                        `tarih` TEXT PRIMARY KEY NOT NULL,
                        `puan` INTEGER NOT NULL,
                        `zaman` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `odul_bonuslari` (
                        `kartTuru` TEXT NOT NULL,
                        `kartAdi` TEXT NOT NULL,
                        `puan` INTEGER NOT NULL,
                        `zaman` INTEGER NOT NULL,
                        PRIMARY KEY(`kartTuru`, `kartAdi`)
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE `agaclar` ADD COLUMN `turId` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `ders_notlari` ADD COLUMN `bulutFotoUrl` TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `canlilar` ADD COLUMN `ad` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `canlilar` ADD COLUMN `satinAlmaFiyati` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `canlilar` ADD COLUMN `konumX` REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `canlilar` ADD COLUMN `konumZ` REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `agaclar` ADD COLUMN `konumX` REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `agaclar` ADD COLUMN `konumZ` REAL NOT NULL DEFAULT 0")

                // Kullanıcının modelini göndermediği eski katalog hayvanlarını güvenle kaldır.
                db.execSQL(
                    "DELETE FROM `canlilar` WHERE `esyaId` IN " +
                        "('yusufcuk','kirlangic','kirpi','ates_bocegi','domuz','tavus'," +
                        "'gecetavsani','guve','lama','midilli','kaz','guvercin','leylek'," +
                        "'balik','sazan','hamster','karaca','bukalemun','ipekbocegi','pelikan')"
                )
                db.execSQL("UPDATE `canlilar` SET `esyaId` = 'kurbaga' WHERE `esyaId` = 'kurbağa'")

                // Eski kayıtlar ilk açılışta üst üste binmesin.
                db.execSQL(
                    "UPDATE `canlilar` SET `konumX` = -10 + ((`id` - 1) % 6) * 3.5, " +
                        "`konumZ` = -5.5 + (((`id` - 1) / 6) % 4) * 3.2"
                )
                db.execSQL(
                    "UPDATE `agaclar` SET `konumX` = -11 + ((`id` - 1) % 7) * 3.6, " +
                        "`konumZ` = 9 - (((`id` - 1) / 7) % 3) * 3.5"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `canlilar` ADD COLUMN `yas` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `canlilar` ADD COLUMN `yasHarcananPuan` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `canlilar` ADD COLUMN `seviye` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `canlilar` ADD COLUMN `seviyeHarcananPuan` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `canlilar` ADD COLUMN `yasIlerlemePuan` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `canlilar` ADD COLUMN `seviyeIlerlemePuan` INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ciftlik_durumu` (
                        `id` INTEGER NOT NULL,
                        `ad` TEXT NOT NULL,
                        `calisanEviSeviye` INTEGER NOT NULL,
                        `calisanEviIlerlemePuan` INTEGER NOT NULL,
                        `calisanEviHarcananPuan` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ciftlik_calisanlari` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `tip` INTEGER NOT NULL,
                        `ad` TEXT NOT NULL,
                        `rol` TEXT NOT NULL,
                        `seviye` INTEGER NOT NULL,
                        `ilerlemePuan` INTEGER NOT NULL,
                        `harcananPuan` INTEGER NOT NULL,
                        `alinmaZamani` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("INSERT OR IGNORE INTO `ciftlik_durumu` (`id`,`ad`,`calisanEviSeviye`,`calisanEviIlerlemePuan`,`calisanEviHarcananPuan`) VALUES (1,'Çiftliğim',1,0,0)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val simdi = System.currentTimeMillis()
                db.execSQL("ALTER TABLE `canlilar` ADD COLUMN `tokluk` INTEGER NOT NULL DEFAULT 70")
                db.execSQL("ALTER TABLE `canlilar` ADD COLUMN `sevgi` INTEGER NOT NULL DEFAULT 60")
                db.execSQL("ALTER TABLE `canlilar` ADD COLUMN `sonBeslenmeZamani` INTEGER NOT NULL DEFAULT $simdi")
                db.execSQL("ALTER TABLE `canlilar` ADD COLUMN `sonSevilmeZamani` INTEGER NOT NULL DEFAULT $simdi")
                db.execSQL("ALTER TABLE `ciftlik_durumu` ADD COLUMN `yemAdedi` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `ciftlik_durumu` ADD COLUMN `yemeHarcananPuan` INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
