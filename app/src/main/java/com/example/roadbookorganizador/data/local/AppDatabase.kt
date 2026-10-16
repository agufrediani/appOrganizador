package com.example.roadbookorganizador.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.roadbookorganizador.data.local.dao.*
import com.example.roadbookorganizador.data.local.entity.*

@Database(
    entities = [
        RallyEntity::class,
        TramoEntity::class,
        VinetaEntity::class,
        CalibracionEntity::class,
        TrackPointEntity::class,
        PuntoInteresEntity::class
    ],
    version = 8,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rallyDao(): RallyDao
    abstract fun tramoDao(): TramoDao
    abstract fun vinetaDao(): VinetaDao
    abstract fun calibracionDao(): CalibracionDao
    abstract fun trackPointDao(): TrackPointDao
    abstract fun puntoInteresDao(): PuntoInteresDao

    companion object {
        /** 7 → 8: columna `dibujo` (viñeta en formato versión 1). Las columnas viejas no se tocan. */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vinetas ADD COLUMN dibujo TEXT")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "roadbook_organizador.db"
                )
                // Nunca borrar los datos al actualizar la app: cada cambio de versión de la base
                // necesita su Migration (ver app/schemas). Solo las versiones de desarrollo
                // anteriores a la 7 se descartan, porque no tienen esquema exportado.
                .addMigrations(MIGRATION_7_8)
                .fallbackToDestructiveMigrationFrom(true, 1, 2, 3, 4, 5, 6)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
