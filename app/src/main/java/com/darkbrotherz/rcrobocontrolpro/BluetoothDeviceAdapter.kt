package com.darkbrotherz.rcrobocontrolpro

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Bluetooth උපාංග ලැයිස්තුවක් (RecyclerView) සඳහා Adapter එක
class BluetoothDeviceAdapter(
    private val deviceList: List<BluetoothDevice>,
    private val onDeviceClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Android වල තියෙන සරල පේළි දෙකේ ඩිසයින් එකේ කොටස් හොයාගැනීම
        val tvDeviceName: TextView = view.findViewById(android.R.id.text1)
        val tvDeviceAddress: TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        // android.R.layout.simple_list_item_2 යනු Android වලම ඇති සරල ඩිසයින් එකකි
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return DeviceViewHolder(view)
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = deviceList[position]

        // උපාංගයේ නම (නමක් නැත්නම් 'Unknown Device' ලෙස පෙන්වයි)
        holder.tvDeviceName.text = device.name ?: "Unknown Device"
        // උපාංගයේ MAC ලිපිනය පෙන්වීම
        holder.tvDeviceAddress.text = device.address

        // ලැයිස්තුවේ යම් උපාංගයක් මත Click කළ විට ක්‍රියාත්මක වීමට
        holder.itemView.setOnClickListener {
            onDeviceClick(device)
        }
    }

    override fun getItemCount(): Int {
        return deviceList.size
    }
}