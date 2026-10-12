package com.darkbrotherz.rcrobocontrolpro

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Data class representing the state of the LED control system.
 */
data class LedState(
    val red: Int,
    val green: Int,
    val blue: Int,
    val brightness: Int,
    val activeLedIds: List<String>
)

/**
 * Robust Sync Manager for generating and sending chunked data packets
 * over Bluetooth for Arduino LED control.
 */
class LedSyncManager(
    private val scope: CoroutineScope,
    private val sendDataFunc: (String) -> Unit
) {

    @Volatile
    private var currentState: LedState? = null

    @Volatile
    private var previousState: LedState? = null

    private var sendJob: Job? = null
    private var periodicJob: Job? = null
    private val isTransmitting = AtomicBoolean(false)

    /**
     * Starts the 0.3s (300ms) periodic check loop to verify if state needs re-syncing or if an update is pending.
     */
    fun startPeriodicSync() {
        periodicJob?.cancel()
        periodicJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(300) // 300ms periodic sync check
                checkAndSyncState()
            }
        }
    }

    /**
     * Stops the periodic sync loop.
     */
    fun stopPeriodicSync() {
        periodicJob?.cancel()
        periodicJob = null
    }

    /**
     * Updates the current state. Triggers immediate transmission (On-Change) if state changed.
     */
    fun updateState(newState: LedState) {
        currentState = newState
        if (currentState != previousState) {
            triggerTransmission()
        }
    }

    /**
     * Checks if current state differs from previous state and triggers transmission if needed.
     */
    private fun checkAndSyncState() {
        val curr = currentState ?: return
        if (curr != previousState && !isTransmitting.get()) {
            triggerTransmission()
        }
    }

    /**
     * Triggers transmission of state data in chunked packets.
     */
    @Synchronized
    private fun triggerTransmission() {
        val stateToSend = currentState ?: return
        if (stateToSend.activeLedIds.isEmpty()) return

        sendJob?.cancel() // Cancel previous pending transmission

        sendJob = scope.launch(Dispatchers.IO) {
            if (!isTransmitting.compareAndSet(false, true)) return@launch

            try {
                transmitStateInChunks(stateToSend)
                previousState = stateToSend
            } finally {
                isTransmitting.set(false)
            }
        }
    }

    /**
     * Groups active LEDs into chunks of 5 and transmits each packet with a 15ms throttling delay.
     *
     * Example packet structure:
     * CHUNK,255,0,0,255,LED1,LED2,LED3,LED4,LED5\n
     */
    private suspend fun transmitStateInChunks(state: LedState) {
        // Chunking: group active LEDs into chunks of 5 (e.g. 57 LEDs -> 11 chunks of 5 + 1 remainder chunk of 2)
        val chunks = state.activeLedIds.chunked(5)

        for (chunk in chunks) {
            if (!currentCoroutineContext().isActive) break

            val idListString = chunk.joinToString(",")
            val packet = "CHUNK,${state.red},${state.green},${state.blue},${state.brightness},$idListString\n"

            // Transmit packet via Bluetooth
            sendDataFunc(packet)

            // Mandatory 15ms delay between consecutive transmissions to prevent Bluetooth buffer overflow
            delay(15)
        }
    }
}
