package com.example.watermypot

import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.fusionauth.jwt.domain.JWT
import io.fusionauth.jwt.hmac.HMACSigner
import kotlinx.coroutines.*
import okhttp3.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList


// The longer it takes the less moisture
const val maxLimitMoisture = 320F;
const val minLimitMoisture = 185F;


class MainActivity : AppCompatActivity() {

    lateinit var wateringBtn: FloatingActionButton
    lateinit var smallWaterBtn: FloatingActionButton
    lateinit var mediumWaterBtn: FloatingActionButton
    lateinit var largeWaterBtn: FloatingActionButton


    lateinit var moistureText: TextView
    lateinit var moistureSeriesChart: LineChart
    lateinit var signer: HMACSigner
    lateinit var moistureValue: String
    lateinit var chartEntries:  ArrayList<Entry>
    var moisturePercentage: Float = 0F
    private lateinit var percentageBar: PercentageBar
    private var url: String = "http://178.119.117.90:25566"

    // Animation
    private val rotateOpen: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_open_anim) }
    private val rotateClose: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_close_anim) }
    private val fromRight: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.from_right_anim) }
    private val toRight: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.to_right_anim) }

    private var clicked = false

    //    var url: String = "http://192.168.0.246:5000"
    data class SensorEntry(
            val id: Int,
            val value: Float,
            val date: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Layout elements
        wateringBtn = findViewById(R.id.wateringActionButton)
        smallWaterBtn = findViewById(R.id.smallWaterActionButton)
        mediumWaterBtn = findViewById(R.id.mediumWaterActionButton)
        largeWaterBtn = findViewById(R.id.largeWaterActionButton)

        moistureText = findViewById(R.id.moistureText)
        percentageBar = findViewById(R.id.moistureVisualization)
        moistureSeriesChart = findViewById(R.id.chart)

        chartEntries = ArrayList()


        wateringBtn.setOnClickListener{
            onWateringButtonClicked()
        }
        smallWaterBtn.setOnClickListener{
            watering("2")
        }
        mediumWaterBtn.setOnClickListener{
            watering("3")
        }
        largeWaterBtn.setOnClickListener{
            watering("5")
        }


        // JWT
        // Build an HMAC signer using a SHA-256 hash
        signer = HMACSigner.newSHA256Signer("superdupersecret")
        moistureValue = "No moisture level"
        moistureText.text = moistureValue
        percentageBar.invalidate()
        updateMoistureLevelView()
        updateMoistureSeries()



    }

    private fun watering(duration: String){
        changeStatusWateringBtns(false)
        try {
            // Show toast
            Toast.makeText(applicationContext, "Watering...", Toast.LENGTH_SHORT).show()
            sendPumpCommand(duration)

        } catch (e: NumberFormatException) {
            Toast.makeText(applicationContext, "Only integers allowed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun changeStatusWateringBtns(status: Boolean) {
        smallWaterBtn.isEnabled = status
        smallWaterBtn.isClickable = status

        mediumWaterBtn.isEnabled = status
        mediumWaterBtn.isClickable = status

        largeWaterBtn.isEnabled = status
        largeWaterBtn.isClickable = status
    }

    private fun onWateringButtonClicked(){
        setVisibility(clicked)
        setAnimation(clicked)
        clicked = !clicked
    }

    private fun setVisibility(clicked: Boolean) {
        if(!clicked){
            smallWaterBtn.visibility = View.VISIBLE
            mediumWaterBtn.visibility = View.VISIBLE
            largeWaterBtn.visibility = View.VISIBLE

        }
        else{
            smallWaterBtn.visibility = View.GONE
            mediumWaterBtn.visibility = View.GONE
            largeWaterBtn.visibility = View.GONE

        }
    }
    private fun setAnimation(clicked: Boolean) {
        if(!clicked){
            wateringBtn.startAnimation(rotateOpen)
//            smallWaterBtn.startAnimation(fromRight)
//            mediumWaterBtn.startAnimation(fromRight)
//            largeWaterBtn.startAnimation(fromRight)
        }
        else{
            wateringBtn.startAnimation(rotateClose)
//            smallWaterBtn.startAnimation(toRight)
//            mediumWaterBtn.startAnimation(toRight)
//            largeWaterBtn.startAnimation(toRight)
        }
    }



    private fun sendPumpCommand(waterTimeValue: String){
        val jwt: JWT = JWT().setIssuer("sander")
                .setIssuedAt(ZonedDateTime.now(ZoneOffset.UTC))
                .setSubject(waterTimeValue)
                .setExpiration(ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(60))

        // Sign and encode the JWT to a JSON string representation

        // Sign and encode the JWT to a JSON string representation
        val encodedJWT: String = JWT.getEncoder().encode(jwt, signer)

        println("token: $encodedJWT")

        val pumpURL = "$url/pump"
        val token = "Bearer $encodedJWT"

        val client = OkHttpClient()

        val request = Request.Builder()
                .url(pumpURL)
                .header("Authorization", token)
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Error: ${e.message}")
                finishWatering("Something went wrong!")
            }

            override fun onResponse(call: Call, response: Response) {
                println("Response code: ${response.code}")
                println("Response body: ${response.body?.string()}")

                // Will only be shown after the 'watering...' toast
                finishWatering("Watering done!")
            }
        })
    }

    private fun finishWatering(message: String){
        runOnUiThread {
            changeStatusWateringBtns(true)

            // Create success toast
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()


        }
    }

    private fun getRequest(url: String): Request {
        // Simulate a delay to represent fetching data from a database or network
        val jwt: JWT = JWT().setIssuer("sander")
                .setIssuedAt(ZonedDateTime.now(ZoneOffset.UTC))
                .setSubject("moisture")
                .setExpiration(ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(60))

        // Sign and encode the JWT to a JSON string representation
        val encodedJWT: String = JWT.getEncoder().encode(jwt, signer)
        val token = "Bearer $encodedJWT"

        return Request.Builder()
                .url(url)
                .header("Authorization", token)
                .build()
    }

    private fun getMoistureLevel() {
        val request = getRequest("$url/moist")
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                println("Response code: ${response.code}")
                parseMoistureSignal(response.body?.string().toString())
            }
        })
    }

    private fun getMoistureSeries() {
        val request = getRequest("$url/moistseries")
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                println("Response code: ${response.code}")

                val seriesString = response.body?.string().toString()
                println(seriesString)
                val sType = object : TypeToken<List<SensorEntry>>() {}.type
                val seriesList = Gson().fromJson<List<SensorEntry>>(seriesString, sType)
                var dates: MutableList<String> = mutableListOf<String>()
                val simpleDateformatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                for ((i, seriesEntry) in seriesList.withIndex()) {
                    val percentage = calculatePercentage(seriesEntry.value)
                    chartEntries.add(Entry(i.toFloat(), percentage))

                    val parsedDate = simpleDateformatter.parse(seriesEntry.date)
                    // dd/MM HH:mm
                    dates.add(SimpleDateFormat("dd/MM HH:mm").format(parsedDate))
//                    dates.add(seriesEntry.date)
                }


                val formatter: IAxisValueFormatter = object : IAxisValueFormatter {
                    override fun getFormattedValue(value: Float, axis: AxisBase): String {
                        return dates.get(value.toInt())
                    }

                    // we don't draw numbers, so no decimal digits needed
                    val decimalDigits: Int
                        get() = 0
                }
                class MyXAxisFormatter(dates: MutableList<String>) : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                        return dates.getOrNull(value.toInt()) ?: value.toString()
                    }
                }

                val moistureLineData = LineDataSet(chartEntries, "Moisture")
                moistureLineData.lineWidth = 3f
                val dataSet = ArrayList<ILineDataSet>()
                dataSet.add(moistureLineData)
                val lineData = LineData(dataSet)

                moistureSeriesChart.data = lineData
                moistureSeriesChart.description.isEnabled = false
                val xAxis: XAxis = moistureSeriesChart.getXAxis()
                xAxis.granularity = 4f // minimum axis-step (interval) is 1
                xAxis.textSize = 7f;
                xAxis.valueFormatter = MyXAxisFormatter(dates)

                moistureSeriesChart.invalidate()
            }
        })
    }

    private fun calculatePercentage(moistSignal: Float): Float{
        return   (maxLimitMoisture - moistSignal) / (maxLimitMoisture - minLimitMoisture)
    }

    private fun parseMoistureSignal(signal: String){
        println(signal)
        if(! signal.contains("No moisture level")){
            moisturePercentage = calculatePercentage(signal.toFloat())
            println("%.3f".format(moisturePercentage))

            // Avoid showing wrongly read analog signal but still allow
            if(moisturePercentage <= 2 && moisturePercentage > -0.1){
                moistureValue = "%.1f".format(moisturePercentage * 100) + "%"
            }
        }
        else {
            moistureValue = signal;
        }

    }

    //   Update the text view using coroutines
    private fun updateMoistureLevelView() {
        GlobalScope.launch {
            while(true) {
                // Call the async function and wait for the result
                async { getMoistureLevel() }.await()
                // Update the text view on the main thread
                withContext(Dispatchers.Main) {
                    moistureText.text = moistureValue
                    if(! moistureValue.contains("No moisture level")){
                        println("it contains: ${moistureValue}")
                        if(moisturePercentage <= 1 && moisturePercentage > 0){
                            percentageBar.setMoistureValue(moisturePercentage)
                            percentageBar.invalidate()
                        }
                    }

                }
                delay(5000)
            }
        }
    }

    //   Update the text view using coroutines
    private fun updateMoistureSeries() {
        GlobalScope.launch {
                // Call the async function and wait for the result
                async { getMoistureSeries() }.await()
        }
    }
}