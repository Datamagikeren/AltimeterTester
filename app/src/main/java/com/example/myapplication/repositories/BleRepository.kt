import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.myapplication.blescanner.model.ExpectedResults
import com.example.myapplication.blescanner.model.TestResponseObject
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.util.*

@Suppress("DEPRECATION")
class BleRepository(private val context: Context) {

    private val btManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val _activeConnections =
        MutableLiveData<MutableMap<String, BluetoothGatt>>(mutableMapOf())
    val activeConnections: MutableLiveData<MutableMap<String, BluetoothGatt>> = _activeConnections

    //val activeBleConnections: MutableSet<BluetoothDevice> = mutableSetOf()
    var foundBTDevices: MutableList<BluetoothDevice> = mutableListOf()
    val testResponse = MutableLiveData<String>()
    private val _testResponseList =
        MutableLiveData<MutableMap<String, TestResponseObject>>(mutableMapOf())
    val testResponseList: LiveData<MutableMap<String, TestResponseObject>> = _testResponseList
    var quattroDevices: MutableSet<BluetoothDevice> = mutableSetOf()

    private val _quattroDevicesCount = MutableLiveData<Int>()
    val quattroDevicesCount: LiveData<Int> = _quattroDevicesCount
    var isSendinUart: Boolean = false
    val expectedResultsLiveData = MutableLiveData<ExpectedResults>()

    private val retryLimit = 3
    private val retryCounts = mutableMapOf<String, Int>()


