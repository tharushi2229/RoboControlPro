package com.darkbrotherz.rcrobocontrolpro

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Production-ready Truck Controller Activity with Master Packet consolidation,
 * Touch/Click state management, and Smart Bluetooth Transmission.
 */
class TruckControlActivity : AppCompatActivity() {

    // UI State Color Constants
    private val colorActive = Color.parseColor("#17C3B2")  // Teal active state
    private val colorNormal = Color.parseColor("#A0A5AA")  // Default card color

    // Master Packet State Variables
    // Format: CMD,<Drive>,<Speed>,<Steering>,<Headlight>,<Parking>,<Horn>,<TrailerUp>,<TrailerDown>\n
    private var driveState = "S"         // F: Forward, B: Backward, S: Stop
    private var currentSpeed = 255       // 0 to 255 PWM Speed
    private var steeringState = "C"      // L: Left, R: Right, C: Center
    private var headlightState = "0"     // 1: ON, 0: OFF
    private var parkingState = "0"       // 1: ON, 0: OFF
    private var hornState = "0"          // 1: ON, 0: OFF
    private var trailerUpState = "0"     // 1: ON, 0: OFF
    private var trailerDownState = "0"   // 1: ON, 0: OFF

    // Smart State Tracking to prevent redundant Bluetooth transmissions
    @Volatile
    private var previousMasterPacket = ""

    // Latching / Toggle Boolean Flags
    private var isParkingLightOn = false
    private var isHeadlightOn = false
    private var isDiffLocked = false

    // Signal Flashing / Blinking Coroutine Jobs
    private var isSignalLeftBlinking = false
    private var signalLeftJob: Job? = null

