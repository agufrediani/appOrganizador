package com.example.roadbookorganizador.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rallyDao(): RallyDao
    abstract fun tramoDao(): TramoDao
    abstract fun vinetaDao(): VinetaDao
    abstract fun calibracionDao(): CalibracionDao
    abstract fun trackPointDao(): TrackPointDao
    abstract fun puntoInteresDao(): PuntoInteresDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "roadbook_organizador.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