    private val UART_SERVICE_UUID =
        UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    private val UART_WRITE_CHARACTERISTIC_UUID =
        UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    private val CLIENT_CHARACTERISTIC_CONFIG_UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private val UART_NOTIFY_CHARACTERISTIC_UUID =
        UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            val deviceAddress = gatt.device.address
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                gatt.discoverServices()
                // Update LiveData map
                _activeConnections.value?.put(gatt.device.address, gatt)
                _activeConnections.postValue(_activeConnections.value) // Notify observers
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Update LiveData map on disconnection
                Log.d("Disconnect", "Device $deviceAddress disconnected")
                //_activeConnections.value?.clear()
                _activeConnections.value?.remove(gatt.device.address)
                _activeConnections.postValue(_activeConnections.value) // Notify observers

/*
                // Check if reconnection is needed
                val currentRetryCount = retryCounts[deviceAddress] ?: 0
                if (currentRetryCount < retryLimit) {
                    retryCounts[deviceAddress] = currentRetryCount + 1
                    reconnectToDevice(deviceAddress)
                    Log.d("Reconnect", "Reconnected to device $deviceAddress")
                } else {
                    // Max retries reached, handle accordingly
                    retryCounts.remove(deviceAddress)
                } */
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val uartService = gatt.getService(UART_SERVICE_UUID)
                val notifyCharacteristic =
                    uartService?.getCharacteristic(UART_NOTIFY_CHARACTERISTIC_UUID)
                notifyCharacteristic?.let { enableNotifications(gatt, it) }
            }
        }


        private val dataBuffer = StringBuilder() // Buffer to accumulate data

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            super.onCharacteristicChanged(gatt, characteristic)

            // Check if this is the expected characteristic
            if (characteristic.uuid == UART_NOTIFY_CHARACTERISTIC_UUID) {
                // Extract the data
                val data = characteristic.value
                val dataString = data?.let { String(it) } ?: "Data is null"

                // Append to buffer
                dataBuffer.append(dataString)

                if (dataString.contains("EOF")) {
                    // Remove the EOF marker and process data
                    val completeData = dataBuffer.toString().replace("EOF", "")
                    testResponse.postValue(completeData)

                    // Process completeData and update testResponseList
                    processCompleteDataAndUpdateList(gatt.device.address, completeData)

                    // Clear buffer
                    dataBuffer.clear()
                }
            }

        }

    }

    // Function to process the complete data and update the testResponseList LiveData
    private fun processCompleteDataAndUpdateList(deviceAddress: String, data: String) {
        // TODO: Convert 'data' into TestResponseObject
        val testResponseObject = convertDataToTestResponseObject(data)!!

        // Update the LiveData map
        val currentList = _testResponseList.value ?: mutableMapOf()
        currentList[deviceAddress] = testResponseObject
        _testResponseList.postValue(currentList)
        Log.d("TestResponseList", _testResponseList.value!!.count().toString())
        testResponseList(_testResponseList.value!!)
        isSendinUart = false
    }

    private fun convertDataToTestResponseObject(data: String): TestResponseObject? {
        val gson = Gson()

        // Find the index of the opening brace of the JSON object
        val startIndex = data.indexOf('{')
        val endIndex = data.indexOf('}', startIndex) + 1

        // Check if both braces are found
        val cleanJsonString = if (startIndex >= 0 && endIndex > 0 && endIndex <= data.length) {
            data.substring(startIndex, endIndex)
        } else {
            Log.e("JSON Parsing", "Invalid JSON format")
           val f = "fail"
            return TestResponseObject(f,f,f,f,f,f,f,f,f,f,f,f, 1,f,f)
        }

        Log.d("TestResponseJson To Be Json", cleanJsonString)

        return try {
            gson.fromJson(cleanJsonString, TestResponseObject::class.java)
        } catch (e: JsonSyntaxException) {
            Log.e("JSON Parsing", "Error parsing JSON", e)
            null // or return a default TestResponseObject
        }
    }

    fun resetScannedCounter(){
        _quattroDevicesCount.value = 0
    }
    // Function to update the count
    fun updateQuattroDevicesCount(newCount: Int) {
        _quattroDevicesCount.value = newCount
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun connectToDevice(device: BluetoothDevice) {
        retryCounts[device.address] = 0 // Reset retry count on new connection attempt
        device.connectGatt(context, false, gattCallback)
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private fun reconnectToDevice(deviceAddress: String) {
        val device = btManager.adapter.getRemoteDevice(deviceAddress)
        connectToDevice(device)
    }

    fun disconnectFromDevice(deviceAddress: String) {
        _activeConnections.value?.let { connections ->
            connections[deviceAddress]?.let { gatt ->
                gatt.disconnect()
                gatt.close()
                connections.remove(deviceAddress)
                _activeConnections.postValue(connections) // Notify observers
            }
        }
    }


    @SuppressLint("MissingPermission")
    fun sendUartMessage(deviceAddress: String, message: String) {
        // Retrieve the current map from MutableLiveData
        isSendinUart = true
        val connections = _activeConnections.value

        // Access the BluetoothGatt object from the map
        val gatt = connections?.get(deviceAddress)

        val uartService = gatt?.getService(UART_SERVICE_UUID)
        val writeCharacteristic = uartService?.getCharacteristic(UART_WRITE_CHARACTERISTIC_UUID)

        writeCharacteristic?.let { characteristic ->
            characteristic.setValue(message)
            gatt.writeCharacteristic(characteristic)
        }
    }


    @SuppressLint("MissingPermission")
    private fun enableNotifications(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
    ) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID).apply {
            value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        }
        gatt.writeDescriptor(descriptor)
    }
    fun testResponseList(responseList: Map<String, TestResponseObject>) {
        val expectedResults = expectedResultsLiveData.value

        // Parse the expected MBar value
        val expectedMBar = expectedResults!!.MBar.toDoubleOrNull()
        if (expectedMBar == null) {
            Log.e("TestResponse", "Invalid MBar in expected results")
            return
        }

        responseList.forEach { (key, response) ->
            try {
                // Parse the MBar value in the response
                Log.d("MbarResponse", response.MBar)
                val responseMBar = response.MBar.toDoubleOrNull()
                if (responseMBar == null) {
                    Log.e("TestResponse", "Invalid MBar in response for key $key")
                } else {
                    // Check if the response MBar is within the range of expected MBar ± 10
                    if (responseMBar in (expectedMBar - 10)..(expectedMBar + 10)) {
                        println("TestResponseObject with key $key has an acceptable MBar value.")
                        response.TestPassed = 2
                    } else {
                        println("TestResponseObject with key $key has an MBar value outside the acceptable range.")
                        response.TestPassed = 3
                    }
                }
            } catch (e: Exception) {
                Log.e("TestResponse", "Error processing response for key $key: ${e.message}")
                response.TestPassed = 3
            }
        }
    }

}
