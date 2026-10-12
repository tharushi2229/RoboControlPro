package com.darkbrotherz.rcrobocontrolpro

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Collections
import java.util.UUID

interface OnStartDragListener {
    fun onStartDrag(viewHolder: RecyclerView.ViewHolder)
}

class JoystickFragment : Fragment(), OnStartDragListener {

    private var bluetoothAdapter: BluetoothAdapter? = null

    // UI Elements
    private var menuIcons: List<ImageView?> = emptyList()
    private var contentContainers: List<LinearLayout?> = emptyList()
    private lateinit var txtTerminalLog: TextView
    private lateinit var terminalScrollView: ScrollView

    // LED & Projects
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var ledSyncManager: LedSyncManager
    private val projectList = mutableListOf<ProjectItem>()
    private lateinit var projectAdapter: ProjectAdapter
    private lateinit var ledAdapter: LedAdapter
    private var projectCountTracker = 0
    private var ledCountTracker = 0
    private var currentOpenedProjectIndex = -1

    private lateinit var projectListContainer: LinearLayout
    private lateinit var projectDetailsContainer: LinearLayout
    private lateinit var txtCurrentProjectTitle: TextView
    private lateinit var colorPreview: View
    private lateinit var ledRecyclerView: RecyclerView
    private lateinit var projectTouchHelper: ItemTouchHelper
    private lateinit var ledTouchHelper: ItemTouchHelper

