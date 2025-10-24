package com.example.chatbot.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chatbot.R
import com.example.chatbot.model.Message

private const val TYPE_USER = 1
private const val TYPE_BOT = 2
private const val TYPE_TYPING = 3

class ChatAdapter : ListAdapter<MessageRow, RecyclerView.ViewHolder>(DIFF) {

    object DIFF : DiffUtil.ItemCallback<MessageRow>() {
        override fun areItemsTheSame(oldItem: MessageRow, newItem: MessageRow): Boolean =
            oldItem.key == newItem.key

        override fun areContentsTheSame(oldItem: MessageRow, newItem: MessageRow): Boolean =
            oldItem == newItem
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is MessageRow.User -> TYPE_USER
        is MessageRow.Bot -> TYPE_BOT
        is MessageRow.Typing -> TYPE_TYPING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> MsgVH(inf.inflate(R.layout.item_message_user, parent, false))
            TYPE_BOT  -> MsgVH(inf.inflate(R.layout.item_message_bot, parent, false))
            else      -> TypingVH(inf.inflate(R.layout.item_typing, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is MessageRow.User -> (holder as MsgVH).bind(row.message.text)
            is MessageRow.Bot  -> (holder as MsgVH).bind(row.message.text)
            is MessageRow.Typing -> { /* nothing */ }
        }
    }

    class MsgVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tv: TextView = view.findViewById(R.id.messageText)
        fun bind(text: String) { tv.text = text }
    }

    class TypingVH(view: View) : RecyclerView.ViewHolder(view)
}

/** Rows for the list (user, bot, typing). */
sealed class MessageRow(val key: String) {
    data class User(val message: Message) : MessageRow("u_${message.id}")
    data class Bot(val message: Message)  : MessageRow("b_${message.id}")
    data object Typing : MessageRow("typing")
}
