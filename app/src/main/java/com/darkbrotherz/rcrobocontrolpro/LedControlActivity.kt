package com.darkbrotherz.rcrobocontrolpro

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class LedControlActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_led_control)

        // ආපසු යාමේ බොත්තම (අලුත් Layout එක)
        val btnBackContainer = findViewById<LinearLayout>(R.id.btnBackContainer)
        btnBackContainer?.setOnClickListener {
            // මේකෙන් දැනට ඉන්න LED පිටුව වැහිලා ආයෙත් පරණ මෙනු එකට යනවා
            finish()
        }
    }
}