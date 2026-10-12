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

class CarControlActivity : AppCompatActivity() {

    // Color constants for UI State
    private val colorActive = Color.parseColor("#17C3B2")  // Teal active state
    private val colorNormal = Color.parseColor("#A0A5AA")  // Default card color

    // Latching / Toggle States
    private var isParkingLightOn = false
    private var isHeadlightOn = false

    // Speed Controller State (0 to 255 PWM speed)
    private var currentSpeed = 255

    // Signal Flashing / Blinking Jobs and States
    private var isSignalLeftBlinking = false
    private var signalLeftJob: Job? = null

    private var isSignalRightBlinking = false
    private var signalRightJob: Job? = null

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_car_control)

        // Back Button
        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
            finish()
        }

        // Speed Controller Setup (SeekBar 0 - 255)
        val sliderSpeed = findViewById<SeekBar>(R.id.sliderSpeed)
        val txtSpeedValue = findViewById<TextView>(R.id.txtSpeedValue)

        sliderSpeed?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentSpeed = progress
                txtSpeedValue?.text = "SPEED: $currentSpeed"
                if (fromUser) {
                    BluetoothManager.sendData("CMD,SPEED,$currentSpeed\n")
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 1. Parking Light Button (Toggle / Latching)
        val btnParkingLight = findViewById<CardView>(R.id.btnParkingBrake)
        btnParkingLight?.setOnClickListener {
            isParkingLightOn = !isParkingLightOn
            if (isParkingLightOn) {
                btnParkingLight.setCardBackgroundColor(colorActive)
                BluetoothManager.sendData("CMD,PARKING,ON\n")
            } else {
                btnParkingLight.setCardBackgroundColor(colorNormal)
                BluetoothManager.sendData("CMD,PARKING,OFF\n")
            }
        }

        // 2. Headlight Button (Toggle / Latching)
        val btnHeadlights = findViewById<CardView>(R.id.btnHeadlights)
        btnHeadlights?.setOnClickListener {
            isHeadlightOn = !isHeadlightOn
            if (isHeadlightOn) {
                btnHeadlights.setCardBackgroundColor(colorActive)
                BluetoothManager.sendData("CMD,HEADLIGHT,ON\n")
            } else {
                btnHeadlights.setCardBackgroundColor(colorNormal)
                BluetoothManager.sendData("CMD,HEADLIGHT,OFF\n")
            }
        }

        // 3. Horn Button (Momentary / Hold-to-Press)
        val btnHorn = findViewById<CardView>(R.id.btnHorn)
        btnHorn?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    btnHorn.setCardBackgroundColor(colorActive)
                    btnHorn.scaleX = 0.95f
                    btnHorn.scaleY = 0.95f
                    BluetoothManager.sendData("CMD,HORN,ON\n")
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    btnHorn.setCardBackgroundColor(colorNormal)
                    btnHorn.scaleX = 1.0f
                    btnHorn.scaleY = 1.0f
                    BluetoothManager.sendData("CMD,HORN,OFF\n")
                    true
                }
                else -> false
            }
        }

        // 4. Signal Light Left / Beam Up (Flashing / Blinking with 500ms time gap)
        val btnSignalLeft = findViewById<CardView>(R.id.btnBeamUp)
        btnSignalLeft?.setOnClickListener {
            toggleLeftSignal(btnSignalLeft)
        }

        // 5. Signal Light Right / Beam Down (Flashing / Blinking with 500ms time gap)
        val btnSignalRight = findViewById<CardView>(R.id.btnBeamDown)
        btnSignalRight?.setOnClickListener {
            toggleRightSignal(btnSignalRight)
        }

        // 6. Tipper Dump Up & Down (Hold-to-Press)
        val btnDumpUp = findViewById<CardView>(R.id.btnDumpUp)
        setupHoldCardButton(btnDumpUp, "CMD,DUMP,UP\n", "CMD,DUMP,STOP\n")

        val btnDumpDown = findViewById<CardView>(R.id.btnDumpDown)
        setupHoldCardButton(btnDumpDown, "CMD,DUMP,DOWN\n", "CMD,DUMP,STOP\n")

        // 7. Differential Lock / Trailer Release Button (Toggle -> White when ON, Normal when OFF)
        val btnDiffLock = findViewById<CardView>(R.id.btnDiffLock)
        var isDiffLocked = false
        btnDiffLock?.setOnClickListener {
            isDiffLocked = !isDiffLocked
            if (isDiffLocked) {
                btnDiffLock.setCardBackgroundColor(Color.WHITE)
                BluetoothManager.sendData("CMD,DIFF,LOCK\n")
            } else {
                btnDiffLock.setCardBackgroundColor(colorNormal)
                BluetoothManager.sendData("CMD,DIFF,UNLOCK\n")
            }
        }

        // 8. Directional Drive & Steering Controls (Hold-to-Press including speed value)
        setupTouchButton(R.id.btnForward, { "CMD,DRIVE,FORWARD,$currentSpeed\n" }, "CMD,DRIVE,STOP\n")
        setupTouchButton(R.id.btnBackward, { "CMD,DRIVE,BACKWARD,$currentSpeed\n" }, "CMD,DRIVE,STOP\n")
        setupTouchButton(R.id.btnSteerLeft, { "CMD,STEER,LEFT,$currentSpeed\n" }, "CMD,STEER,CENTER\n")
        setupTouchButton(R.id.btnSteerRight, { "CMD,STEER,RIGHT,$currentSpeed\n" }, "CMD,STEER,CENTER\n")
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

    /**
     * Helper for CardView Hold-to-Press controls with visual press state
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupHoldCardButton(card: CardView?, pressCmd: String, releaseCmd: String) {
        card?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    card.setCardBackgroundColor(colorActive)
                    card.scaleX = 0.93f
                    card.scaleY = 0.93f
                    BluetoothManager.sendData(pressCmd)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    card.setCardBackgroundColor(colorNormal)
                    card.scaleX = 1.0f
                    card.scaleY = 1.0f
                    BluetoothManager.sendData(releaseCmd)
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Helper for Directional Touch Arrow Buttons (Independent Hold-to-Press with dynamic speed packet)
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchButton(viewId: Int, pressCmdProvider: () -> String, releaseCmd: String) {
        val view = findViewById<View>(viewId) ?: return
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.alpha = 0.5f         // Visual press feedback
                    v.scaleX = 0.90f
                    v.scaleY = 0.90f
                    BluetoothManager.sendData(pressCmdProvider())
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 1.0f         // Visual unpress feedback
                    v.scaleX = 1.0f
                    v.scaleY = 1.0f
                    BluetoothManager.sendData(releaseCmd)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        signalLeftJob?.cancel()
        signalRightJob?.cancel()
    }
}
