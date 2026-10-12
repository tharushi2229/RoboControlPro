package com.darkbrotherz.rcrobocontrolpro

import android.Manifest
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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

    // Bluetooth අවසර ඉල්ලන Popup එක සෑදීම
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

    // අයිකන් 5 සඳහා විචල්‍යයන්
    private lateinit var tabMainShop: ImageView
    private lateinit var tabMainCommunity: ImageView
    private lateinit var tabMainHome: ImageView
    private lateinit var tabMainLearning: ImageView
    private lateinit var tabMainJoystick: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestPermissions()

        val viewPager = findViewById<ViewPager2>(R.id.mainViewPager)
        tabMainShop = findViewById(R.id.tabMainShop)
        tabMainCommunity = findViewById(R.id.tabMainCommunity)
        tabMainHome = findViewById(R.id.tabMainHome)
        tabMainLearning = findViewById(R.id.tabMainLearning)
        tabMainJoystick = findViewById(R.id.tabMainJoystick)

        viewPager.isUserInputEnabled = false

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 5
            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> ShopFragment()
                    1 -> CommunityFragment()
                    2 -> HomeFragment()
                    3 -> LearningFragment()
                    4 -> JoystickFragment()
                    else -> Fragment()
                }
            }
        }

        tabMainShop.setOnClickListener { showComingSoon() }
        tabMainCommunity.setOnClickListener { showComingSoon() }
        tabMainHome.setOnClickListener { showComingSoon() }
        tabMainLearning.setOnClickListener { showComingSoon() }
        tabMainJoystick.setOnClickListener { viewPager.currentItem = 4 }

        // අයිකන් තාවකාලිකව අක්‍රීය කිරීම (Visual feedback)
        disableTab(tabMainShop)
        disableTab(tabMainCommunity)
        disableTab(tabMainHome)
        disableTab(tabMainLearning)

        // පිටුව මාරු වෙද්දී අදාළ අයිකනය කොළ පාට කිරීම සඳහා
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateTabUI(position)
            }
        })

        // ඇප් එක ඕපන් වෙද්දී Joystick (Game Controller) එක පෙන්වීම
        viewPager.currentItem = 4
    }

    private fun showComingSoon() {
        Toast.makeText(this, "Coming Soon in next update!", Toast.LENGTH_SHORT).show()
    }

    private fun disableTab(tab: ImageView) {
        tab.alpha = 0.4f
    }

    // තෝරන පිටුව අනුව අයිකන් වල පාට වෙනස් කරන ෆන්ක්ෂන් එක
    private fun updateTabUI(selectedPosition: Int) {

        val selectedTab = when (selectedPosition) {
            0 -> tabMainShop
            1 -> tabMainCommunity
            2 -> tabMainHome
            3 -> tabMainLearning
            4 -> tabMainJoystick
            else -> tabMainHome
        }

        val allTabs = arrayOf(tabMainShop, tabMainCommunity, tabMainHome, tabMainLearning, tabMainJoystick)

        for (tab in allTabs) {
            if (tab == selectedTab) {
                val activeBg = GradientDrawable()
                activeBg.shape = GradientDrawable.RECTANGLE
                activeBg.cornerRadius = 60f
                activeBg.setColor(Color.parseColor("#17C3B2"))
                // Add a green border (stroke) around the selected icon
                activeBg.setStroke(4, Color.parseColor("#00FF00"))

                tab.background = activeBg
                tab.setColorFilter(Color.parseColor("#000000"))
            } else {
                tab.background = null
                tab.setColorFilter(Color.parseColor("#80FFFFFF"))
            }

            // තාවකාලිකව අක්‍රීය කළ අයිකන සඳහා opacity අඩු කිරීම
            if (tab == tabMainShop || tab == tabMainCommunity || tab == tabMainHome || tab == tabMainLearning) {
                tab.alpha = 0.4f
            } else {
                tab.alpha = 1.0f
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        requestPermissionLauncher.launch(permissionsToRequest)
    }
}