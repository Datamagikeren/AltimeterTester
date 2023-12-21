package com.example.myapplication.blescanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import com.example.myapplication.blescanner.model.BleScanCallback
import java.util.UUID

class BleScanManager(
    btManager: BluetoothManager,
    private val scanPeriod: Long = DEFAULT_SCAN_PERIOD,
    private val scanCallback: BleScanCallback = BleScanCallback()
) {


    private val btAdapter = btManager.adapter
    private val bleScanner = btAdapter.bluetoothLeScanner

    var beforeScanActions: MutableList<() -> Unit> = mutableListOf()
    var afterScanActions: MutableList<() -> Unit> = mutableListOf()

    /** True when the manager is performing the scan */
    private var scanning = false

    private val handler = Handler(Looper.getMainLooper())

    /**
     * Scans for Bluetooth LE devices and stops the scan after [scanPeriod] seconds.
     * Does not checks the required permissions are granted, check must be done beforehand.
     */
    @SuppressLint("MissingPermission")
    fun scanBleDevices() {
        fun stopScan() {
            scanning = false
            bleScanner.stopScan(scanCallback)
            executeAfterScanActions()
        }

        if (scanning) {
            stopScan()
        } else {
            handler.postDelayed({ stopScan() }, scanPeriod)
            executeBeforeScanActions()

            // Create scan settings
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            // Start scanning with filters
            scanning = true
            bleScanner.startScan(createScanFilters(), scanSettings, scanCallback)
        }
    }

    companion object {
        const val DEFAULT_SCAN_PERIOD: Long = 2000

        private fun executeListOfFunctions(toExecute: List<() -> Unit>) {
            toExecute.forEach {
                it()
            }

        }}
    private fun executeBeforeScanActions() {
        executeListOfFunctions(beforeScanActions)
    }

    private fun executeAfterScanActions() {
        executeListOfFunctions(afterScanActions)
    }

    val targetUuids = listOf(
        UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"),
    )
    private fun createScanFilters(): List<ScanFilter> {
        return targetUuids.map { uuid ->
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(uuid))
                .build()
        }
    }


}