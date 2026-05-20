package com.example.chatterapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

@Composable
fun PrivateChatScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA)),
        contentAlignment = Alignment.Center
    ) {
        Text("✉️ Privatne poruke 1-na-1 (Uskoro)", fontSize = 15.sp, color = Color.Gray)
    }
}
