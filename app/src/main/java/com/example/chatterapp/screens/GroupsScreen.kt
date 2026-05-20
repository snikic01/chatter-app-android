package com.example.chatterapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatterapp.data.ChatMessage

// Model podataka za grupe koji pravimo unutar samog ekrana radi jednostavnosti
data class AndroidChatGroup(val id: Int, val name: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    currentUsername: String,
    messagesList: List<ChatMessage>,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    onSendMessageClick: () -> Unit,
    listState: LazyListState,
    // NOVI PARAMETRI KOJI ČINE EKRAN DINAMIČKIM:
    activeGroupId: Int,
    onGroupChange: (Int) -> Unit,
    groupsList: List<AndroidChatGroup>
) {
    if (activeGroupId == 0) {
        // --- 1. PRIKAZ LISTE SVIH DOSTUPNIH GRUPA ---
        Scaffold(
            topBar = { TopAppBar(title = { Text("Izaberi Čet Grupu", fontWeight = FontWeight.Bold) }) }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF8F9FA))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groupsList) { group ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGroupChange(group.id) }, // Klik postavlja aktivni ID grupe
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = group.name,
                            modifier = Modifier.padding(20.dp),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                    }
                }
            }
        }
    } else {
        // --- 2. PRIKAZ ČETA ZA SELEKTOVANU GRUPU ---
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2196F3))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onGroupChange(0) }) { // Vraća nazad na listu grupa
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Grupa ID: $activeGroupId", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messagesList) { msg ->
                    val isMyMessage = msg.username.trim().lowercase() == currentUsername.trim().lowercase()
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isMyMessage) Alignment.CenterEnd else Alignment.CenterStart) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (isMyMessage) Color(0xFF2196F3) else Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(0.75f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(text = if (isMyMessage) "Ti" else msg.username, fontWeight = FontWeight.Bold, color = if (isMyMessage) Color.White else Color(0xFF2196F3), fontSize = 13.sp)
                                    Text(text = msg.date, color = if (isMyMessage) Color(0xFFE0E0E0) else Color.Gray, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = msg.message, color = if (isMyMessage) Color.White else Color.Black, fontSize = 15.sp)

                                // Dynamic Seen Status ispod poruke
                                if (msg.seenBy.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "✓ Viđeno: ${msg.seenBy.joinToString(", ")}",
                                        fontSize = 11.sp,
                                        color = if (isMyMessage) Color(0xFFE0E0E0) else Color.Gray,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = textInput, onValueChange = onTextInputChange, modifier = Modifier.weight(1f).padding(end = 8.dp), shape = RoundedCornerShape(24.dp))
                IconButton(onClick = onSendMessageClick, enabled = textInput.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Pošalji", tint = Color(0xFF2196F3))
                }
            }
        }
    }
}
