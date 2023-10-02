package com.swapps.cardiopulm.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "healthReportDB")
data class HealthReport(
    @ColumnInfo(name = "timestamp") val timestamp: String?,
    @ColumnInfo(name = "heartRate") val heartRate: String?,
    @ColumnInfo(name = "respiratoryRate") val respiratoryRate: String?,
    @ColumnInfo(name = "nausea") val nausea: String?,
    @ColumnInfo(name = "headache") val headache: String?,
    @ColumnInfo(name = "diarrhoea") val diarrhoea: String?,
    @ColumnInfo(name = "soarThroat") val soarThroat: String?,
    @ColumnInfo(name = "fever") val fever: String?,
    @ColumnInfo(name = "muscleAche") val muscleAche: String?,
    @ColumnInfo(name = "lossOfSmellOrTaste") val lossOfSmellOrTaste: String?,
    @ColumnInfo(name = "cough") val cough: String?,
    @ColumnInfo(name = "shortnessOfBreath") val shortnessOfBreath: String?,
    @ColumnInfo(name = "feelingTired") val feelingTired: String?,
    @PrimaryKey(autoGenerate = true) var id: Int = 0
)