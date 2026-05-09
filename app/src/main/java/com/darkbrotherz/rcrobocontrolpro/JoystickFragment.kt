package com.darkbrotherz.rcrobocontrolpro

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.IOException
import java.util.UUID

class JoystickFragment : Fragment() {

    private var bluetoothAdapter: BluetoothAdapter? = null

    // Arduino වලට සම්බන්ධ වෙන ප්‍රධාන Socket එක
    private var bluetoothSocket: BluetoothSocket? = null

    // HC-05 / HC-06 සඳහා භාවිතා කරන සම්මත මුරපදය (UUID)
    private val myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_joystick, container, false)

        val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        val btnDisconnect = view.findViewById<Button>(R.id.btnDisconnect)
        val statusBadgeCard = view.findViewById<CardView>(R.id.statusBadgeCard)

        btnDisconnect.setOnClickListener { showBluetoothDevicesDialog() }
        statusBadgeCard.setOnClickListener { showBluetoothDevicesDialog() }

        return view
    }

    @SuppressLint("MissingPermission")
    private fun showBluetoothDevicesDialog() {
        if (bluetoothAdapter == null) {
            Toast.makeText(requireContext(), "මෙම උපාංගයට Bluetooth පහසුකම නොමැත", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter!!.isEnabled) {
            Toast.makeText(requireContext(), "කරුණාකර පළමුව Bluetooth On කරන්න", Toast.LENGTH_SHORT).show()
            return
        }

        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.bottom_sheet_bluetooth, null)

        val rvBluetoothDevices = dialogView.findViewById<RecyclerView>(R.id.rvBluetoothDevices)

        if (rvBluetoothDevices != null) {
            rvBluetoothDevices.layoutManager = LinearLayoutManager(requireContext())

            val pairedDevices = bluetoothAdapter!!.bondedDevices.toList()

            if (pairedDevices.isNotEmpty()) {
                val adapter = BluetoothDeviceAdapter(pairedDevices) { device ->
                    Toast.makeText(requireContext(), "${device.name} වෙත සම්බන්ධ වෙමින්...", Toast.LENGTH_SHORT).show()
                    bottomSheetDialog.dismiss()

                    // ලැයිස්තුවෙන් තේරූ පසු Arduino එකට Connect වෙන කේතය ක්‍රියාත්මක කිරීම
                    connectToDevice(device.address)
                }
                rvBluetoothDevices.adapter = adapter
            } else {
                Toast.makeText(requireContext(), "Paired කරපු Bluetooth උපාංග කිසිවක් නැත", Toast.LENGTH_SHORT).show()
            }
        }

        bottomSheetDialog.setContentView(dialogView)
        bottomSheetDialog.show()
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(address: String) {
        // ඇප් එක හිර නොවී තියෙන්න වෙනම Thread එකකින් Connect කිරීම සිදු කරයි
        Thread {
            try {
                val device = bluetoothAdapter?.getRemoteDevice(address)
                bluetoothSocket = device?.createRfcommSocketToServiceRecord(myUUID)
                bluetoothAdapter?.cancelDiscovery()
                bluetoothSocket?.connect()

                // සම්බන්ධය සාර්ථක නම් පණිවිඩයක් පෙන්වීම
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "සාර්ථකව සම්බන්ධ විය!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                // සම්බන්ධ වීමට නොහැකි වුවහොත් පණිවිඩයක් පෙන්වීම
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "සම්බන්ධ වීමට නොහැකි විය. Bluetooth උපාංගය On දැයි බලන්න.", Toast.LENGTH_LONG).show()
                }
                try {
                    bluetoothSocket?.close()
                } catch (e2: IOException) {
                    e2.printStackTrace()
                }
            }
        }.start()
    }
}