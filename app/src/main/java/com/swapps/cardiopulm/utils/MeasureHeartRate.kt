package com.swapps.cardiopulm.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.swapps.cardiopulm.database.HealthReportDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.P)
fun getHeartRate(context: Context, uri: Uri, timestamp: String) {
    var m_bitmap: Bitmap? = null
    val retriever = MediaMetadataRetriever()
    val frameList = ArrayList<Bitmap>()
    try {
        retriever.setDataSource(context, uri)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)
        val aduration = duration!!.toInt()
        var i = 10
        while (i < aduration) {
            val bitmap = retriever.getFrameAtIndex(i)
            if (bitmap != null) {
                frameList.add(bitmap)
            }
            i += 5
        }
    } catch (e: Exception) {
        Log.e("MeaureHeartRate", "Exception --> $e")
    } finally {
        retriever?.release()
        var redBucket: Long = 0
        var pixelCount: Long = 0
        val a = mutableListOf<Long>()
        for (i in frameList) {
            redBucket = 0
            for (y in 550 until 650) {
                for (x in 550 until 650) {
                    val c: Int = i.getPixel(x, y)
                    pixelCount++
                    redBucket += Color.red(c) + Color.blue(c) + Color.green(c)
                }
            }
            a.add(redBucket)
        }
        val b = mutableListOf<Long>()
        for (i in 0 until a.lastIndex - 5) {
            var temp =
                (a.elementAt(i) + a.elementAt(i + 1) + a.elementAt(i + 2) + a.elementAt(
                    i + 3
                ) + a.elementAt(
                    i + 4
                )) / 4
            b.add(temp)
        }
        var x = b.elementAt(0)
        var count = 0
        for (i in 1 until b.lastIndex) {
            var p=b.elementAt(i.toInt())
            if ((p-x) > 3500) {
                count += 1
            }
            x = b.elementAt(i.toInt())
        }
        val rate = ((((count.toFloat() / 45) * 60).toInt())/2).toString()

        val healthReportDatabase = HealthReportDatabase.getHealthReportDatabase(context)
        GlobalScope.launch(Dispatchers.IO) {
            healthReportDatabase.healthReportDao().updateHealthReport(rate, timestamp)
        }

    }

}