package com.darkbrotherz.rcrobocontrolpro

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2

class MainActivity : AppCompatActivity() {

    // Bluetooth සහ Location අවසර ඉල්ලන Popup එක සෑදීම
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var allGranted = true
        permissions.entries.forEach {
            if (!it.value) allGranted = false
        }

        if (allGranted) {
            Toast.makeText(this, "Bluetooth අවසර ලබා දෙන ලදී!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "ඇප් එක වැඩ කිරීමට Bluetooth අවසර අවශ්‍යයි!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ඇප් එක ඕපන් වෙද්දීම අවසර ඉල්ලන ෆන්ක්ෂන් එකට කතා කිරීම
        checkAndRequestPermissions()

        val viewPager = findViewById<ViewPager2>(R.id.mainViewPager)
        val tabMainJoystick = findViewById<ImageView>(R.id.tabMainJoystick)
        val tabMainLearning = findViewById<ImageView>(R.id.tabMainLearning)
        val tabMainHome = findViewById<ImageView>(R.id.tabMainHome)
        val tabMainCommunity = findViewById<ImageView>(R.id.tabMainCommunity)
        val tabMainShop = findViewById<ImageView>(R.id.tabMainShop)

        viewPager.isUserInputEnabled = false

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return 5
            }

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> Fragment()
                    1 -> Fragment()
                    2 -> JoystickFragment() // අපේ Joystick පිටුව
                    3 -> Fragment()
                    4 -> Fragment()
                    else -> Fragment()
                }
            }
        }

        tabMainJoystick.setOnClickListener { viewPager.currentItem = 0 }
        tabMainLearning.setOnClickListener { viewPager.currentItem = 1 }
        tabMainHome.setOnClickListener { viewPager.currentItem = 2 }
        tabMainCommunity.setOnClickListener { viewPager.currentItem = 3 }
        tabMainShop.setOnClickListener { viewPager.currentItem = 4 }

        viewPager.currentItem = 2
    }

    // ෆෝන් එකේ Android වර්ෂන් එක බලලා හරියටම ඕනේ කරන අවසර ටික ඉල්ලීම
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 සහ ඊට අලුත් ෆෝන් සඳහා
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            // පරණ ෆෝන් සඳහා
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

        // Popup එක තිරය මත පෙන්වීම
        requestPermissionLauncher.launch(permissionsToRequest)
    }
}