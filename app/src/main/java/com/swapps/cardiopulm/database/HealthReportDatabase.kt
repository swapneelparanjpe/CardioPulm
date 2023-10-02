package com.swapps.cardiopulm.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [HealthReport::class], version = 1)
abstract class HealthReportDatabase: RoomDatabase() {

    abstract fun healthReportDao(): HealthReportDao

    companion object {

        @Volatile
        private var INSTANCE: HealthReportDatabase? = null

        fun getHealthReportDatabase(context: Context): HealthReportDatabase {
            val tempInstance = INSTANCE
            if(tempInstance != null)
                return tempInstance
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HealthReportDatabase::class.java,
                    "healthReportDB"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }

}