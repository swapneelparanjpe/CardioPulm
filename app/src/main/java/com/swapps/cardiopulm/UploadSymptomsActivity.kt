package com.swapps.cardiopulm

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.RatingBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.swapps.cardiopulm.database.HealthReport
import com.swapps.cardiopulm.database.HealthReportDatabase
import com.swapps.cardiopulm.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class UploadSymptomsActivity : AppCompatActivity() {

    private lateinit var spnrSymptomDropdown: Spinner
    private lateinit var rbSeverity: RatingBar
    private lateinit var btnSave: Button

    private var symptoms: MutableList<Float> = MutableList(10) {0.0f}

    private lateinit var healthReportDatabase: HealthReportDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_symptoms)

        spnrSymptomDropdown = findViewById(R.id.spnrSymptomDropdown)
        rbSeverity = findViewById(R.id.rbSeverity)
        btnSave = findViewById(R.id.btnSave)

        healthReportDatabase = HealthReportDatabase.getHealthReportDatabase(this)

        // Get Heart Rate and Respiratory Rate
        val heartRate = intent.getStringExtra(HEART_RATE)
        val respiratoryRate = intent.getStringExtra(RESPIRATORY_RATE)
        val timestamp = intent.getStringExtra(TIMESTAMP)


        // Populate Spinner with values
        ArrayAdapter.createFromResource(
            this,
            R.array.symptoms_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spnrSymptomDropdown.adapter = adapter
        }

        // Display the last recorded rating of selected spinner item
        spnrSymptomDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                rbSeverity.rating = symptoms[position]
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
//                Not required
            }

        }

        // Update the rating for the selected spinner item
        rbSeverity.setOnRatingBarChangeListener{_, rating, _ ->
            symptoms[spnrSymptomDropdown.selectedItemPosition] = rating
        }

//        Save health report to database
        btnSave.setOnClickListener {
            val healthReport = HealthReport(
                timestamp,
                heartRate,
                respiratoryRate,
                nausea = symptoms[0].toString(),
                headache = symptoms[1].toString(),
                diarrhoea = symptoms[2].toString(),
                soarThroat = symptoms[3].toString(),
                fever = symptoms[4].toString(),
                muscleAche = symptoms[5].toString(),
                lossOfSmellOrTaste = symptoms[6].toString(),
                cough = symptoms[7].toString(),
                shortnessOfBreath = symptoms[8].toString(),
                feelingTired = symptoms[9].toString()
            )

            GlobalScope.launch(Dispatchers.IO) {
                healthReportDatabase.healthReportDao().insertHealthReport(healthReport)
            }

            Toast.makeText(this, "Your Health Report has been saved", Toast.LENGTH_LONG).show()

            val mainActivity = Intent(this, MainActivity::class.java)
            startActivity(mainActivity)
        }

    }
}