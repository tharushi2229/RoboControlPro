package com.darkbrotherz.rcrobocontrolpro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HomeFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val rv = view.findViewById<RecyclerView>(R.id.homeRecyclerView)
        rv.layoutManager = LinearLayoutManager(requireContext())

        val sampleList = listOf(
            HomeFeedItem(
                "uploader name",
                "Description of the uploaded video according to the lesson conducted",
                "1 day ago",
                "void setup() {\n  pinMode(13, OUTPUT);\n}\nvoid loop() {\n  digitalWrite(13, HIGH);\n}"
            ),
            HomeFeedItem(
                "uploader name",
                "Description of the uploaded video according to the lesson conducted",
                "1 day ago",
                "void setup() {\n  Serial.begin(9600);\n}\nvoid loop() {\n  Serial.println(\"Hello World\");\n}"
            ),
            HomeFeedItem(
                "uploader name",
                "Description of the uploaded video according to the lesson conducted",
                "1 day ago",
                "void setup() {\n  pinMode(LED_BUILTIN, OUTPUT);\n}\nvoid loop() {\n  digitalWrite(LED_BUILTIN, !digitalRead(LED_BUILTIN));\n}"
            )
        )

        rv.adapter = HomeAdapter(sampleList)

        return view
    }
}
