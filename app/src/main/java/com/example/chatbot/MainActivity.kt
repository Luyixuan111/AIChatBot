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
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Button


class MainActivity : AppCompatActivity() {

    private lateinit var chatList: RecyclerView
    private lateinit var input: EditText
    private lateinit var send: MaterialButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: MaterialToolbar

    private val adapter = ChatAdapter()
    private val rows = mutableListOf<MessageRow>()

    private var autoCallTimer: android.os.CountDownTimer? = null


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
                R.id.nav_emergency_list -> {
                    showEmergencyListDialog()
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
                showEmergencyContactDialog()
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

    // Emergency Contact button (create emergency contact)

    private data class EmergencyContact(val name: String, val relation: String, val phone: String)

    private fun saveEmergencyContact(c: EmergencyContact) {
        val sp = getSharedPreferences("chatbot_prefs", MODE_PRIVATE)
        sp.edit()
            .putString("ec_name", c.name)
            .putString("ec_relation", c.relation)
            .putString("ec_phone", c.phone)
            .apply()
    }

    private fun loadEmergencyContact(): EmergencyContact? {
        val sp = getSharedPreferences("chatbot_prefs", MODE_PRIVATE)
        val name = sp.getString("ec_name", null)
        val relation = sp.getString("ec_relation", null)
        val phone = sp.getString("ec_phone", null)
        return if (!name.isNullOrBlank() && !phone.isNullOrBlank()) {
            EmergencyContact(name, relation.orEmpty(), phone)
        } else null
    }

    private fun showEmergencyContactDialog() {
        val content = layoutInflater.inflate(R.layout.dialog_emergency_contact, null)
        val etName = content.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etName)
        val etRelation = content.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etRelation)
        val etPhone = content.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPhone)

        // preload if exists
        loadEmergencyContact()?.let { c ->
            etName.setText(c.name)
            etRelation.setText(c.relation)
            etPhone.setText(c.phone)
        }

        val dlg = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Set Emergency Contact")
            .setView(content)
            .setPositiveButton("Save", null) // we attach click later to do validation
            .setNeutralButton("Call", null)   // enabled later if a phone exists
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        dlg.setOnShowListener {
            val btnSave = dlg.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            val btnCall = dlg.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL)

            fun validPhone(p: String) = p.filter { it.isDigit() || it == '+' }.length >= 7

            // enable Call if number present
            btnCall.isEnabled = !etPhone.text.isNullOrBlank()
            btnCall.setOnClickListener {
                val phone = etPhone.text?.toString()?.trim().orEmpty()
                if (validPhone(phone)) {
                    openDialer(phone)
                } else {
                    appendBot("⚠️ Phone number looks invalid.")
                }
            }

            btnSave.setOnClickListener {
                val name = etName.text?.toString()?.trim().orEmpty()
                val relation = etRelation.text?.toString()?.trim().orEmpty()
                val phone = etPhone.text?.toString()?.trim().orEmpty()

                if (name.isEmpty()) { etName.error = "Required"; return@setOnClickListener }
                if (!validPhone(phone)) { etPhone.error = "Invalid phone"; return@setOnClickListener }

                val contact = EmergencyContact(name, relation, phone)
//                saveEmergencyContact(contact)
                upsertEmergencyContactToList(contact)
                appendBot("✅ Emergency contact saved: ${contact.name} (${contact.relation.ifBlank { "Contact" }}) • ${contact.phone}")
                dlg.dismiss()
            }
        }

        dlg.show()
    }

    private fun openDialer(phone: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
            data = android.net.Uri.parse("tel:$phone")
        }
        startActivity(intent)
    }

    private fun loadEmergencyList(): MutableList<EmergencyContact> {
        val sp = getSharedPreferences("chatbot_prefs", MODE_PRIVATE)
        val raw = sp.getString("ec_list", "[]") ?: "[]"
        val arr = org.json.JSONArray(raw)
        val list = mutableListOf<EmergencyContact>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list += EmergencyContact(
                o.optString("name"),
                o.optString("relation"),
                o.optString("phone")
            )
        }
        return list
    }

    private fun saveEmergencyList(list: List<EmergencyContact>) {
        val arr = org.json.JSONArray()
        list.forEach { c ->
            val o = org.json.JSONObject()
            o.put("name", c.name)
            o.put("relation", c.relation)
            o.put("phone", c.phone)
            arr.put(o)
        }
        val sp = getSharedPreferences("chatbot_prefs", MODE_PRIVATE)
        sp.edit().putString("ec_list", arr.toString()).apply()
    }

    private inner class ContactAdapter(
        val data: MutableList<EmergencyContact>,
        private val onCall: (EmergencyContact) -> Unit,
        private val onChanged: () -> Unit
    ) : RecyclerView.Adapter<ContactAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvName = v.findViewById<TextView>(R.id.tvName)
            val tvRelation = v.findViewById<TextView>(R.id.tvRelation)
            val tvPhone = v.findViewById<TextView>(R.id.tvPhone)
            val btnCall = v.findViewById<Button>(R.id.btnCall)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = layoutInflater.inflate(R.layout.item_emergency_contact, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(h: VH, pos: Int) {
            val c = data[pos]
            h.tvName.text = c.name
            h.tvRelation.text = if (c.relation.isBlank()) "Contact" else c.relation
            h.tvPhone.text = c.phone
            h.btnCall.setOnClickListener { onCall(c) }

            // hold to pop up
            h.itemView.setOnLongClickListener {
                val actions = arrayOf("Set Primary", "Edit", "Delete", "Cancel")
                com.google.android.material.dialog.MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(c.name)
                    .setItems(actions) { d, which ->
                        when (which) {
                            0 -> { // set primary
                                setPrimaryContact(c)
                                // set to index 0
                                val index = data.indexOfFirst { it.phone == c.phone || it.name.equals(c.name, true) }
                                if (index >= 0) {
                                    val item = data.removeAt(index)
                                    data.add(0, item)
                                    notifyDataSetChanged()
                                    onChanged()
                                }
                            }
                            1 -> { // edit
                                showContactEditorDialog(initial = c) {
                                    // reload
                                    val fresh = loadEmergencyList()
                                    data.clear()
                                    data.addAll(fresh)
                                    notifyDataSetChanged()
                                    onChanged()
                                }
                            }
                            2 -> { // delete
                                val list = loadEmergencyList().toMutableList()
                                val idx = list.indexOfFirst { it.phone == c.phone || it.name.equals(c.name, true) }
                                if (idx >= 0) {
                                    list.removeAt(idx)
                                    saveEmergencyList(list)
                                    showSavedSnackbar("Deleted：${c.name}")
                                    // sync data
                                    val di = data.indexOfFirst { it.phone == c.phone || it.name.equals(c.name, true) }
                                    if (di >= 0) {
                                        data.removeAt(di)
                                        notifyItemRemoved(di)
                                    } else {
                                        notifyDataSetChanged()
                                    }
                                    onChanged()
                                }
                            }
                            else -> d.dismiss()
                        }
                    }
                    .show()
                true
            }
        }

        override fun getItemCount() = data.size
    }