    private var isSignalRightBlinking = false
    private var signalRightJob: Job? = null

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_truck_control)

        // Back Button
        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
            finish()
        }

        // 1. Speed Slider Controller (0 - 255 PWM range)
        val sliderSpeed = findViewById<SeekBar>(R.id.sliderSpeed)
        val txtSpeedValue = findViewById<TextView>(R.id.txtSpeedValue)

        sliderSpeed?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentSpeed = progress
                txtSpeedValue?.text = "SPEED: $currentSpeed"
                updateAndSendMasterPacket()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 2. Headlight Button (Toggle / Latching - Tap to Activate / Tap to OFF)
        val btnHeadlights = findViewById<CardView>(R.id.btnHeadlights)
        btnHeadlights?.setOnClickListener {
            isHeadlightOn = !isHeadlightOn
            headlightState = if (isHeadlightOn) "1" else "0"
            btnHeadlights.setCardBackgroundColor(if (isHeadlightOn) Color.WHITE else colorNormal)
            updateAndSendMasterPacket()
        }

        // 3. Parking Light Button (Toggle / Latching - Tap to Activate / Tap to OFF)
        val btnParkingLight = findViewById<CardView>(R.id.btnParkingBrake)
        btnParkingLight?.setOnClickListener {
            isParkingLightOn = !isParkingLightOn
            parkingState = if (isParkingLightOn) "1" else "0"
            btnParkingLight.setCardBackgroundColor(if (isParkingLightOn) Color.WHITE else colorNormal)
            updateAndSendMasterPacket()
        }

        // 4. Horn Button (Momentary / Hold-to-Press: ACTION_DOWN & ACTION_UP)
        val btnHorn = findViewById<CardView>(R.id.btnHorn)
        btnHorn?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    btnHorn.setCardBackgroundColor(colorActive)
                    btnHorn.scaleX = 0.95f
                    btnHorn.scaleY = 0.95f
                    hornState = "1"
                    updateAndSendMasterPacket()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    btnHorn.setCardBackgroundColor(colorNormal)
                    btnHorn.scaleX = 1.0f
                    btnHorn.scaleY = 1.0f
                    hornState = "0"
                    updateAndSendMasterPacket()
                    true
                }
                else -> false
            }
        }

        // 5. Trailer Up Control (Dump Up - Hold-to-Press)
        val btnDumpUp = findViewById<CardView>(R.id.btnDumpUp)
        btnDumpUp?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    btnDumpUp.setCardBackgroundColor(colorActive)
                    btnDumpUp.scaleX = 0.93f
                    btnDumpUp.scaleY = 0.93f
                    trailerUpState = "1"
                    updateAndSendMasterPacket()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    btnDumpUp.setCardBackgroundColor(colorNormal)
                    btnDumpUp.scaleX = 1.0f
                    btnDumpUp.scaleY = 1.0f
                    trailerUpState = "0"
                    updateAndSendMasterPacket()
                    true
                }
                else -> false
            }
        }

        // 6. Trailer Down Control (Dump Down - Hold-to-Press)
        val btnDumpDown = findViewById<CardView>(R.id.btnDumpDown)
        btnDumpDown?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    btnDumpDown.setCardBackgroundColor(colorActive)
                    btnDumpDown.scaleX = 0.93f
                    btnDumpDown.scaleY = 0.93f
                    trailerDownState = "1"
                    updateAndSendMasterPacket()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    btnDumpDown.setCardBackgroundColor(colorNormal)
                    btnDumpDown.scaleX = 1.0f
                    btnDumpDown.scaleY = 1.0f
                    trailerDownState = "0"
                    updateAndSendMasterPacket()
                    true
                }
                else -> false
            }
        }

        // 7. Differential Lock / Trailer Release Button (Toggle -> White when ON)
        val btnDiffLock = findViewById<CardView>(R.id.btnDiffLock)
        btnDiffLock?.setOnClickListener {
            isDiffLocked = !isDiffLocked
            btnDiffLock.setCardBackgroundColor(if (isDiffLocked) Color.WHITE else colorNormal)
            updateAndSendMasterPacket()
        }

        // 8. Drive Up & Down Arrow Buttons (Hold-to-Press with Auto-Stop)
        val btnForward = findViewById<View>(R.id.btnForward)
        btnForward?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.alpha = 0.5f
                    v.scaleX = 0.90f
                    v.scaleY = 0.90f
                    driveState = "F"
                    updateAndSendMasterPacket()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 1.0f
                    v.scaleX = 1.0f
                    v.scaleY = 1.0f
                    driveState = "S"
                    updateAndSendMasterPacket()
                    true
                }
                else -> false
            }
        }

        val btnBackward = findViewById<View>(R.id.btnBackward)
        btnBackward?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.alpha = 0.5f
                    v.scaleX = 0.90f
                    v.scaleY = 0.90f
                    driveState = "B"
                    updateAndSendMasterPacket()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 1.0f
                    v.scaleX = 1.0f
                    v.scaleY = 1.0f
                    driveState = "S"
                    updateAndSendMasterPacket()
                    true
                }
                else -> false
            }
        }

        // 9. Steering Left & Right Arrow Buttons (Hold-to-Press with Auto-Center)
        val btnSteerLeft = findViewById<View>(R.id.btnSteerLeft)
        btnSteerLeft?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.alpha = 0.5f
                    v.scaleX = 0.90f
                    v.scaleY = 0.90f
                    steeringState = "L"
                    updateAndSendMasterPacket()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 1.0f
                    v.scaleX = 1.0f
                    v.scaleY = 1.0f
                    steeringState = "C"
                    updateAndSendMasterPacket()
                    true
                }
                else -> false
            }
        }

        val btnSteerRight = findViewById<View>(R.id.btnSteerRight)
        btnSteerRight?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.alpha = 0.5f
                    v.scaleX = 0.90f
                    v.scaleY = 0.90f
                    steeringState = "R"
                    updateAndSendMasterPacket()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 1.0f
                    v.scaleX = 1.0f
                    v.scaleY = 1.0f
                    steeringState = "C"
                    updateAndSendMasterPacket()
                    true
                }
                else -> false
            }
        }

        // 10. Signal Light Left / Beam Up (Flashing / Blinking with 500ms Interval)
        val btnSignalLeft = findViewById<CardView>(R.id.btnBeamUp)
        btnSignalLeft?.setOnClickListener {
            toggleLeftSignal(btnSignalLeft)
        }

        // 11. Signal Light Right / Beam Down (Flashing / Blinking with 500ms Interval)
        val btnSignalRight = findViewById<CardView>(R.id.btnBeamDown)
        btnSignalRight?.setOnClickListener {
            toggleRightSignal(btnSignalRight)
        }

        // Send initial master packet snapshot
        updateAndSendMasterPacket()
    }

    /**
     * Consolidates all vehicle states into a single master packet string and transmits over Bluetooth.
     * Format: CMD,<Drive>,<Speed>,<Steering>,<Headlight>,<Parking>,<Horn>,<TrailerUp>,<TrailerDown>\n
     */
    private fun updateAndSendMasterPacket() {
        val currentPacket = "CMD,$driveState,$currentSpeed,$steeringState,$headlightState,$parkingState,$hornState,$trailerUpState,$trailerDownState\n"

        // Smart State Tracking: Send immediately on state change, ignore redundant transmissions
        if (currentPacket != previousMasterPacket) {
            previousMasterPacket = currentPacket
            BluetoothManager.sendData(currentPacket)
        }
    }

    /**
     * Toggles Left Signal Blinking (500ms ON / 500ms OFF loop)
     */
    private fun toggleLeftSignal(btn: CardView) {
        isSignalLeftBlinking = !isSignalLeftBlinking

        if (isSignalLeftBlinking) {
            signalLeftJob?.cancel()
            signalLeftJob = lifecycleScope.launch {
                while (isSignalLeftBlinking) {
                    btn.setCardBackgroundColor(colorActive)
                    BluetoothManager.sendData("CMD,SIGNAL,LEFT_ON\n")
                    delay(500)

                    btn.setCardBackgroundColor(colorNormal)
                    BluetoothManager.sendData("CMD,SIGNAL,LEFT_OFF\n")
                    delay(500)
                }
            }
        } else {
            signalLeftJob?.cancel()
            btn.setCardBackgroundColor(colorNormal)
            BluetoothManager.sendData("CMD,SIGNAL,LEFT_OFF\n")
        }
    }

    /**
     * Toggles Right Signal Blinking (500ms ON / 500ms OFF loop)
     */
    private fun toggleRightSignal(btn: CardView) {
        isSignalRightBlinking = !isSignalRightBlinking

        if (isSignalRightBlinking) {
            signalRightJob?.cancel()
            signalRightJob = lifecycleScope.launch {
                while (isSignalRightBlinking) {
                    btn.setCardBackgroundColor(colorActive)
                    BluetoothManager.sendData("CMD,SIGNAL,RIGHT_ON\n")
                    delay(500)

                    btn.setCardBackgroundColor(colorNormal)
                    BluetoothManager.sendData("CMD,SIGNAL,RIGHT_OFF\n")
                    delay(500)
                }
            }
        } else {
            signalRightJob?.cancel()
            btn.setCardBackgroundColor(colorNormal)
            BluetoothManager.sendData("CMD,SIGNAL,RIGHT_OFF\n")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        signalLeftJob?.cancel()
        signalRightJob?.cancel()
    }
}
