package com.java.heartrategooglefit

import android.Manifest
import android.Manifest.permission.ACTIVITY_RECOGNITION
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType

@Suppress("UNREACHABLE_CODE")
class MainActivity : AppCompatActivity() {


    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val PERMISSION_REQUEST_FINE_LOCATION = 2
        val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 100
    }

    private lateinit var scanButton: Button
    private lateinit var deviceListView: ListView
    private var selectedDevice: BluetoothDevice? = null

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private lateinit var devicesArrayAdapter: ArrayAdapter<String>

    // Create a HashSet to track connected devices
    private val connectedDevices: HashSet<BluetoothDevice> = HashSet()

    private val scanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val deviceName = device.name ?: "Unknown Device"
            devicesArrayAdapter.add(deviceName)
        }
    }

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {


        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                showToast("Connecting to GATT server")
                if(newState == BluetoothProfile.STATE_CONNECTED){
                    showToast("Discovering the services")
                    gatt?.discoverServices()
                }
                else if (newState == BluetoothProfile.STATE_DISCONNECTED)
                {
                    showToast("Disconnected the server the services")
                    gatt?.close()
                }

            } else {
                showToast("Not able to connect to the device from GATT server")
                gatt?.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                showToast("Services discovered")
                val services: List<BluetoothGattService>? = gatt?.services
                // Loop through services and characteristics
                // ...

                if (services != null) {
                    for (service in services){
                        val serviceName = service.uuid.toString() // Change this to the correct way of getting service name
                        Toast.makeText(this@MainActivity, "Service: $serviceName", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                showToast("Failed to discover services")
            }
        }

        // Implement other methods for reading/writing characteristics
        // ...
    }



    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)




        scanButton = findViewById(R.id.scanButton)
        deviceListView = findViewById(R.id.deviceListView)

        devicesArrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        deviceListView.adapter = devicesArrayAdapter

        deviceListView.setOnItemClickListener { _, _, position, _ ->
            val deviceName = devicesArrayAdapter.getItem(position)
            val device = bluetoothAdapter?.bondedDevices?.find { it.name == deviceName }

            if (device != null) {
                showToast("click")
                connectToDevice(device)
            }
        }

        scanButton.setOnClickListener {
            checkPermissionsAndStartScan()
        }

        //Create a Fitness API client
        //Create a FitnessOptions instance, declaring the data types and access type (read and/or write) your app needs:
        val fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
            .build()

        //Get an instance of the Account object to use with the API:
        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)

        //Check if the user has previously granted the necessary data access, and if not, initiate the authorization flow:

        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                this, // your activity
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE, // e.g. 1
                account,
                fitnessOptions)
        }


    }


    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        showToast("inside connectToDevice()")
        selectedDevice = device // Store the selected device for connection
        val gatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private fun checkPermissionsAndStartScan() {
        if (bluetoothAdapter == null) {
            showToast("Bluetooth not supported on this device")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent =  Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else {
                // Handle the case where permission is not granted.
                // You might want to request the permission or display a message to the user.
                showToast("Bluetooth permission required.")
            }

        } else {
            startScanning()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    private fun startScanning() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_FINE_LOCATION
            )
        } else {
            devicesArrayAdapter.clear()
            val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
            bluetoothLeScanner?.startScan(scanCallback)

        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            when (resultCode) {
                Activity.RESULT_OK -> when (requestCode) {
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE -> Toast.makeText(this , "result is from Google Fit", Toast.LENGTH_SHORT).show()
                    else -> {
                        // Result wasn't from Google Fit
                    }
                }
                REQUEST_ENABLE_BT -> {
                    if (resultCode == Activity.RESULT_OK) {
                        startScanning()
                    } else {
                        showToast("Bluetooth permission denied")
                    }
                }

            }
        }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted

            Toast.makeText(this, "ACTIVITY_RECOGNITION permission granted", Toast.LENGTH_SHORT)
                .show()

        } else if (requestCode == PERMISSION_REQUEST_FINE_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning()
            } else {
                showToast("Location permission denied")
            }
        }
    }

}