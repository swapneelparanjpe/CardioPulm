package com.swapps.cardiopulm.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HealthReportDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertHealthReport(healthReport: HealthReport): Long

    @Query("SELECT * FROM healthReportDB ORDER BY id DESC LIMIT 1")
    fun getRecentHealthReport(): HealthReport

    @Query("UPDATE healthReportDB SET heartRate = :heartRate WHERE timestamp = :timestamp")
    fun updateHealthReport(heartRate: String, timestamp: String)

}