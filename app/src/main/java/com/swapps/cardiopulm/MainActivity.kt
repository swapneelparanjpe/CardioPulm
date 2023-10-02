package com.swapps.cardiopulm

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.swapps.cardiopulm.utils.COUNTDOWN_HEART_RATE_MEASUREMENT
import com.swapps.cardiopulm.utils.COUNTDOWN_RESPIRATORY_RATE_MEASUREMENT
import com.swapps.cardiopulm.utils.HEART_RATE
import com.swapps.cardiopulm.utils.INITIATE_HEART_RATE_MEASUREMENT
import com.swapps.cardiopulm.utils.INITIATE_RESPIRATORY_RATE_MEASUREMENT
import com.swapps.cardiopulm.utils.RESPIRATORY_RATE
import com.swapps.cardiopulm.utils.TIMESTAMP
import com.swapps.cardiopulm.utils.getHeartRate
import com.swapps.cardiopulm.utils.isPermissionGranted
import com.swapps.cardiopulm.utils.requestPermission
import java.sql.Timestamp
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.*


class MainActivity : AppCompatActivity(), SensorEventListener {

    companion object {
        private const val CAMERA_PERMISSION = android.Manifest.permission.CAMERA
        private const val CAMERA_PERMISSION_CODE = 255
        private const val TAG = "CameraXApp"
    }

    private lateinit var pvCamera: PreviewView
    private lateinit var btnMeasureHeartRate: Button
    private lateinit var btnMeasureRespiratoryRate: Button
    private lateinit var menuHistory: MenuItem

    private lateinit var camera: Camera
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var sensorManager: SensorManager
    private lateinit var sensor: Sensor

    private var isRespiratoryRateMeasurementEnabled: Boolean = false

    private var respiratoryData = ArrayList<ArrayList<Float>>()

    private var heartRate: String = ""
    private var respiratoryRate: Int = -1

    private lateinit var videoUri: Uri

    private lateinit var timestamp : String

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pvCamera = findViewById(R.id.pvCamera)
        btnMeasureHeartRate = findViewById(R.id.btnMeasureHeartRate)
        btnMeasureRespiratoryRate = findViewById(R.id.btnMeasureRespiratoryRate)

        // Request camera permissions
        askCameraPermission()
        cameraExecutor = Executors.newSingleThreadExecutor()

        btnMeasureHeartRate.setOnClickListener {
            btnMeasureHeartRate.isEnabled = false
            Toast.makeText(this@MainActivity, "Please place your index finger on back camera", Toast.LENGTH_LONG).show()
            startCountDownTimer(5, INITIATE_HEART_RATE_MEASUREMENT)
        }