    private var selectedRed = 255
    private var selectedGreen = 255
    private var selectedBlue = 255
    private var currentBrightness = 255

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_joystick, container, false)
        sharedPreferences = requireContext().getSharedPreferences("RoboControlAutoSave", Context.MODE_PRIVATE)

        ledSyncManager = LedSyncManager(lifecycleScope) { packet ->
            BluetoothManager.sendData(packet)
        }
        ledSyncManager.startPeriodicSync()

        val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as? AndroidBluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter

        // Start background Bluetooth server listener (allows Phone-to-Phone connections)
        BluetoothManager.startServerListener(lifecycleScope) { deviceName ->
            Toast.makeText(requireContext(), "Connected to $deviceName", Toast.LENGTH_SHORT).show()
            startBluetoothReader()
        }

        // UI Setup
        val btnLights = view.findViewById<ImageView>(R.id.btnLights)
        val btnVehicle = view.findViewById<ImageView>(R.id.btnVehicle)
        val btnRobot = view.findViewById<ImageView>(R.id.btnRobot)
        val btnTerminal = view.findViewById<ImageView>(R.id.btnTerminal)

        val ledContainer = view.findViewById<LinearLayout>(R.id.ledContainer)
        val vehicleContainer = view.findViewById<LinearLayout>(R.id.vehicleContainer)
        val botContainer = view.findViewById<LinearLayout>(R.id.botContainer)
        val terminalContainer = view.findViewById<LinearLayout>(R.id.terminalContainer)

        menuIcons = listOf(btnLights, btnVehicle, btnRobot, btnTerminal)
        contentContainers = listOf(ledContainer, vehicleContainer, botContainer, terminalContainer)

        // Terminal setup
        txtTerminalLog = view.findViewById(R.id.txtTerminalLog)
        terminalScrollView = view.findViewById(R.id.terminalScrollView)
        val etTerminalInput = view.findViewById<EditText>(R.id.etTerminalInput)
        view.findViewById<ImageView>(R.id.btnTerminalSend)?.setOnClickListener {
            val command = etTerminalInput?.text.toString()?.trim() ?: ""
            if (command.isNotEmpty()) {
                BluetoothManager.sendData("$command\n")
                appendTerminalMessage("> $command")
                etTerminalInput?.setText("")
            }
        }
        view.findViewById<ImageView>(R.id.btnClear)?.setOnClickListener { txtTerminalLog.text = "█" }

        // Bluetooth Setup
        view.findViewById<Button>(R.id.btnConnected)?.setOnClickListener { showBluetoothDevicesDialog() }

        // LED Setup
        projectListContainer = view.findViewById(R.id.projectListContainer)
        projectDetailsContainer = view.findViewById(R.id.projectDetailsContainer)
        val projectRecyclerView = view.findViewById<RecyclerView>(R.id.projectRecyclerView)
        txtCurrentProjectTitle = view.findViewById(R.id.txtCurrentProjectTitle)
        ledRecyclerView = view.findViewById(R.id.ledRecyclerView)
        colorPreview = view.findViewById(R.id.colorPreview)

        loadProjectsData()

        projectAdapter = ProjectAdapter(projectList, { projectTouchHelper.startDrag(it) }, { saveProjectsData() }, { openProjectDetails(it) }, { showRenameProjectDialog(it) })
        projectRecyclerView?.layoutManager = LinearLayoutManager(requireContext())
        projectRecyclerView?.adapter = projectAdapter
        projectTouchHelper = ItemTouchHelper(createDragCallback(projectList, projectAdapter))
        projectTouchHelper.attachToRecyclerView(projectRecyclerView)

        ledAdapter = LedAdapter(mutableListOf(), { ledTouchHelper.startDrag(it) }, { saveProjectsData() }, { showRenameLedDialog(it) })
        ledRecyclerView?.layoutManager = LinearLayoutManager(requireContext())
        ledRecyclerView?.adapter = ledAdapter
        ledTouchHelper = ItemTouchHelper(createDragCallbackForLed(ledAdapter))
        ledTouchHelper.attachToRecyclerView(ledRecyclerView)

        // Project Plus/Delete Actions
        view.findViewById<ImageView>(R.id.btnAddProject)?.setOnClickListener {
            projectList.add(ProjectItem("Project ${projectList.size + 1}"))
            projectAdapter.notifyItemInserted(projectList.size - 1)
            saveProjectsData()
        }

        view.findViewById<ImageView>(R.id.btnDeleteProject)?.setOnClickListener {
            val iterator = projectList.iterator()
            var changed = false
            while (iterator.hasNext()) {
                if (iterator.next().isSelected) {
                    iterator.remove()
                    changed = true
                }
            }
            if (changed) {
                projectAdapter.notifyDataSetChanged()
                saveProjectsData()
            } else {
                Toast.makeText(requireContext(), "Select projects to delete", Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<ImageView>(R.id.btnProjectGuide)?.setOnClickListener {
            startActivity(Intent(requireContext(), CodeGuideActivity::class.java))
        }

        // LED Plus/Delete Actions
        view.findViewById<ImageView>(R.id.btnAddLed)?.setOnClickListener {
            if (currentOpenedProjectIndex != -1) {
                val list = projectList[currentOpenedProjectIndex].ledList
                if (list.size < 60) {
                    val nextNum = list.size + 1
                    val shortId = "LED$nextNum"
                    list.add(LedItem("LED $nextNum", shortId = shortId))
                    ledAdapter.notifyItemInserted(list.size - 1)
                    saveProjectsData()
                } else {
                    Toast.makeText(requireContext(), "එක් ව්‍යාපෘතියකට එක් කළ හැක්කේ උපරිම LED 60ක් පමණි!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        view.findViewById<ImageView>(R.id.btnDeleteLed)?.setOnClickListener {
            if (currentOpenedProjectIndex != -1) {
                val list = projectList[currentOpenedProjectIndex].ledList
                val iterator = list.iterator()
                var changed = false
                while (iterator.hasNext()) {
                    if (iterator.next().isSelected) {
                        iterator.remove()
                        changed = true
                    }
                }
                if (changed) {
                    ledAdapter.notifyDataSetChanged()
                    saveProjectsData()
                } else {
                    Toast.makeText(requireContext(), "Select LEDs to delete", Toast.LENGTH_SHORT).show()
                }
            }
        }

        view.findViewById<ImageView>(R.id.btnLedGuide)?.setOnClickListener {
            startActivity(Intent(requireContext(), CodeGuideActivity::class.java))
        }

        view.findViewById<ImageView>(R.id.btnProjectGuide)?.setOnClickListener {
            startActivity(Intent(requireContext(), CodeGuideActivity::class.java))
        }

        view.findViewById<LinearLayout>(R.id.btnBackToProjects)?.setOnClickListener {
            projectListContainer.visibility = View.VISIBLE
            projectDetailsContainer.visibility = View.GONE
            currentOpenedProjectIndex = -1
        }

        // Menu Listeners
        btnLights?.setOnClickListener { selectMenuIcon(btnLights); showContainer(ledContainer) }
        btnVehicle?.setOnClickListener { selectMenuIcon(btnVehicle); showContainer(vehicleContainer) }
        btnRobot?.setOnClickListener { selectMenuIcon(btnRobot); showContainer(botContainer) }
        btnTerminal?.setOnClickListener { selectMenuIcon(btnTerminal); showContainer(terminalContainer) }

        // Vehicle Select Listeners
        view.findViewById<CardView>(R.id.cardTruck)?.setOnClickListener {
            startActivity(Intent(requireContext(), TruckControlActivity::class.java))
        }
        view.findViewById<CardView>(R.id.cardCar)?.setOnClickListener {
            startActivity(Intent(requireContext(), CarControlActivity::class.java))
        }

        // Color Wheel and Slider Listeners
        val imgColorWheel = view.findViewById<ImageView>(R.id.imgColorWheel)
        val sliderBrightness = view.findViewById<SeekBar>(R.id.sliderBrightness)
        val switchRgb = view.findViewById<SwitchCompat>(R.id.switchRgb)

        imgColorWheel?.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                val bitmap = (imgColorWheel.drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val x = event.x.toInt().coerceIn(0, bitmap.width - 1)
                    val y = event.y.toInt().coerceIn(0, bitmap.height - 1)
                    val pixel = bitmap.getPixel(x, y)
                    if (Color.alpha(pixel) > 0) {
                        selectedRed = Color.red(pixel)
                        selectedGreen = Color.green(pixel)
                        selectedBlue = Color.blue(pixel)
                        colorPreview.setBackgroundColor(pixel)
                        if (switchRgb?.isChecked == true) sendBatchLedData()
                    }
                }
            }
            true
        }

        sliderBrightness?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentBrightness = progress
                if (switchRgb?.isChecked == true && fromUser) sendBatchLedData()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Set default view
        selectMenuIcon(btnLights)
        showContainer(ledContainer)

        return view
    }

    private fun selectMenuIcon(sIcon: ImageView?) {
        menuIcons.forEach { icon ->
            if (icon == sIcon) {
                val activeBg = GradientDrawable().apply {
                    setColor(Color.parseColor("#17C3B2"))
                    cornerRadius = 24f
                }
                icon?.background = activeBg
                icon?.setColorFilter(Color.BLACK)
            } else {
                icon?.setBackgroundResource(android.R.color.transparent)
                icon?.setColorFilter(Color.WHITE)
            }
        }
    }
    private fun showContainer(cToShow: LinearLayout?) { contentContainers.forEach { it?.visibility = if(it == cToShow) View.VISIBLE else View.GONE } }
    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {}

    /**
     * Implementing robust batched sequential sending logic for WS2812B LEDs
     */
    private fun sendBatchLedData() {
        if (currentOpenedProjectIndex == -1) return

        val leds = projectList[currentOpenedProjectIndex].ledList
        val selectedLeds = leds.filter { it.isSelected }
        val targetList = if (selectedLeds.isNotEmpty()) selectedLeds else leds.filter { it.isSwitchedOn }

        val activeIds = targetList.mapIndexed { index, led ->
            if (led.shortId.isNotEmpty()) led.shortId else "LED${leds.indexOf(led) + 1}"
        }

        if (activeIds.isEmpty()) return

        val newState = LedState(
            red = selectedRed,
            green = selectedGreen,
            blue = selectedBlue,
            brightness = currentBrightness,
            activeLedIds = activeIds
        )

        ledSyncManager.updateState(newState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::ledSyncManager.isInitialized) {
            ledSyncManager.stopPeriodicSync()
        }
    }

    @SuppressLint("MissingPermission")
    private fun showBluetoothDevicesDialog() {
        val d = BottomSheetDialog(requireContext())
        val v = layoutInflater.inflate(R.layout.bottom_sheet_bluetooth, null)
        val rv = v.findViewById<RecyclerView>(R.id.rvBluetoothDevices)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = BluetoothDeviceAdapter(bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()) { device ->
            d.dismiss()
            Toast.makeText(requireContext(), "Connecting to ${device.name}...", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch(Dispatchers.IO) {
                val isConnected = BluetoothManager.connectToDevice(device)
                withContext(Dispatchers.Main) {
                    if (isConnected) {
                        Toast.makeText(requireContext(), "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
                        startBluetoothReader()
                    } else {
                        Toast.makeText(requireContext(), "Connection failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        d.setContentView(v)
        d.show()
    }

    private var terminalReadJob: Job? = null

    /**
     * Continuously reads incoming messages from Bluetooth InputStream in background IO thread
     * and safely updates the main UI thread.
     */
    private fun startBluetoothReader() {
        terminalReadJob?.cancel()
        terminalReadJob = lifecycleScope.launch(Dispatchers.IO) {
            val socket = BluetoothManager.bluetoothSocket ?: return@launch
            val buffer = ByteArray(1024)

            try {
                val inputStream = socket.inputStream
                while (isActive && socket.isConnected) {
                    val bytes = inputStream.read(buffer)
                    if (bytes > 0) {
                        val incomingText = String(buffer, 0, bytes)
                        withContext(Dispatchers.Main) {
                            appendTerminalMessage("< $incomingText")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Appends terminal log messages and auto-scrolls to bottom.
     */
    private fun appendTerminalMessage(msg: String) {
        val currentLog = txtTerminalLog.text.toString().replace("█", "").trim()
        val newLog = if (currentLog.isEmpty()) msg else "$currentLog\n$msg"
        txtTerminalLog.text = "$newLog █"
        terminalScrollView.post {
            terminalScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun showRenameProjectDialog(index: Int) {
        val et = EditText(requireContext())
        et.setText(projectList[index].name)
        AlertDialog.Builder(requireContext())
            .setTitle("Rename Project")
            .setView(et)
            .setPositiveButton("OK") { _, _ ->
                projectList[index].name = et.text.toString()
                projectAdapter.notifyItemChanged(index)
                saveProjectsData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRenameLedDialog(index: Int) {
        if (currentOpenedProjectIndex == -1) return
        val item = projectList[currentOpenedProjectIndex].ledList[index]

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 30, 60, 10)
        }

        // Label for LED Name
        val lblName = TextView(requireContext()).apply {
            text = "LED Name:"
            textSize = 14f
            setTextColor(Color.parseColor("#17C3B2"))
            setPadding(0, 0, 0, 8)
        }

        val etName = EditText(requireContext()).apply {
            setText(item.name)
            setSelection(item.name.length)
        }

        // Label for Short ID
        val lblShortId = TextView(requireContext()).apply {
            text = "Short ID (Signal ID):"
            textSize = 14f
            setTextColor(Color.parseColor("#17C3B2"))
            setPadding(0, 24, 0, 8)
        }

        val currentId = if (item.shortId.isNotEmpty()) item.shortId else "LED${index + 1}"
        val etShortId = EditText(requireContext()).apply {
            setText(currentId)
            setSelection(currentId.length)
        }

        layout.addView(lblName)
        layout.addView(etName)
        layout.addView(lblShortId)
        layout.addView(etShortId)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit LED Details")
            .setView(layout)
            .setPositiveButton("OK") { _, _ ->
                val newName = etName.text.toString().trim()
                val newShortId = etShortId.text.toString().trim()
                if (newName.isNotEmpty()) item.name = newName
                if (newShortId.isNotEmpty()) item.shortId = newShortId
                ledAdapter.notifyItemChanged(index)
                saveProjectsData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openProjectDetails(index: Int) {
        currentOpenedProjectIndex = index
        val project = projectList[index]
        txtCurrentProjectTitle.text = project.name
        ledAdapter.updateList(project.ledList)
        projectListContainer.visibility = View.GONE
        projectDetailsContainer.visibility = View.VISIBLE
    }

    private fun loadProjectsData() {
        val jsonString = sharedPreferences.getString("projects_json", null)
        if (jsonString != null) {
            try {
                projectList.clear()
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val projectJson = jsonArray.getJSONObject(i)
                    val project = ProjectItem(
                        projectJson.getString("name"),
                        rgbColor = projectJson.optInt("rgbColor", Color.WHITE)
                    )
                    val ledArray = projectJson.getJSONArray("ledList")
                    for (j in 0 until ledArray.length()) {
                        val ledJson = ledArray.getJSONObject(j)
                        val savedShortId = ledJson.optString("shortId", "")
                        val shortId = if (savedShortId.isNotEmpty()) savedShortId else "LED${j + 1}"
                        project.ledList.add(LedItem(
                            name = ledJson.getString("name"),
                            isSwitchedOn = ledJson.optBoolean("isSwitchedOn", false),
                            shortId = shortId
                        ))
                    }
                    projectList.add(project)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveProjectsData() {
        try {
            val jsonArray = JSONArray()
            for (project in projectList) {
                val projectJson = JSONObject()
                projectJson.put("name", project.name)
                projectJson.put("rgbColor", project.rgbColor)
                val ledArray = JSONArray()
                for (led in project.ledList) {
                    val ledJson = JSONObject()
                    ledJson.put("name", led.name)
                    ledJson.put("isSwitchedOn", led.isSwitchedOn)
                    val shortId = if (led.shortId.isNotEmpty()) led.shortId else "LED${project.ledList.indexOf(led) + 1}"
                    ledJson.put("shortId", shortId)
                    ledArray.put(ledJson)
                }
                projectJson.put("ledList", ledArray)
                jsonArray.put(projectJson)
            }
            sharedPreferences.edit().putString("projects_json", jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun createDragCallback(l: MutableList<ProjectItem>, a: RecyclerView.Adapter<*>) = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
        override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean { Collections.swap(l, v.adapterPosition, t.adapterPosition); a.notifyItemMoved(v.adapterPosition, t.adapterPosition); return true }
        override fun onSwiped(v: RecyclerView.ViewHolder, d: Int) {}
    }
    private fun createDragCallbackForLed(a: LedAdapter) = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
        override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean { val list = projectList[currentOpenedProjectIndex].ledList; Collections.swap(list, v.adapterPosition, t.adapterPosition); a.notifyItemMoved(v.adapterPosition, t.adapterPosition); return true }
        override fun onSwiped(v: RecyclerView.ViewHolder, d: Int) {}
    }
}

// Data Classes
data class LedItem(var name: String, var isSelected: Boolean = false, var isSwitchedOn: Boolean = false, var shortId: String = "")
data class ProjectItem(var name: String, var isSelected: Boolean = false, var ledList: MutableList<LedItem> = mutableListOf(), var rgbColor: Int = Color.WHITE)

// Adapters
class ProjectAdapter(val list: MutableList<ProjectItem>, val onDragStart: (RecyclerView.ViewHolder) -> Unit, val onDataChanged: () -> Unit, val onProjectClick: (Int) -> Unit, val onProjectLongClick: (Int) -> Unit) : RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder>() {
    inner class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txt: TextView = itemView.findViewById(R.id.txtProjectName)
        val chk: CheckBox = itemView.findViewById(R.id.chkProjectSelect)
        val drag: ImageView = itemView.findViewById(R.id.imgProjectDragHandle)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ProjectViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false))
    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val item = list[position]
        holder.txt.text = item.name
        holder.chk.setOnCheckedChangeListener(null)
        holder.chk.isChecked = item.isSelected
        holder.chk.setOnCheckedChangeListener { _, isChecked -> item.isSelected = isChecked }
        holder.itemView.setOnClickListener { onProjectClick(position) }
        holder.itemView.setOnLongClickListener { onProjectLongClick(position); true }
        holder.drag.setOnTouchListener { _, event -> if (event.action == MotionEvent.ACTION_DOWN) onDragStart(holder); false }
    }
    override fun getItemCount() = list.size
}

class LedAdapter(private var list: MutableList<LedItem>, val onDragStart: (RecyclerView.ViewHolder) -> Unit, val onDataChanged: () -> Unit, val onLedLongClick: (Int) -> Unit) : RecyclerView.Adapter<LedAdapter.LedViewHolder>() {
    fun updateList(newList: MutableList<LedItem>) { list = newList; notifyDataSetChanged() }
    inner class LedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txt: TextView = itemView.findViewById(R.id.txtLedName)
        val txtShortId: TextView = itemView.findViewById(R.id.txtShortId)
        val chk: CheckBox = itemView.findViewById(R.id.chkSelect)
        val drag: ImageView = itemView.findViewById(R.id.imgDragHandle)
        val sw: SwitchCompat = itemView.findViewById(R.id.switchLed)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = LedViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_led, parent, false))
    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: LedViewHolder, position: Int) {
        val item = list[position]
        holder.txt.text = item.name
        holder.txtShortId.text = if (item.shortId.isNotEmpty()) item.shortId else "LED${position + 1}"
        holder.chk.setOnCheckedChangeListener(null)
        holder.chk.isChecked = item.isSelected
        holder.chk.setOnCheckedChangeListener { _, isChecked -> item.isSelected = isChecked }
        holder.sw.setOnCheckedChangeListener(null)
        holder.sw.isChecked = item.isSwitchedOn
        holder.sw.setOnCheckedChangeListener { _, isChecked -> item.isSwitchedOn = isChecked; onDataChanged() }
        holder.itemView.setOnLongClickListener { onLedLongClick(position); true }
        holder.drag.setOnTouchListener { _, event -> if (event.action == MotionEvent.ACTION_DOWN) onDragStart(holder); false }
    }
    override fun getItemCount() = list.size
}
