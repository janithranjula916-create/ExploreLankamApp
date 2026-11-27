package com.example.explorelanka.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.example.explorelanka.data.local.dao.TripDao
import com.example.explorelanka.data.local.dao.WeatherDao
import com.example.explorelanka.data.local.dao.JournalDao
import com.example.explorelanka.data.local.dao.ScanHistoryDao
import com.example.explorelanka.data.local.entity.TripEntity
import com.example.explorelanka.data.local.entity.TripStopEntity
import com.example.explorelanka.data.local.entity.WeatherCacheEntity
import com.example.explorelanka.data.local.entity.JournalPhotoEntity
import com.example.explorelanka.data.local.entity.ScanHistoryEntity
import com.example.explorelanka.data.local.entity.SmartPlanEntity
import com.example.explorelanka.data.local.entity.SmartPlanStopEntity
import com.example.explorelanka.data.local.dao.SmartPlanDao

@Database(
    entities = [
        TripEntity::class, TripStopEntity::class, WeatherCacheEntity::class,
        JournalPhotoEntity::class, ScanHistoryEntity::class,
        SmartPlanEntity::class, SmartPlanStopEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class ExploreLankaDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun weatherDao(): WeatherDao
    abstract fun journalDao(): JournalDao
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun smartPlanDao(): SmartPlanDao

    companion object {
        // Preserve the Step 3 -> Step 4 migration.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE trips ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
                connection.execSQL("ALTER TABLE trips ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Add weather without dropping or clearing saved trips.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """CREATE TABLE IF NOT EXISTS `weather_cache` (
                        `placeId` TEXT NOT NULL,
                        `temperatureCelsius` REAL NOT NULL,
                        `humidityPercent` INTEGER,
                        `weatherCode` INTEGER,
                        `windKmh` REAL,
                        `isDay` INTEGER,
                        `observedAtLocal` TEXT,
                        `maxTemperatureCelsius` REAL,
                        `minTemperatureCelsius` REAL,
                        `rainChancePercent` INTEGER,
                        `fetchedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`placeId`)
                    )""".trimIndent()
                )
            }
        }

        // Adds travel photo metadata and QR scan history, preserving trips/weather.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """CREATE TABLE IF NOT EXISTS `journal_photos` (
                        `id` TEXT NOT NULL,
                        `ownerId` TEXT NOT NULL,
                        `destinationId` TEXT NOT NULL,
                        `localPath` TEXT NOT NULL,
                        `caption` TEXT NOT NULL,
                        `capturedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )""".trimIndent()
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_journal_photos_ownerId` ON `journal_photos` (`ownerId`)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_journal_photos_destinationId` ON `journal_photos` (`destinationId`)"
                )
                connection.execSQL(
                    """CREATE TABLE IF NOT EXISTS `scan_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ownerId` TEXT NOT NULL,
                        `destinationId` TEXT NOT NULL,
                        `scannedAtMillis` INTEGER NOT NULL
                    )""".trimIndent()
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_scan_history_ownerId` ON `scan_history` (`ownerId`)"
                )
            }
        }


        // Add separate offline smart itinerary tables without changing synced My Trips.
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """CREATE TABLE IF NOT EXISTS `smart_plans` (
                        `id` TEXT NOT NULL,
                        `ownerId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `startDate` TEXT NOT NULL,
                        `numberOfDays` INTEGER NOT NULL,
                        `startingCity` TEXT NOT NULL,
                        `interestsCsv` TEXT NOT NULL,
                        `budgetLkr` INTEGER NOT NULL,
                        `estimatedTotalLkr` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )""".trimIndent()
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_smart_plans_ownerId` ON `smart_plans` (`ownerId`)"
                )
                connection.execSQL(
                    """CREATE TABLE IF NOT EXISTS `smart_plan_stops` (
                        `planId` TEXT NOT NULL,
                        `destinationId` TEXT NOT NULL,
                        `dayIndex` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        `estimatedAdmissionLkr` INTEGER NOT NULL,
                        PRIMARY KEY(`planId`, `destinationId`),
                        FOREIGN KEY(`planId`) REFERENCES `smart_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent()
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_smart_plan_stops_planId` ON `smart_plan_stops` (`planId`)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_smart_plan_stops_planId_dayIndex` ON `smart_plan_stops` (`planId`, `dayIndex`)"
                )
            }
        }

        // Preserve all version-5 smart plans. Existing rows receive null coordinates
        // and continue resolving their original 62-city names as before.
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE `smart_plans` ADD COLUMN `startingLatitude` REAL")
                connection.execSQL("ALTER TABLE `smart_plans` ADD COLUMN `startingLongitude` REAL")
            }
        }

        @Volatile private var INSTANCE: ExploreLankaDatabase? = null

        fun getInstance(context: Context): ExploreLankaDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ExploreLankaDatabase::class.java,
                    "explore_lanka.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
