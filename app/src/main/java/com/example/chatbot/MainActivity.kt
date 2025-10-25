package com.example.chatbot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatbot.model.Message
import com.example.chatbot.ui.ChatAdapter
import com.example.chatbot.ui.MessageRow
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.navigation.NavigationView
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var chatList: RecyclerView
    private lateinit var input: EditText
    private lateinit var send: MaterialButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: MaterialToolbar

    private val adapter = ChatAdapter()
    private val rows = mutableListOf<MessageRow>()

    // ---------- Activity lifecycle ----------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // ----- Toolbar & Drawer -----
        toolbar = findViewById(R.id.topAppBar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navigationView)

        // Open the Drawer
        toolbar.setNavigationOnClickListener {
            drawerLayout.open()
        }

        // Left side menu bar click event
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_new_chat -> {
                    rows.clear(); pushList(); appendBot("New chat started.")
                    drawerLayout.close(); true
                }
                R.id.nav_history -> {
                    appendBot("📚 Showing chat history (demo).")
                    drawerLayout.close(); true
                }
                R.id.nav_vitals -> {
                    appendBot("🩺 Let's record your vitals.")
                    drawerLayout.close(); true
                }
                R.id.nav_meds -> {
                    appendBot("💊 Medication reminders available here.")
                    drawerLayout.close(); true
                }
                R.id.nav_caregiver -> {
                    appendBot("📞 Calling caregiver (demo).")
                    drawerLayout.close(); true
                }
                R.id.nav_clinic -> {
                    appendBot("🏥 Finding nearby clinics (demo).")
                    drawerLayout.close(); true
                }
                else -> false
            }
        }

        // ----- Chat view setup -----
        val root = findViewById<View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(0, 0, 0, ime.bottom)
            WindowInsetsCompat.CONSUMED
        }

        chatList = findViewById(R.id.chatList)
        input = findViewById(R.id.messageInput)
        send = findViewById(R.id.sendBtn)

        chatList.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        chatList.adapter = adapter

        appendBot("Hi! I’m your Kotlin chat. Ask me anything.")

        send.setOnClickListener { submitMessage() }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitMessage(); true
            } else false
        }
    }

    // ---------- Top-right menu ----------
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_health_panel -> { showHealthToolsSheet(); true }
            R.id.action_clear -> { rows.clear(); pushList(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ---------- BottomSheet health tools ----------
    private fun showHealthToolsSheet() {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this)
            .inflate(R.layout.bottomsheet_health_tools, null)
        dialog.setContentView(view)

        view.findViewById<Chip>(R.id.chipBP)
            .setOnClickListener {
                appendBot("🩺 Please enter your Blood Pressure (e.g. 120/80 mmHg)")
                showBpDialog()
            }

        view.findViewById<Chip>(R.id.chipHR)
            .setOnClickListener {
                appendBot("Please enter your Heart Rate (e.g. 72 bpm)")
                showHrDialog()
            }

        view.findViewById<Chip>(R.id.chipMeds)
            .setOnClickListener {
                appendBot("Let's set a medication reminder.")
                dialog.dismiss()
            }

        view.findViewById<Chip>(R.id.chipEmergency)
            .setOnClickListener {
                appendBot("Emergency contact (demo).")
                dialog.dismiss()
            }

        dialog.show()
    }

    // ---------- Chat helpers ----------
    private fun submitMessage() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return

        appendUser(text)
        input.setText("")

        showTyping(true)
        chatList.postDelayed({
            showTyping(false)
            appendBot(fakeReply(text))
        }, 600)
    }

    private fun appendUser(text: String) {
        val msg = Message(id = UUID.randomUUID().toString(), text = text, isUser = true)
        rows += MessageRow.User(msg)
        pushList()
    }

    private fun appendBot(text: String) {
        val msg = Message(id = UUID.randomUUID().toString(), text = text, isUser = false)
        rows += MessageRow.Bot(msg)
        pushList()
    }

    private fun showTyping(show: Boolean) {
        if (show && rows.none { it is MessageRow.Typing }) {
            rows += MessageRow.Typing
        } else if (!show) {
            rows.removeAll { it is MessageRow.Typing }
        }
        pushList()
    }

    private fun pushList() {
        adapter.submitList(rows.toList())
        chatList.scrollToPosition(adapter.itemCount - 1)
    }

    private fun showTextInputDialog(
        title: String,
        hint: String,
        onConfirm: (String) -> Unit
    ) {
        val inputView = layoutInflater.inflate(R.layout.dialog_single_input, null)
        val et = inputView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etValue)
        et.hint = hint

        val dlg = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(inputView)
            .setPositiveButton("OK") { d, _ ->
                val text = et.text.toString().trim()
                if (text.isNotEmpty()) onConfirm(text)
                d.dismiss()
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        dlg.show()
    }

    // HR dialog – single input
    private fun showHrDialog() {
        showTextInputDialog(
            title = "Enter Heart Rate",
            hint = "e.g. 72 bpm"
        ) { text ->
            appendBot("❤️ Recorded heart rate: $text bpm")
        }
    }

    // BP dialog – two-step input (systolic then diastolic)
    private fun showBpDialog() {
        showTextInputDialog(
            title = "Enter Systolic Pressure",
            hint = "e.g. 120 mmHg"
        ) { sys ->
            showTextInputDialog(
                title = "Enter Diastolic Pressure",
                hint = "e.g. 80 mmHg"
            ) { dia ->
                appendBot("🩺 Recorded blood pressure: $sys/$dia mmHg")
            }
        }
    }

    private fun fakeReply(userText: String): String =
        "You said: “$userText”. (This AI focuses on elder health support.)"
}
