package com.example.wellness

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity


class PolarActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "Polar_MainActivity"
        private const val SHARED_PREFS_KEY = "polar_device_id"
        private const val PERMISSION_REQUEST_CODE = 1
    }

    private lateinit var sharedPreferences: SharedPreferences
    private val bluetoothOnActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode != Activity.RESULT_OK) {
            Log.w(TAG, "Bluetooth off")
        }
    }
    private var deviceId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.polar_activity)
        sharedPreferences = getPreferences(MODE_PRIVATE)
        deviceId = sharedPreferences.getString(SHARED_PREFS_KEY, "")

        val setIdButton: Button = findViewById(R.id.buttonSetID)
        val ecgConnectButton: Button = findViewById(R.id.buttonConnectEcg)
        //val hrConnectButton: Button = findViewById(R.id.buttonConnectHr)
        val homeView: ImageView = findViewById(R.id.home)
        checkBT()

        setIdButton.setOnClickListener { onClickChangeID(it) }
        ecgConnectButton.setOnClickListener { onClickConnectEcg(it) }
        homeView.setOnClickListener { onClickHome(it) }
        //hrConnectButton.setOnClickListener { onClickConnectHr(it) }
    }

    private fun onClickHome(view: View) {
        val intent = Intent(this@PolarActivity, MainActivity::class.java)
        startActivity(intent)
    }

    private fun onClickConnectEcg(view: View) {
        checkBT()
        if (deviceId == null || deviceId == "") {
            deviceId = sharedPreferences.getString(SHARED_PREFS_KEY, "")
            showDialog(view)
        } else {
            showToast(getString(R.string.connecting) + " " + deviceId)
            val intent = Intent(this, ECGActivity::class.java)
            intent.putExtra("id", deviceId)
            startActivity(intent)
        }
    }

    private fun onClickChangeID(view: View) {
        showDialog(view)
    }

    private fun showDialog(view: View) {
        val dialog = AlertDialog.Builder(this, androidx.appcompat.R.style.AlertDialog_AppCompat_Light)
        dialog.setTitle("Enter your Polar device's ID")
        val viewInflated = LayoutInflater.from(applicationContext).inflate(R.layout.device_id_activity, view.rootView as ViewGroup, false)
        val input = viewInflated.findViewById<EditText>(R.id.input)
        if (deviceId?.isNotEmpty() == true) input.setText(deviceId)
        input.inputType = InputType.TYPE_CLASS_TEXT
        dialog.setView(viewInflated)
        dialog.setPositiveButton("OK") { _: DialogInterface?, _: Int ->
            deviceId = input.text.toString().uppercase()
            val editor = sharedPreferences.edit()
            editor.putString(SHARED_PREFS_KEY, deviceId)
            editor.apply()
        }
        dialog.setNegativeButton("Cancel") { dialogInterface: DialogInterface, _: Int -> dialogInterface.cancel() }
        dialog.show()
    }

    private fun checkBT() {
        val btManager = applicationContext.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = btManager.adapter
        if (bluetoothAdapter == null) {
            showToast("Device doesn't support Bluetooth")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothOnActivityResultLauncher.launch(enableBtIntent)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), PERMISSION_REQUEST_CODE)
            } else {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (index in 0..grantResults.lastIndex) {
                if (grantResults[index] == PackageManager.PERMISSION_DENIED) {
                    Log.w(TAG, "Needed permissions are missing")
                    showToast("Needed permissions are missing")
                    return
                }
            }
            Log.d(TAG, "Needed permissions are granted")
        }
    }

    private fun showToast(message: String) {
        val toast = Toast.makeText(applicationContext, message, Toast.LENGTH_LONG)
        toast.show()
    }
}