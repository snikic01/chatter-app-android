package com.example.chatterapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrivateChatsScreen() {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("✉️", fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Privatni četovi 1-na-1", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text("Uskoro: Pregled i pisanje privatnih poruka iz baze.", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
