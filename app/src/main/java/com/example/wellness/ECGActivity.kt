package com.example.wellness

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidplot.xy.BoundaryMode
import com.androidplot.xy.StepMode
import com.androidplot.xy.XYPlot
import com.google.gson.GsonBuilder
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl.defaultImplementation
import com.polar.sdk.api.errors.PolarInvalidArgument
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarEcgData
import com.polar.sdk.api.model.PolarHrData
import com.polar.sdk.api.model.PolarSensorSetting
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class ECGActivity : AppCompatActivity(), PlotterListener {
    companion object {
        private const val TAG = "ECGActivity"
    }

    private lateinit var api: PolarBleApi
    private lateinit var textViewHR: TextView
    private lateinit var textViewRR: TextView
    private lateinit var textViewDeviceId: TextView
    private lateinit var textViewBattery: TextView
    private lateinit var textViewFwVersion: TextView
    private lateinit var homeView: ImageView
    private lateinit var plot: XYPlot
    private lateinit var ecgPlotter: EcgPlotter
    private var ecgDisposable: Disposable? = null
    private var hrDisposable: Disposable? = null
    private lateinit var btn_start: Button
    private var isRecording = false
    private val recordedData = mutableListOf<String>()
    private val recordedDataHR = mutableListOf<String>()
    private var recordingStartTime: Long = 0
    private val buffer = mutableListOf<String>()
    private val bufferSize = 1500  // Regola questa dimensione in base alla frequenza di campionamento



    private lateinit var deviceId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ecg_activity)
        deviceId = intent.getStringExtra("id") ?: throw Exception("ECGActivity couldn't be created, no deviceId given")
        textViewHR = findViewById(R.id.hr)
        textViewRR = findViewById(R.id.rr)
        textViewDeviceId = findViewById(R.id.deviceId)
        textViewBattery = findViewById(R.id.battery_level)
        textViewFwVersion = findViewById(R.id.fw_version)
        homeView = findViewById(R.id.home)
        plot = findViewById(R.id.plot)
        btn_start = findViewById(R.id.start_button)

        api = defaultImplementation(
                applicationContext,
                setOf(
                        PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                        PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                        PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO
                )
        )
        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                Log.d(TAG, "BluetoothStateChanged $powered")
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "Device connected " + polarDeviceInfo.deviceId)
                Toast.makeText(applicationContext, R.string.connected, Toast.LENGTH_SHORT).show()
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "Device connecting ${polarDeviceInfo.deviceId}")
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "Device disconnected ${polarDeviceInfo.deviceId}")
            }

            override fun bleSdkFeatureReady(identifier: String, feature: PolarBleApi.PolarBleSdkFeature) {
                Log.d(TAG, "feature ready $feature")

                when (feature) {
                    PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING -> {
                        streamECG()
                        streamHR()
                    }
                    else -> {}
                }
            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                if (uuid == UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb")) {
                    val msg = "Firmware: " + value.trim { it <= ' ' }
                    Log.d(TAG, "Firmware: " + identifier + " " + value.trim { it <= ' ' })
                    textViewFwVersion.append(msg.trimIndent())
                }
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.d(TAG, "Battery level $identifier $level%")
                val batteryLevelText = "Battery level: $level%"
                textViewBattery.append(batteryLevelText)
            }

            override fun hrNotificationReceived(identifier: String, data: PolarHrData.PolarHrSample) {
                // deprecated
            }

            override fun polarFtpFeatureReady(identifier: String) {
                // deprecated
            }

            override fun streamingFeaturesReady(identifier: String, features: Set<PolarBleApi.PolarDeviceDataType>) {
                // deprecated
            }

            override fun hrFeatureReady(identifier: String) {
                // deprecated
            }

        })
        try {
            api.connectToDevice(deviceId)
        } catch (a: PolarInvalidArgument) {
            a.printStackTrace()
        }
        val deviceIdText = "ID: $deviceId"
        textViewDeviceId.text = deviceIdText

        ecgPlotter = EcgPlotter("ECG", 130)
        ecgPlotter.setListener(this)

        homeView.setOnClickListener { onClickHome(it) }
        btn_start.setOnClickListener { onClickStart() }


        plot.addSeries(ecgPlotter.getSeries(), ecgPlotter.formatter)
        plot.setRangeBoundaries(-1.5, 1.5, BoundaryMode.FIXED)
        plot.setRangeStep(StepMode.INCREMENT_BY_FIT, 0.25)
        plot.setDomainStep(StepMode.INCREMENT_BY_VAL, 130.0)
        plot.setDomainBoundaries(0, 650, BoundaryMode.FIXED)
        plot.linesPerRangeLabel = 2
    }

    private fun onClickHome(view: View) {
        val intent = Intent(this@ECGActivity, MainActivity::class.java)
        startActivity(intent)
    }

    private fun onClickStart() {
        if(isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        // Ottieni la data corrente e formatta il prefisso del nome della cartella
        val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val sessionPrefix = date + "_Session"

        var sessionDir: File? = null
        var newSessionAllowed = true
        val currentTime = System.currentTimeMillis()
        val oneHourInMillis = (60 * 60 * 1000).toLong()

        for (i in 1..3) {
            sessionDir = File(documentsDir, sessionPrefix + i)
            if (!sessionDir.exists()) {
                if (isLastSessionOlderThanOneHour(documentsDir, sessionPrefix, currentTime, oneHourInMillis)) {
                    sessionDir.mkdirs()
                } else {
                    newSessionAllowed = false
                }
                break
            } else if (sessionDir.isDirectory && !containsEcgDat(sessionDir) && !containsHrDat(sessionDir)) {
                // La cartella esiste e non contiene immagini
                break
            }
        }

        if (sessionDir == null || sessionDir.exists() && sessionDir.list() != null && containsEcgDat(sessionDir) && containsHrDat(sessionDir) || !newSessionAllowed) {
            Log.d("session", sessionDir.toString())
            Toast.makeText(this, "Numero massimo di sessioni per oggi raggiunto, tutte le cartelle piene o meno di un'ora dall'ultima sessione.", Toast.LENGTH_SHORT).show()
            return
        }

        isRecording = true
        recordedData.clear()
        recordedDataHR.clear()
        buffer.clear()
        recordingStartTime = System.currentTimeMillis()
        btn_start.text = "Stop Recording"
        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
        startEcgStreamThread()
        // Stop recording after 10 minutes
        Handler(Looper.getMainLooper()).postDelayed({ stopRecording() }, 10 * 60 * 1000)
    }

    private fun stopRecording() {
        isRecording = false
        btn_start.text = "Start Recording"
        Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
        saveBufferToRecordedData()
        saveDataToJSON()
        saveEcgToDat()

        stopEcgStreamThread()
    }

    private fun saveBufferToRecordedData() {
        synchronized(buffer) {
            recordedData.addAll(buffer)
            buffer.clear()
        }
    }

    /*private fun saveHrToDat() {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        // Ottieni la data corrente e formatta il prefisso del nome della cartella
        val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val sessionPrefix = date + "_Session"

        var sessionDir: File? = null
        var newSessionAllowed = true
        val currentTime = System.currentTimeMillis()
        val oneHourInMillis = (60 * 60 * 1000).toLong()

        for (i in 1..3) {
            sessionDir = File(documentsDir, sessionPrefix + i)
            if (!sessionDir.exists()) {
                if (isLastSessionOlderThanOneHour(documentsDir, sessionPrefix, currentTime, oneHourInMillis)) {
                    sessionDir.mkdirs()
                } else {
                    newSessionAllowed = false
                }
                break
            } else if (sessionDir.isDirectory && !containsHrDat(sessionDir)) {
                // La cartella esiste e non contiene immagini
                break
            }
        }

        if (sessionDir == null || sessionDir.exists() && sessionDir.list() != null && containsHrDat(sessionDir) || !newSessionAllowed) {
            Log.d("session", sessionDir.toString())
            Toast.makeText(this, "Numero massimo di sessioni per oggi raggiunto, tutte le cartelle piene o meno di un'ora dall'ultima sessione.", Toast.LENGTH_SHORT).show()
            return
        }

        val datFile = File(sessionDir, "hr.dat")
        try {
            FileOutputStream(datFile).use { fos ->
                val dataOutputStream = DataOutputStream(fos)
                for (data in recordedDataHR) {
                    val bytes = data.toByteArray(Charsets.UTF_8)
                    dataOutputStream.writeInt(bytes.size) // Scrivi la dimensione dell'array di byte
                    dataOutputStream.write(bytes) // Scrivi l'array di byte
                }
                dataOutputStream.close()
            }
            Toast.makeText(this, "Data saved to hr.dat", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show()
        }
    } */

    private fun saveEcgToDat() {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        // Ottieni la data corrente e formatta il prefisso del nome della cartella
        val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val sessionPrefix = date + "_Session"

        var sessionDir: File? = null
        var newSessionAllowed = true
        val currentTime = System.currentTimeMillis()
        val oneHourInMillis = (60 * 60 * 1000).toLong()

        for (i in 1..3) {
            sessionDir = File(documentsDir, sessionPrefix + i)
            if (!sessionDir.exists()) {
                if (isLastSessionOlderThanOneHour(documentsDir, sessionPrefix, currentTime, oneHourInMillis)) {
                    sessionDir.mkdirs()
                } else {
                    newSessionAllowed = false
                }
                break
            } else if (sessionDir.isDirectory && !containsEcgDat(sessionDir) && !containsHrDat(sessionDir)) {
                // La cartella esiste e non contiene immagini
                break
            }
        }

        if (sessionDir == null || sessionDir.exists() && sessionDir.list() != null && containsEcgDat(sessionDir) && containsHrDat(sessionDir) || !newSessionAllowed) {
            Log.d("session", sessionDir.toString())
            Toast.makeText(this, "Numero massimo di sessioni per oggi raggiunto, tutte le cartelle piene o meno di un'ora dall'ultima sessione.", Toast.LENGTH_SHORT).show()
            return
        }

        val datFile = File(sessionDir, "ecg.dat")
        try {
            FileOutputStream(datFile).use { fos ->
                val dataOutputStream = DataOutputStream(fos)
                for (data in recordedData) {
                    val bytes = data.toByteArray(Charsets.UTF_8)
                    dataOutputStream.writeInt(bytes.size) // Scrivi la dimensione dell'array di byte
                    dataOutputStream.write(bytes) // Scrivi l'array di byte
                }
                dataOutputStream.close()
            }
            Toast.makeText(this, "Data saved to ecg.dat", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show()
        }
        val hrFile = File(sessionDir, "hr.dat")
        try {
            FileOutputStream(hrFile).use { fos ->
                val dataOutputStream = DataOutputStream(fos)
                for (data in recordedDataHR) {
                    val bytes = data.toByteArray(Charsets.UTF_8)
                    dataOutputStream.writeInt(bytes.size) // Scrivi la dimensione dell'array di byte
                    dataOutputStream.write(bytes) // Scrivi l'array di byte
                }
                dataOutputStream.close()
            }
            Toast.makeText(this, "Data saved to hr.dat", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isLastSessionOlderThanOneHour(documentsDir: File, sessionPrefix: String, currentTime: Long, oneHourInMillis: Long): Boolean {
        for (i in 3 downTo 1) {
            val sessionDir = File(documentsDir, sessionPrefix + i)
            if (sessionDir.exists()) {
                val lastModified = sessionDir.lastModified()
                if (currentTime - lastModified < oneHourInMillis) {
                    return false
                }
            }
        }
        return true
    }

    private fun containsEcgDat(directory: File): Boolean {
        val files = directory.listFiles() ?: return false
        for (file in files) {
            if (file.isFile && isEcgDatFile(file)) {
                return true
            }
        }
        return false
    }

    private fun isEcgDatFile(file: File): Boolean {
        val fileExtensions = "ecg.dat"
        val fileName = file.name.lowercase(Locale.getDefault())
        return if (fileName.equals(fileExtensions)) {
            true
        } else false
    }

    private fun containsHrDat(directory: File): Boolean {
        val files = directory.listFiles() ?: return false
        for (file in files) {
            if (file.isFile && isHrDatFile(file)) {
                return true
            }
        }
        return false
    }

    private fun isHrDatFile(file: File): Boolean {
        val fileExtensions = "hr.dat"
        val fileName = file.name.lowercase(Locale.getDefault())
        return if (fileName.equals(fileExtensions)) {
            true
        } else false
    }

    private fun saveDataToJSON() {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

        if (!documentsDir.exists()) {
            documentsDir.mkdirs() // Crea la cartella se necessario
        }

        val jsonFile = File(documentsDir, "ecg.json")
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonStr = gson.toJson(recordedData)

        val hrfile = File(documentsDir, "hr.json")
        val json = GsonBuilder().setPrettyPrinting().create()
        val jsonString = json.toJson(recordedDataHR)

        try {
            FileWriter(jsonFile).use { writer ->
                writer.write(jsonStr)
            }
            FileWriter(hrfile).use { writer ->
                writer.write(jsonString)
            }
            Toast.makeText(this, "Data saved to ecg.json", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show()
        }


    }


    public override fun onDestroy() {
        super.onDestroy()
        ecgDisposable?.let {
            if (!it.isDisposed) it.dispose()
        }
        api.shutDown()
    }

    private var ecgStreamThread: Thread? = null

    private fun startEcgStreamThread() {
        ecgStreamThread = thread(start = true) {
            prova()
        }
    }

    private fun stopEcgStreamThread() {
        ecgStreamThread?.interrupt()
    }
    fun prova() {
        while (!Thread.currentThread().isInterrupted) {
            val isDisposed = ecgDisposable?.isDisposed ?: true
            if (isDisposed) {
                ecgDisposable = api.requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.ECG)
                        .toFlowable()
                        .flatMap { sensorSetting: PolarSensorSetting -> api.startEcgStreaming(deviceId, sensorSetting.maxSettings()) }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { polarEcgData: PolarEcgData ->
                                    for (data in polarEcgData.samples) {
                                        ecgPlotter.sendSingleSample((data.voltage.toFloat() / 1000.0).toFloat())

                                        if (isRecording) {
                                            try {
                                                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                                val timestamp = sdf.format(Date())
                                                synchronized(buffer) {
                                                    buffer.add("$timestamp,${(data.voltage.toFloat() / 1000.0).toString()}")
                                                    if (buffer.size >= bufferSize) {
                                                        saveBufferToRecordedData()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                Log.e(TAG, "Error while recording data: ${e.message}")
                                            }
                                        }
                                    }
                                    // Aggiornamenti dell'interfaccia utente e altre operazioni qui
                                },
                                { error: Throwable ->
                                    Log.e(TAG, "Ecg stream failed $error")
                                    ecgDisposable = null
                                },
                                {
                                    Log.d(TAG, "Ecg stream complete")
                                }
                        )
            } else {
                // NOTE stops streaming if it is "running"
                ecgDisposable?.dispose()
                ecgDisposable = null
            }

            // Aggiungi un ritardo prima di eseguire nuovamente lo streaming
            try {
                Thread.sleep(1000) // Ritardo di 1 secondo
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }



    fun streamECG() {
        val isDisposed = ecgDisposable?.isDisposed ?: true
        if (isDisposed) {
            ecgDisposable = api.requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.ECG)
                    .toFlowable()
                    .flatMap { sensorSetting: PolarSensorSetting -> api.startEcgStreaming(deviceId, sensorSetting.maxSettings()) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { polarEcgData: PolarEcgData ->
                                Log.d(TAG, "ecg update")
                                for (data in polarEcgData.samples) {
                                    ecgPlotter.sendSingleSample((data.voltage.toFloat() / 1000.0).toFloat())

                                    /*if (isRecording) {
                                        try {
                                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                            val timestamp = sdf.format(Date())
                                            synchronized(buffer) {
                                                buffer.add("$timestamp,${(data.voltage.toFloat() / 1000.0).toString()}")
                                                if (buffer.size >= bufferSize) {
                                                    saveBufferToRecordedData()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Log.e(TAG, "Error while recording data: ${e.message}")
                                        }
                                    }*/
                                }
                            },
                            { error: Throwable ->
                                Log.e(TAG, "Ecg stream failed $error")
                                ecgDisposable = null
                            },
                            {
                                Log.d(TAG, "Ecg stream complete")
                            }
                    )
        } else {
            // NOTE stops streaming if it is "running"
            ecgDisposable?.dispose()
            ecgDisposable = null
        }
    }

    fun streamHR() {
        val isDisposed = hrDisposable?.isDisposed ?: true
        if (isDisposed) {
            hrDisposable = api.startHrStreaming(deviceId)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { hrData: PolarHrData ->
                                for (sample in hrData.samples) {
                                    Log.d(TAG, "HR " + sample.hr)
                                    if (sample.rrsMs.isNotEmpty()) {
                                        val rrText = "(${sample.rrsMs.joinToString(separator = "ms, ")}ms)"
                                        textViewRR.text = rrText
                                    }

                                    textViewHR.text = sample.hr.toString()
                                    if(isRecording) {
                                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                        val timestamp = sdf.format(Date()) // Ottieni il timestamp corrente
                                        recordedDataHR.add("$timestamp,${sample.hr.toString()},(${sample.rrsMs.joinToString(separator = "ms, ")}ms)")

                                    }

                                }
                            },
                            { error: Throwable ->
                                Log.e(TAG, "HR stream failed. Reason $error")
                                hrDisposable = null
                            },
                            { Log.d(TAG, "HR stream complete") }
                    )
        } else {
            // NOTE stops streaming if it is "running"
            hrDisposable?.dispose()
            hrDisposable = null
        }
    }

    override fun update() {
        runOnUiThread { plot.redraw() }
    }
}
