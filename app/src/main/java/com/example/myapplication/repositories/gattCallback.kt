

package com.example.myapplication.repositories

import BleRepository
import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import androidx.core.content.ContentProviderCompat.requireContext


object gattCallback : BluetoothGattCallback() {

    /*

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
            _activeConnections.value?.remove(gatt.device.address)
            _activeConnections.postValue(_activeConnections.value) // Notify observers

            // Check if reconnection is needed
            val currentRetryCount = retryCounts[deviceAddress] ?: 0
            if (currentRetryCount < retryLimit) {
                retryCounts[deviceAddress] = currentRetryCount + 1
                reconnectToDevice(deviceAddress)
            } else {
                // Max retries reached, handle accordingly
                retryCounts.remove(deviceAddress)
            }
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

    } */
}