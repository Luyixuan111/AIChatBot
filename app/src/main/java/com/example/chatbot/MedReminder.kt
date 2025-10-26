package com.example.chatbot

import java.util.UUID

data class MedReminder(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dose: String,
    val hour: Int,
    val minute: Int,
    val days: Set<Int>
)
