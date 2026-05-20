package com.example.chatterapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GroupsScreen(onGroupSelect: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable { onGroupSelect(8) }, // Klik automatski bira grupu 8
        contentAlignment = Alignment.Center
    ) {
        Text("Klikni ovde da učitaš grupu: KontraverzniBiznismeni", fontSize = 16.sp, color = Color.Black)
    }
}
