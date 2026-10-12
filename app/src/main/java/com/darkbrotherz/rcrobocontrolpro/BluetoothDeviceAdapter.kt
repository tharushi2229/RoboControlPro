package com.darkbrotherz.rcrobocontrolpro

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BluetoothDeviceAdapter(
    private val deviceList: List<BluetoothDevice>,
    private val onDeviceClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDeviceName: TextView = view.findViewById(android.R.id.text1)
        val tvDeviceAddress: TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return DeviceViewHolder(view)
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = deviceList[position]
        holder.tvDeviceName.text = device.name ?: "Unknown Device"
        holder.tvDeviceAddress.text = device.address
        holder.itemView.setOnClickListener { onDeviceClick(device) }
    }

    override fun getItemCount(): Int = deviceList.size
}