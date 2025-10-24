package com.example.chatbot.model

enum class MessageStatus { SENDING, SENT, FAILED }

data class Message(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT
)
