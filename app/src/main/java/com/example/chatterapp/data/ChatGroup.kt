package com.example.chatterapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

@Composable
fun GroupsScreen(
    currentUsername: String,
    messagesList: List<ChatMessage>,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    onSendMessageClick: () -> Unit,
    listState: LazyListState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Gornji bar sa imenom grupe
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2196F3))
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Grupa: KontraverzniBiznismeni (ID: 8)",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Čet oblačići (Chat Bubbles) sa ugrađenom proveri za ulogovanog korisnika
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messagesList) { msg ->
                val isMyMessage = msg.username.trim().lowercase() == currentUsername.trim().lowercase()

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isMyMessage) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMyMessage) Color(0xFF2196F3) else Color.White
                        ),
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isMyMessage) 12.dp else 0.dp,
                            bottomEnd = if (isMyMessage) 0.dp else 12.dp
                        ),
                        modifier = Modifier.fillMaxWidth(0.75f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isMyMessage) "Ti" else msg.username,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMyMessage) Color.White else Color(0xFF2196F3),
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = msg.date,
                                    color = if (isMyMessage) Color(0xFFE0E0E0) else Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.message,
                                color = if (isMyMessage) Color.White else Color.Black,
                                fontSize = 15.sp
                            )

                            // --- DYNAMIC SEEN STATUS ISPOD PORUKE ---
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

        // Donji deo za kucanje i slanje
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = onTextInputChange,
                placeholder = { Text("Upiši poruku...") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                maxLines = 3,
                shape = RoundedCornerShape(24.dp)
            )

            IconButton(
                onClick = onSendMessageClick,
                enabled = textInput.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Pošalji",
                    tint = Color(0xFF2196F3)
                )
            }
        }
    }
}
