package com.example.chatterapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DashboardScreen(username: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Ulogovan si kao: $username 👋", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Arhitektura ekrana je uspešno resetovana i modularna!", fontSize = 14.sp, color = Color.Gray)
        }
    }
}
