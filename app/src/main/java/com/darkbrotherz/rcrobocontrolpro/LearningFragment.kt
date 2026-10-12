package com.darkbrotherz.rcrobocontrolpro

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class LearningFragment : Fragment() {

    private var learnMenuIcons: List<ImageView?> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_learning, container, false)

        val btnLearnLights = view.findViewById<ImageView>(R.id.btnLearnLights)
        val btnLearnVehicle = view.findViewById<ImageView>(R.id.btnLearnVehicle)
        val btnLearnRobot = view.findViewById<ImageView>(R.id.btnLearnRobot)
        val btnLearnTerminal = view.findViewById<ImageView>(R.id.btnLearnTerminal)

        learnMenuIcons = listOf(btnLearnLights, btnLearnVehicle, btnLearnRobot, btnLearnTerminal)

        btnLearnLights?.setOnClickListener { 
            selectLearnMenuIcon(btnLearnLights)
            openDetailPage("Pinned", "Welcome to Arduino Academy Community!", "Welcome everyone! This is a place to share projects, ask questions, and help fellow makers...", "by Moderator", "1 day ago")
        }
        btnLearnVehicle?.setOnClickListener { 
            selectLearnMenuIcon(btnLearnVehicle)
            openDetailPage("Solved", "Arduino Uno not responding - Need help!", "My Arduino Uno suddenly stopped working. The power LED is on but it won't upload code.", "by User", "6 hours ago")
        }
        btnLearnRobot?.setOnClickListener { 
            selectLearnMenuIcon(btnLearnRobot)
            openDetailPage("Robot", "Robot Controls coming soon!", "This section will contain all the instructions and code for building your own robot.", "by Dev Team", "Just now")
        }
        btnLearnTerminal?.setOnClickListener { 
            selectLearnMenuIcon(btnLearnTerminal)
            openDetailPage("Terminal", "Terminal commands explained", "Learn how to use the terminal to debug and control your Arduino in real-time.", "by Admin", "2 days ago")
        }

        return view
    }

    private fun selectLearnMenuIcon(sIcon: ImageView?) {
        learnMenuIcons.forEach { icon ->
            if (icon == sIcon) {
                val activeBg = GradientDrawable().apply {
                    setColor(Color.parseColor("#17C3B2"))
                    cornerRadius = 60f
                    setStroke(4, Color.parseColor("#00FF00"))
                }
                icon?.background = activeBg
                icon?.setColorFilter(Color.BLACK)
            } else {
                icon?.setBackgroundResource(android.R.color.transparent)
                icon?.setColorFilter(Color.WHITE)
            }
        }
    }

    private fun openDetailPage(tag: String, title: String, content: String, user: String, time: String) {
        val intent = Intent(requireContext(), InfoDetailActivity::class.java).apply {
            putExtra("TAG", tag)
            putExtra("TITLE", title)
            putExtra("CONTENT", content)
            putExtra("USER", user)
            putExtra("TIME", time)
        }
        startActivity(intent)
    }
}
