package com.example.chatterapp.data

data class ChatMessage(
    val username: String,
    val message: String,
    val date: String,
    val seenBy: List<String> = emptyList() // Lista korisnika koji su videli poruku
)
