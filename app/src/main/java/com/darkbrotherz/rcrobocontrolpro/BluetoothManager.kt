package com.darkbrotherz.rcrobocontrolpro

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.*
import java.io.OutputStream
import java.util.UUID

object BluetoothManager {
    var bluetoothSocket: BluetoothSocket? = null
    val myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var serverJob: Job? = null

    fun sendData(data: String) {
        try {
            val outputStream: OutputStream? = bluetoothSocket?.outputStream
            outputStream?.write(data.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Robust client connection with Insecure & Reflection fallbacks.
     */
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice): Boolean {
        var socket: BluetoothSocket? = null

        // Attempt 1: Standard Secure Socket
        try {
            socket = device.createRfcommSocketToServiceRecord(myUUID)
            socket.connect()
            bluetoothSocket = socket
            return true
        } catch (e1: Exception) {
            e1.printStackTrace()
        }

        // Attempt 2: Insecure Socket
        try {
            socket = device.createInsecureRfcommSocketToServiceRecord(myUUID)
            socket.connect()
            bluetoothSocket = socket
            return true
        } catch (e2: Exception) {
            e2.printStackTrace()
        }

        // Attempt 3: Reflection Fallback on RFCOMM Port 1
        try {
            val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            socket = method.invoke(device, 1) as BluetoothSocket
            socket.connect()
            bluetoothSocket = socket
            return true
        } catch (e3: Exception) {
            e3.printStackTrace()
        }

        return false
    }

    /**
     * Background Server listener so Phone B can accept incoming connections from Phone A.
     */
    @SuppressLint("MissingPermission")
    fun startServerListener(scope: CoroutineScope, onConnected: (String) -> Unit) {
        serverJob?.cancel()
        serverJob = scope.launch(Dispatchers.IO) {
            var serverSocket: BluetoothServerSocket? = null
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter() ?: return@launch
                serverSocket = adapter.listenUsingInsecureRfcommWithServiceRecord("RoboControl", myUUID)
                val socket = serverSocket.accept()
                if (socket != null) {
                    bluetoothSocket = socket
                    withContext(Dispatchers.Main) {
                        onConnected(socket.remoteDevice.name ?: "Remote Phone")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    serverSocket?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
