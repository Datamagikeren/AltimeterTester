package com.example.myapplication

import BleRepository
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.myapplication.blescanner.BleScanManager
import com.example.myapplication.blescanner.model.BleScanCallback
import com.example.myapplication.databinding.FragmentBlescannerBinding
import com.example.myapplication.permissions.BleScanRequiredPermissions
import com.example.myapplication.permissions.PermissionUtilities
import android.Manifest
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myapplication.blescanner.adapter.BleConnectedDeviceAdapter
import com.example.myapplication.blescanner.model.ExpectedResults
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.IndexOutOfBoundsException

@Suppress("DEPRECATION")
class BleScannerFragment : Fragment() {

    private val permissionsUtilities = PermissionUtilities()
    private val bleScanRequiredPermissions = BleScanRequiredPermissions()
    private lateinit var btManager: BluetoothManager
    private lateinit var bleScanManager: BleScanManager
    private lateinit var bleRepository: BleRepository


    private var _binding: FragmentBlescannerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        _binding = FragmentBlescannerBinding.inflate(inflater, container, false)
        return binding.root

    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("NotifyDataSetChanged", "MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bleRepository = BleRepository(requireContext())
        val activeConnections = bleRepository.activeConnections
        val foundBTDevices = bleRepository.foundBTDevices
        val quattroDevices = bleRepository.quattroDevices
        var expectedResultsLiveData = bleRepository.expectedResultsLiveData

        bleRepository.quattroDevicesCount.observe(viewLifecycleOwner) { count ->
            val text = "$count/9"
            binding.devicesFound.text = text // Update your TextView here
        }


        // Initialize the adapter with an empty map and a click listener
        val adapterConnectedDevices =
            BleConnectedDeviceAdapter(emptyMap(), emptyMap()) { position, deviceAddress ->
                // Handle the click event
                val device = bleRepository.activeConnections.value?.get(deviceAddress)?.device
                device?.let {
                    bleRepository.sendUartMessage(it.address, "Lasse JSON#")
                }
            }


        // Setup RecyclerView
        binding.rvConnectedDevices.apply {
            adapter = adapterConnectedDevices
            layoutManager = GridLayoutManager(context, 3)
        }

        // Observe the LiveData from BleRepository and update the adapter
        activeConnections.observe(viewLifecycleOwner) { newActiveConnections ->
            // Update adapter with new data
            try {
                adapterConnectedDevices.updateActiveConnections(newActiveConnections)
            }catch(e: IndexOutOfBoundsException){
                Log.e("RecyclerViewError", "Caught IndexOutOfBoundsException: ${e.message}")
                activeConnections.value?.clear()
            }
        }

        // Observe the LiveData from BleRepository for test responses
       bleRepository.testResponseList.observe(viewLifecycleOwner) { newTestResponseMap ->
            adapterConnectedDevices.updateTestResponses(newTestResponseMap)
        }


        // BleManager creation
        btManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bleScanManager = BleScanManager(btManager, scanCallback = BleScanCallback({ scanResult ->
            val device = scanResult?.device
            val deviceName = device?.name ?: device?.address
            if (deviceName.isNullOrBlank()) return@BleScanCallback
            val deviceAddress = device?.address ?: "No MAC address found"
            Log.d(
                "BleScannerFragment",
                "Found device: Name = $deviceName, Address = $deviceAddress"
            )
            if (deviceName == "QUATTRO 3") {
                device?.let {
                    if (!quattroDevices.contains(it)) {
                        quattroDevices.add(it)
                        bleRepository.updateQuattroDevicesCount(quattroDevices.size)
                    }
                }
            }
        }))


        // Adding the actions the manager must do before and after scanning
        bleScanManager.beforeScanActions.add {
            foundBTDevices.clear()
        }

        binding.btnScan.setOnClickListener {
            val context = requireContext()
            binding.btnConnect.isEnabled = false
            binding.btnScan.isEnabled = false

            // Check for Bluetooth Connect permission on Android 12 and higher
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    BLE_PERMISSION_REQUEST_CODE
                )
            } else {
                // Continue with existing permissions check and BLE scan
                when (permissionsUtilities.checkPermissionsGranted(
                    context,
                    bleScanRequiredPermissions.permissions
                )) {
                    true -> bleScanManager.scanBleDevices()
                    false -> permissionsUtilities.checkPermissions(
                        requireActivity(),
                        bleScanRequiredPermissions.permissions,
                        BLE_PERMISSION_REQUEST_CODE
                    )
                }
            }

            // Re-enable the connect button after 2 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                binding.btnConnect.isEnabled = true
                binding.btnScan.isEnabled = true
            }, BleScanManager.DEFAULT_SCAN_PERIOD + 1000)
        }



        val btnConnect = binding.btnConnect
        btnConnect.setOnClickListener {
            quattroDevices.forEach { device ->
                bleRepository.connectToDevice(device)
            }
            bleRepository.resetScannedCounter()
            bleRepository.quattroDevices.clear()
            foundBTDevices.clear()
        }

        val btnDisconnectQuattro = binding.btnDisconnect
        btnDisconnectQuattro.setOnClickListener {
            // Create a copy of the device addresses to disconnect
            val devicesToDisconnect = bleRepository.activeConnections.value?.keys?.toList()

            // Disconnect devices using the copy of the addresses
            devicesToDisconnect?.forEach { deviceAddress ->
                bleRepository.disconnectFromDevice(deviceAddress)
            }

            // Notify the adapter outside of the iteration
            adapterConnectedDevices.notifyDataSetChanged()
        }


        binding.btnSendUartAll.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) {
                val devicesToSendUart = bleRepository.activeConnections.value?.keys?.toList()
                devicesToSendUart?.forEach { device ->
                    bleRepository.sendUartMessage(device, "Lasse JSON#")
                    waitForUartSending()
                } ?: run {
                    Log.e("Error", "No devices to send UART to")
                }
            }
        }


        binding.btnShutdownAll.setOnClickListener {
            val devicesToShutDown = bleRepository.activeConnections.value?.keys?.toList()
            devicesToShutDown!!.forEach { device ->
                bleRepository.sendUartMessage(device, "Reset Reset#")
            }
        }
        val editTextMBar = binding.editTextMBar
        val btnSave = binding.saveMbar

        btnSave.setOnClickListener {
            val expectedResults = ExpectedResults(MBar = "1010")
            val newMBarValue = editTextMBar.text.toString()
            expectedResults.MBar = newMBarValue
            expectedResultsLiveData.postValue(expectedResults)
        }


    }
    private suspend fun waitForUartSending() {
        while (bleRepository.isSendinUart) {
            delay(100) // Wait for 100ms before checking again
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        permissionsUtilities.dispatchOnRequestPermissionsResult(
            requestCode,
            grantResults,
            onGrantedMap = mapOf(BLE_PERMISSION_REQUEST_CODE to { bleScanManager.scanBleDevices() }),
            onDeniedMap = mapOf(BLE_PERMISSION_REQUEST_CODE to {
                Toast.makeText(
                    requireContext(),
                    "Some permissions were not granted, please grant them and try again",
                    Toast.LENGTH_LONG
                ).show()
            })
        )
    }

    companion object {
        private const val BLE_PERMISSION_REQUEST_CODE = 1
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}