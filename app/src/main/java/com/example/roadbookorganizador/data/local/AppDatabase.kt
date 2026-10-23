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
        PuntoInteresEntity::class,
        BajaPendienteEntity::class
    ],
    version = 14,
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

        /**
         * 8 → 9: datos del taqueo (ESPECIFICACION_TAQUEO.md). Cada viñeta que ya existía recibe
         * un uid nuevo (32 hex); las coordenadas que ya tenían quedan sin origen ni verificar.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vinetas ADD COLUMN uid TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE vinetas SET uid = lower(hex(randomblob(16))) WHERE uid = ''")
                db.execSQL("ALTER TABLE vinetas ADD COLUMN origenCoordenada TEXT")
                db.execSQL("ALTER TABLE vinetas ADD COLUMN coordenadaFechaUtc TEXT")
                db.execSQL("ALTER TABLE vinetas ADD COLUMN coordenadaPor TEXT")
                db.execSQL("ALTER TABLE vinetas ADD COLUMN precisionGpsM REAL")
                db.execSQL("ALTER TABLE vinetas ADD COLUMN verificadaTerreno INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE vinetas ADD COLUMN latitudAnterior REAL")
                db.execSQL("ALTER TABLE vinetas ADD COLUMN longitudAnterior REAL")
            }
        }

        /**
         * 9 → 10: sincronización con la plataforma. id de la viñeta en el servidor, la viñeta
         * como vino del servidor, la marca de "falta subir" y las viñetas borradas a borrar allá.
         * Las viñetas que ya estaban quedan como estaban (no se suben solas, para no duplicar).
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vinetas ADD COLUMN webId INTEGER")
                db.execSQL("ALTER TABLE vinetas ADD COLUMN datosServidor TEXT")
                db.execSQL("ALTER TABLE vinetas ADD COLUMN pendienteSubir INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `bajas_pendientes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`tramoId` INTEGER NOT NULL, `webId` INTEGER NOT NULL, `uid` TEXT NOT NULL, `creadoMs` INTEGER NOT NULL)"
                )
            }
        }

        /** 10 → 11: uid de cada tramo (ESPECIFICACION_TAQUEO 0 bis) y marca de "falta subir". */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tramos ADD COLUMN uid TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE tramos SET uid = lower(hex(randomblob(16))) WHERE uid = ''")
                db.execSQL("ALTER TABLE tramos ADD COLUMN pendienteSubir INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tramos ADD COLUMN datosServidor TEXT")
            }
        }

        /** 11 → 12: zonas y puntos de interés de la plataforma (id del servidor, forma, coordenadas). */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE puntos_interes ADD COLUMN webId INTEGER")
                db.execSQL("ALTER TABLE puntos_interes ADD COLUMN forma TEXT NOT NULL DEFAULT 'PUNTO'")
                db.execSQL("ALTER TABLE puntos_interes ADD COLUMN coordenadasGeoJson TEXT")
                db.execSQL("ALTER TABLE puntos_interes ADD COLUMN servicios TEXT")
                db.execSQL("ALTER TABLE puntos_interes ADD COLUMN tramoWebId INTEGER")
                db.execSQL("ALTER TABLE puntos_interes ADD COLUMN tramoIdentificador TEXT")
            }
        }

        /** 12 → 13: el camino desde la viñeta anterior (para dibujar el trazado sin señal). */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vinetas ADD COLUMN geometriaDesdeAnterior TEXT")
            }
        }

        /** 13 → 14: marca de camino por subir a la plataforma (revisión 23). */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vinetas ADD COLUMN caminoPendiente INTEGER NOT NULL DEFAULT 0")
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
                .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
                .fallbackToDestructiveMigrationFrom(true, 1, 2, 3, 4, 5, 6)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
