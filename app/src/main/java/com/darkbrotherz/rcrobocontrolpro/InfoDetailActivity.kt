package com.darkbrotherz.rcrobocontrolpro

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class InfoDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info_detail)

        val tag = intent.getStringExtra("TAG")
        val title = intent.getStringExtra("TITLE")
        val content = intent.getStringExtra("CONTENT")
        val user = intent.getStringExtra("USER")
        val time = intent.getStringExtra("TIME")

        findViewById<TextView>(R.id.txtDetailTag).text = tag
        findViewById<TextView>(R.id.txtDetailTitle).text = title
        findViewById<TextView>(R.id.txtDetailContent).text = content
        findViewById<TextView>(R.id.txtDetailUser).text = user
        findViewById<TextView>(R.id.txtDetailTime).text = time

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}
