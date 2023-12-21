package com.example.myapplication.blescanner.adapter

import android.bluetooth.BluetoothGatt
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.blescanner.model.TestResponseObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BleConnectedDeviceAdapter(
    private var activeConnections: Map<String, BluetoothGatt>,
    private var testResponseMap: Map<String, TestResponseObject>,
    private val onItemClicked: (position: Int, Any?) -> Unit

) :
    RecyclerView.Adapter<BleConnectedDeviceAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val deviceNameTextView: TextView = itemView.findViewById(R.id.device_name)
        val deviceMac: TextView = itemView.findViewById(R.id.device_mac)
        val jsonResponseTextView: TextView = itemView.findViewById(R.id.json_response)
        val relativeLayout: RelativeLayout = itemView.findViewById(R.id.card_color)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                // Retrieve the device address at this position
                val deviceAddress = activeConnections.keys.elementAt(position)
                onItemClicked(position, deviceAddress)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val deviceView = inflater.inflate(R.layout.list_item_card, parent, false)

        return ViewHolder(deviceView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val deviceAddress = activeConnections.keys.toList()[position]
        val bluetoothGatt = activeConnections[deviceAddress]
        val device = bluetoothGatt?.device  // Get the BluetoothDevice associated with this Gatt

        holder.deviceMac.text = device?.address ?: "Unknown Address"
        try {
            holder.deviceNameTextView.text = device?.name ?: "Unknown Device"
        } catch (e: SecurityException) {
            holder.deviceNameTextView.text = "Name unavailable"
            // Optionally log the exception or inform the user
        }

        val testResponse = testResponseMap[device!!.address]
        if (testResponse?.TestPassed == 2) {
            holder.relativeLayout.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.green))
        } else if (testResponse?.TestPassed == 3){
            holder.relativeLayout.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.red))
        }
        else {
            // Set a default color or handle the case where testResponse is null
            // For example, setting to red or any other default color
            holder.relativeLayout.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.orange))
        }
        holder.jsonResponseTextView.text = testResponse?.let {
            "${it.macAddress} Mbar: ${it.MBar} Temp: ${it.Temperature} Volt: ${it.Volt}"
        } ?: "No response"
    }
    private var disconnectionCounter = 0
    private val disconnectionThreshold = 2 // Number of disconnections to trigger the clear action
    private val disconnectionTimeWindow = 2000L // Time window in milliseconds (2 seconds)
    private var disconnectionTimer: Job? = null
    fun updateActiveConnections(newActiveConnections: Map<String, BluetoothGatt>) {
        // Check if the new map is significantly smaller than the current map
        if (this.activeConnections.size > newActiveConnections.size) {
            // Increment disconnection counter
            disconnectionCounter++

            // Start or reset the timer
            disconnectionTimer?.cancel()
            disconnectionTimer = GlobalScope.launch {
                Log.d("DCLogger", "DClogger activated")
                delay(disconnectionTimeWindow)
                if (disconnectionCounter >= disconnectionThreshold) {
                    // Clear the list if the threshold is reached within the time window
                    this@BleConnectedDeviceAdapter.activeConnections = emptyMap()
                    withContext(Dispatchers.Main) {
                        notifyDataSetChanged()
                    }
                }
                // Reset the counter
                disconnectionCounter = 0
            }
        } else {
            // Update the list normally
            this.activeConnections = newActiveConnections
            notifyDataSetChanged()
        }
    }

    fun updateTestResponses(newTestResponseMap: Map<String, TestResponseObject>) {
        this.testResponseMap = newTestResponseMap
        notifyDataSetChanged()  // Notify the adapter that the data has changed
    }

    override fun getItemCount(): Int {
        return activeConnections.size
    }
}
