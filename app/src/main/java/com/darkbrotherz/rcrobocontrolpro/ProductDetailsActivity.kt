package com.darkbrotherz.rcrobocontrolpro

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class ProductDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_details)

        // ShopFragment එකෙන් එවපු දත්ත ලබා ගැනීම
        val name = intent.getStringExtra("NAME") ?: "No Name"
        val price = intent.getStringExtra("PRICE") ?: "0.00"
        val imageUrl = intent.getStringExtra("IMAGE_URL")
        val description = intent.getStringExtra("DESCRIPTION") ?: "High quality component for your Arduino projects."

        // UI එකට දත්ත ඇතුළත් කිරීම
        findViewById<TextView>(R.id.txtBigName).text = name
        findViewById<TextView>(R.id.txtBigPrice).text = price
        findViewById<TextView>(R.id.txtBigDescription).text = description

        val imgBig = findViewById<ImageView>(R.id.imgBigProduct)
        val imgSub1 = findViewById<ImageView>(R.id.imgSub1)
        val imgSub2 = findViewById<ImageView>(R.id.imgSub2)
        
        if (!imageUrl.isNullOrEmpty()) {
            val fullImageUrl = if (imageUrl.startsWith("http")) {
                imageUrl
            } else {
                "https://discount-purposely-rejoin.ngrok-free.dev/ceylon_shop_db/" + imageUrl
            }

            android.util.Log.d("DETAILS_IMAGE", "Loading: $fullImageUrl")

            Glide.with(this)
                .load(fullImageUrl)
                .fitCenter()
                .into(imgBig)

            Glide.with(this)
                .load(fullImageUrl)
                .centerCrop()
                .into(imgSub1)

            Glide.with(this)
                .load(fullImageUrl)
                .centerCrop()
                .into(imgSub2)
        } else {
            imgBig.setImageDrawable(null)
            imgSub1.setImageDrawable(null)
            imgSub2.setImageDrawable(null)
        }

        // Back button function
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Add to Cart බොත්තම වැඩ කරන විදිහ
        findViewById<Button>(R.id.btnBigAddToCart).setOnClickListener {
            Toast.makeText(this, "$name Cart එකට එකතු කළා!", Toast.LENGTH_SHORT).show()
        }
    }
}
