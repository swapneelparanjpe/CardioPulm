package com.swapps.cardiopulm

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.swapps.cardiopulm.database.HealthReportDatabase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var tvHistoryTimestamp: TextView
    private lateinit var tvHistoryHeartRate: TextView
    private lateinit var tvHistoryRespiratoryRate: TextView
    private lateinit var tvHistoryNausea: TextView
    private lateinit var tvHistoryHeadache: TextView
    private lateinit var tvHistoryDiarrhoea: TextView
    private lateinit var tvHistorySoarThroat: TextView
    private lateinit var tvHistoryFever: TextView
    private lateinit var tvHistoryMuscleAche: TextView
    private lateinit var tvHistoryLossOfSmellOrTaste: TextView
    private lateinit var tvHistoryCough: TextView
    private lateinit var tvHistoryShortnessOfBreath: TextView
    private lateinit var tvHistoryFeelingTired: TextView

    private lateinit var healthReportDatabase: HealthReportDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        tvHistoryTimestamp = findViewById(R.id.tvHistoryTimestamp)
        tvHistoryHeartRate = findViewById(R.id.tvHistoryHeartRate)
        tvHistoryRespiratoryRate = findViewById(R.id.tvHistoryRespiratoryRate)
        tvHistoryNausea = findViewById(R.id.tvHistoryNausea)
        tvHistoryHeadache = findViewById(R.id.tvHistoryHeadache)
        tvHistoryDiarrhoea = findViewById(R.id.tvHistoryDiarrhoea)
        tvHistorySoarThroat = findViewById(R.id.tvHistorySoarThroat)
        tvHistoryFever = findViewById(R.id.tvHistoryFever)
        tvHistoryMuscleAche = findViewById(R.id.tvHistoryMuscleAche)
        tvHistoryLossOfSmellOrTaste = findViewById(R.id.tvHistoryLossOfSmellOrTaste)
        tvHistoryCough = findViewById(R.id.tvHistoryCough)
        tvHistoryShortnessOfBreath = findViewById(R.id.tvHistoryShortnessOfBreath)
        tvHistoryFeelingTired = findViewById(R.id.tvHistoryFeelingTired)

        healthReportDatabase = HealthReportDatabase.getHealthReportDatabase(this)

        populateHistory()

    }

    private fun populateHistory() {
        GlobalScope.launch {
            val healthReport = healthReportDatabase.healthReportDao().getRecentHealthReport()
            tvHistoryTimestamp.text = "Timestamp: ${healthReport.timestamp}"
            tvHistoryHeartRate.text = "Heart Rate: ${healthReport.heartRate}"
            tvHistoryRespiratoryRate.text = "Respiratory Rate: ${healthReport.respiratoryRate}"
            tvHistoryNausea.text = "Nausea: ${healthReport.nausea} / 5.0"
            tvHistoryHeadache.text = "Headache: ${healthReport.headache} / 5.0"
            tvHistoryDiarrhoea.text = "Diarrhoea: ${healthReport.diarrhoea} / 5.0"
            tvHistorySoarThroat.text = "Soar Throat: ${healthReport.soarThroat} / 5.0"
            tvHistoryFever.text = "Fever: ${healthReport.fever} / 5.0"
            tvHistoryMuscleAche.text = "Muscle Ache: ${healthReport.muscleAche} / 5.0"
            tvHistoryLossOfSmellOrTaste.text = "Loss of Smell or Taste: ${healthReport.lossOfSmellOrTaste} / 5.0"
            tvHistoryCough.text = "Cough: ${healthReport.cough} / 5.0"
            tvHistoryShortnessOfBreath.text = "Shortness of Breath: ${healthReport.shortnessOfBreath} / 5.0"
            tvHistoryFeelingTired.text = "Feeling Tired: ${healthReport.feelingTired} / 5.0"

        }
    }

}