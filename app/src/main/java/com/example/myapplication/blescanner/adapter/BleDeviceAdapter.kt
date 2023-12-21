package com.example.myapplication.blescanner.adapter

import BleRepository
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.blescanner.model.TestResponseObject

/* class BleDeviceAdapter (
    private val activeConnections: MutableSet<BluetoothDevice>,
    private val repo: BleRepository,
    private val onItemClicked: (position: Int) -> Unit

) :
    RecyclerView.Adapter<BleDeviceAdapter.ViewHolder>() {

    var copyOfList = repo.testReponseList

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val deviceNameTextView: TextView = itemView.findViewById(R.id.device_name)
        val deviceMac: TextView = itemView.findViewById(R.id.device_mac)
        val jsonResponseTextView: TextView = itemView.findViewById(R.id.json_response)


        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onItemClicked(position)
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
        val deviceList = activeConnections.toList()  // Convert the set to a list
        val device = deviceList[position]  // Get the BluetoothDevice at the given position


        holder.deviceMac.text = device.address  // Set the MAC address
        try {
            holder.deviceNameTextView.text = device.name ?: "Unknown Device"  // Set the device name
        } catch (e: SecurityException) {
            holder.deviceNameTextView.text = "Name unavailable"
            // Optionally log the exception or inform the user
        }

        // Check if the MAC address is in the testReponseList
        val testResponse = copyOfList[device.address]
        if (testResponse != null) {
            // Update the jsonResponseTextView with the test response
            holder.jsonResponseTextView.text = "Mac:${testResponse.macAddress} Mbar: ${testResponse.MBar} Temp: ${testResponse.Temperature} Volt: ${testResponse.Volt}"
        } else {
            // Set a default text or leave it blank if no response is associated with this device
            holder.jsonResponseTextView.text = "No response"
        }

    }
    fun updateTestResponses(newTestResponseList: MutableMap<String, TestResponseObject>) {
        copyOfList = newTestResponseList
        notifyDataSetChanged()  // Notify the adapter that the data has changed
    }


    override fun getItemCount(): Int {
        return activeConnections.size
    }


} */