//        Emergency contact list button
    private fun showEmergencyListDialog() {
        val content = layoutInflater.inflate(R.layout.dialog_emergency_list, null)
        val tvCountdown = content.findViewById<TextView>(R.id.tvCountdown)
        val rv = content.findViewById<RecyclerView>(R.id.rvContacts)
        val btnAdd = content.findViewById<Button>(R.id.btnAdd)

        val contacts = loadEmergencyList()

        rv.layoutManager = LinearLayoutManager(this)
        val adapter = ContactAdapter(
            data = contacts,
            onCall = { c ->
                cancelAutoCallTimer()
                appendBot("📞 Dialing ${c.name} (${c.relation.ifBlank { "Contact" }}) at ${c.phone}…")
                openDialer(c.phone)
            },
            onChanged = {
                // after edit reload the timer
                if (adapter.itemCount > 0) {
                    tvCountdown.text = "Auto call ${contacts[0].name} in 60s…"
                } else {
                    tvCountdown.text = "No contacts found."
                }
            }
        )
        rv.adapter = adapter

        val dlg = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Emergency contacts")
            .setView(content)
            .setNegativeButton("Close") { d, _ ->
                cancelAutoCallTimer(); d.dismiss()
            }
            .create()

        // cancel auto call
        val cancelByUser: () -> Unit = {
            if (autoCallTimer != null) {
                appendBot("⏹️ Auto-call cancelled.")
                cancelAutoCallTimer()
                tvCountdown.text = "Auto-call cancelled."
            }
        }
        rv.setOnTouchListener { _, _ -> cancelByUser(); false }
        btnAdd.setOnClickListener {
            cancelByUser()
            showContactEditorDialog(initial = null) {
                // after added refresh it
                val fresh = loadEmergencyList()
                contacts.clear()
                contacts.addAll(fresh)
                adapter.notifyDataSetChanged()
                if (contacts.isNotEmpty()) {
                    tvCountdown.text = "Auto call ${contacts[0].name} in 60s…"
                } else {
                    tvCountdown.text = "No contacts found."
                }
            }
            dlg.dismiss()
        }

        dlg.setOnShowListener {
            startAutoCallTimer(
                onTick = { secLeft ->
                    if (adapter.itemCount > 0) {
                        tvCountdown.text = "Auto call ${contacts[0].name} in ${secLeft}s…"
                    } else {
                        tvCountdown.text = "No contacts found."
                    }
                },
                onFinish = {
                    if (adapter.itemCount > 0) {
                        val first = contacts[0]
                        appendBot("⏰ No response. Auto dialing ${first.name} • ${first.phone}")
                        openDialer(first.phone)
                    } else {
                        tvCountdown.text = "No contacts found."
                    }
                    dlg.dismiss()
                }
            )
        }

        dlg.setOnDismissListener { cancelAutoCallTimer() }
        dlg.show()
    }

    private fun startAutoCallTimer(onTick: (Int) -> Unit, onFinish: () -> Unit) {
        cancelAutoCallTimer()
        autoCallTimer = object : android.os.CountDownTimer(60_000, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                onTick((millisUntilFinished / 1000).toInt())
            }
            override fun onFinish() { onFinish() }
        }.start()
    }

    private fun cancelAutoCallTimer() {
        autoCallTimer?.cancel()
        autoCallTimer = null
    }

    // Emergency contact update
    private fun upsertEmergencyContactToList(c: EmergencyContact) {
        val list = loadEmergencyList()

        val idx = list.indexOfFirst {
            it.phone == c.phone || it.name.equals(c.name, ignoreCase = true)
        }
        if (idx >= 0) {
            list[idx] = c
        } else {
            list.add(0, c)
        }

        saveEmergencyList(list)

        saveEmergencyContact(c)

        showSavedSnackbar("Added Successfully")
    }

    private fun setPrimaryContact(c: EmergencyContact) {
        val list = loadEmergencyList().toMutableList()
        val idx = list.indexOfFirst { it.phone == c.phone || it.name.equals(c.name, true) }
        if (idx >= 0) {
            val item = list.removeAt(idx)
            list.add(0, item)
            saveEmergencyList(list)
            showSavedSnackbar("Set primary contact successfully：${item.name}")
        }
    }

    private fun showSavedSnackbar(msg: String) {
        val rootView = findViewById<View>(R.id.root)
        com.google.android.material.snackbar.Snackbar.make(rootView, msg, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
    }

    private fun showContactEditorDialog(
        initial: EmergencyContact? = null,
        onSaved: (() -> Unit)? = null
    ) {
        val content = layoutInflater.inflate(R.layout.dialog_emergency_contact, null)
        val etName = content.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etName)
        val etRelation = content.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etRelation)
        val etPhone = content.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPhone)

        // pre filled
        initial?.let {
            etName.setText(it.name)
            etRelation.setText(it.relation)
            etPhone.setText(it.phone)
        }

        val dlg = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(if (initial == null) "Add contact" else "Edit contact")
            .setView(content)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        dlg.setOnShowListener {
            val btnSave = dlg.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)

            fun validPhone(p: String) = p.filter { it.isDigit() || it == '+' }.length >= 7

            btnSave.setOnClickListener {
                val name = etName.text?.toString()?.trim().orEmpty()
                val relation = etRelation.text?.toString()?.trim().orEmpty()
                val phone = etPhone.text?.toString()?.trim().orEmpty()

                if (name.isEmpty()) { etName.error = "Required"; return@setOnClickListener }
                if (!validPhone(phone)) { etPhone.error = "Invalid phone"; return@setOnClickListener }

                upsertEmergencyContactToList(EmergencyContact(name, relation, phone))
                dlg.dismiss()
                onSaved?.invoke()
            }
        }

        dlg.show()
    }




    private fun fakeReply(userText: String): String =
        "You said: “$userText”. (This AI focuses on elder health support.)"
}