        timestamp = Timestamp(System.currentTimeMillis()).toString()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)

        btnMeasureRespiratoryRate.setOnClickListener {
            btnMeasureRespiratoryRate.isEnabled = false
            Toast.makeText(this@MainActivity, "Please lie on your back and rest the phone on your chest facing upwards", Toast.LENGTH_LONG).show()
            startCountDownTimer(10, INITIATE_RESPIRATORY_RATE_MEASUREMENT)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menuHistory = menu!!.findItem(R.id.mi_history)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mi_history -> {
                val historyActivity = Intent(this, HistoryActivity::class.java)
                startActivity(historyActivity)
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "In order to measure heart rate, you need to allow CardioPulm to access camera", Toast.LENGTH_SHORT).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun askCameraPermission() {
        if (isPermissionGranted(this, CAMERA_PERMISSION)) {
            startCamera()
        } else {
            requestPermission(this, CAMERA_PERMISSION, CAMERA_PERMISSION_CODE)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(pvCamera.surfaceProvider)
            val recorder = Recorder.Builder().setQualitySelector(
                QualitySelector.from(Quality.HIGHEST, FallbackStrategy.higherQualityOrLowerThan(Quality.SD))
            ).build()
            videoCapture = VideoCapture.withOutput(recorder)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun startVideoRecording() {

        val videoCapture = this.videoCapture ?: return
        btnMeasureHeartRate.isEnabled = false

        val currentRecording = recording
        if(currentRecording != null) {
            currentRecording.stop()
            recording = null
            camera.cameraControl.enableTorch(false)
            btnMeasureRespiratoryRate.isEnabled = true
            return
        }

        val contentValues = ContentValues()
        val currentTimeMillis = System.currentTimeMillis()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, currentTimeMillis)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/CardioPulm")

        val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                if(recordEvent is VideoRecordEvent.Start) {
                    btnMeasureHeartRate.isEnabled = false
                    camera.cameraControl.enableTorch(true)
                    startCountDownTimer(45, COUNTDOWN_HEART_RATE_MEASUREMENT)
                }
                else if(recordEvent is VideoRecordEvent.Finalize) {
                        if (!recordEvent.hasError()) {
                            val runnable = Runnable {
                                Thread.sleep(2000)
                                videoUri = recordEvent.outputResults.outputUri
                                getHeartRate(this, videoUri, timestamp)
                            }
                            val thread = Thread(runnable)
                            thread.start()

                        } else {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture ends with error: " + "${recordEvent.error}")
                        }
                    btnMeasureHeartRate.visibility = View.GONE
                    }
                }
    }

    private fun startCountDownTimer(time: Int, timerCode: String) {
        var numSeconds = time
        val countDownTimer = object: CountDownTimer((numSeconds *1000).toLong(), 1000) {
            override fun onTick(p0: Long) {
                when (timerCode) {
                    INITIATE_HEART_RATE_MEASUREMENT -> btnMeasureHeartRate.text = "Starting recording in $numSeconds..."
                    COUNTDOWN_HEART_RATE_MEASUREMENT -> btnMeasureHeartRate.text = "$numSeconds seconds left..."
                    INITIATE_RESPIRATORY_RATE_MEASUREMENT -> btnMeasureRespiratoryRate.text = "Starting recording in $numSeconds..."
                    COUNTDOWN_RESPIRATORY_RATE_MEASUREMENT -> btnMeasureRespiratoryRate.text = "$numSeconds seconds left..."
                }
                numSeconds -= 1
            }

            @RequiresApi(Build.VERSION_CODES.P)
            override fun onFinish() {
                when (timerCode) {
                    INITIATE_HEART_RATE_MEASUREMENT -> startVideoRecording()
                    COUNTDOWN_HEART_RATE_MEASUREMENT -> startVideoRecording()
                    INITIATE_RESPIRATORY_RATE_MEASUREMENT -> {
                        isRespiratoryRateMeasurementEnabled = true
                        startCountDownTimer(45, COUNTDOWN_RESPIRATORY_RATE_MEASUREMENT)
                    }
                    COUNTDOWN_RESPIRATORY_RATE_MEASUREMENT -> {
                        isRespiratoryRateMeasurementEnabled = false
                        respiratoryRate = getRespiratoryRateFromData()
                        btnMeasureRespiratoryRate.visibility = View.GONE

                        val uploadSymptomsActivityIntent = Intent(this@MainActivity, UploadSymptomsActivity::class.java)
                        uploadSymptomsActivityIntent.putExtra(HEART_RATE, heartRate)
                        uploadSymptomsActivityIntent.putExtra(RESPIRATORY_RATE, respiratoryRate)
                        uploadSymptomsActivityIntent.putExtra(TIMESTAMP, timestamp)
                        startActivity(uploadSymptomsActivityIntent)
                    }
                }
            }
        }
        countDownTimer.start()
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        if (isRespiratoryRateMeasurementEnabled && sensorEvent != null) {
            respiratoryData.add(
                arrayListOf(sensorEvent.values[0], sensorEvent.values[1],sensorEvent.values[2]))
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
//        TODO("Not yet implemented")
    }

    private fun getRespiratoryRateFromData(): Int {
        var previousValue : Float = 10f
        var currentValue : Float
        var k = 0
        for (data in respiratoryData) {
            currentValue = sqrt(
                Math.pow(data[0].toDouble(), 2.0) + Math.pow(data[1].toDouble(), 2.0) + Math.pow(data[2].toDouble(), 2.0)
            ).toFloat()
            if (abs(x = previousValue - currentValue) > 0.038) {
                k++
            }
            previousValue = currentValue
        }

        val ret = (k/45.00)

        return (ret*30).toInt()

    }

}