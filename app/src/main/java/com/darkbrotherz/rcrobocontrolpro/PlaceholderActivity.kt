package com.darkbrotherz.rcrobocontrolpro

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class PlaceholderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_placeholder)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}
