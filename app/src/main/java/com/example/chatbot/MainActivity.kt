package com.example.chatbot

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatbot.model.Message
import com.example.chatbot.ui.ChatAdapter
import com.example.chatbot.ui.MessageRow
import com.google.android.material.button.MaterialButton
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var chatList: RecyclerView
    private lateinit var input: EditText
    private lateinit var send: MaterialButton
    private val adapter = ChatAdapter()

    private val rows = mutableListOf<MessageRow>()
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_new_chat -> { rows.clear(); pushList(); appendBot("New chat started."); true }
            R.id.action_clear    -> { rows.clear(); pushList(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val toolbar: com.google.android.material.appbar.MaterialToolbar = findViewById(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_new_chat -> { rows.clear(); pushList(); appendBot("New chat started."); true }
                R.id.action_clear    -> { rows.clear(); pushList(); true }
                else -> false
            }
        }


        // Handle IME insets so input bar lifts with keyboard
        val root = findViewById<View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(0, 0, 0, ime.bottom)
            WindowInsetsCompat.CONSUMED
        }

        chatList = findViewById(R.id.chatList)
        input = findViewById(R.id.messageInput)
        send = findViewById(R.id.sendBtn)

        chatList.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        chatList.adapter = adapter

        // Seed a greeting
        appendBot("Hi! I’m your Kotlin chat. Ask me anything.")

        send.setOnClickListener { submitMessage() }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitMessage()
                true
            } else false
        }
    }

    private fun submitMessage() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return

        appendUser(text)
        input.setText("")

        // Show a typing indicator then fake a reply after a short delay
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

    private fun fakeReply(userText: String): String =
        "You said: “$userText”. (Replace this with real API later.)"
}
