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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatterapp.data.ChatMessage

// Proširujemo tvoj lokalni model da aplikacija zna ko je vlasnik (za brisanje)
data class AndroidChatGroup(val id: Int, val name: String, val isOwner: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    currentUsername: String,
    messagesList: List<ChatMessage>,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    onSendMessageClick: () -> Unit,
    listState: LazyListState,
    activeGroupId: Int,
    onGroupChange: (Int) -> Unit,
    groupsList: List<AndroidChatGroup>
) {
    // Lokalna stanja za dijalog članova (da ne opterećujemo MainActivity)
    var showMembersDialog by remember { mutableStateOf(false) }

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
                            .clickable { onGroupChange(group.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = group.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2196F3)
                                )
                                Text(
                                    text = if (group.isOwner) "Ti si vlasnik (Možeš obrisati)" else "Član si grupe (Možeš napustiti)",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            // Dugmici sa akcijama (Okidaju promenu grupe sa negativnim ID-jem koji MainActivity prepoznaje)
                            IconButton(onClick = {
                                if (group.isOwner) {
                                    onGroupChange(-group.id) // Negativan ID signalizira brisanje u MainActivity
                                } else {
                                    onGroupChange(-group.id * 100) // Ekstreman negativan ID signalizira napuštanje
                                }
                            }) {
                                Icon(
                                    imageVector = if (group.isOwner) Icons.Default.Delete else Icons.Default.ExitToApp,
                                    contentDescription = "Akcija",
                                    tint = Color.Red
                                )
                            }
                        }
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onGroupChange(0) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Grupa ID: $activeGroupId", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                // Dugme za otvaranje članova (Pali lokalni dijalog)
                TextButton(onClick = { showMembersDialog = true }) {
                    Icon(Icons.Default.Info, contentDescription = "Članovi", tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Članovi", color = Color.White, fontWeight = FontWeight.Bold)
                }
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

    // --- DIJALOG KOJI PRIKAZUJE KO JE SVE U OVOJ GRUPI ---
    if (showMembersDialog) {
        AlertDialog(
            onDismissRequest = { showMembersDialog = false },
            title = { Text("Članovi grupe", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text("Korisnici sa sajta koji su u ovom četu:", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Koristićemo autore trenutnih poruka da rekonstruišemo listu članova na ekranu
                    val uniqueMembers = messagesList.map { it.username }.distinct()

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (uniqueMembers.isEmpty()) {
                            item { Text("• Nema aktivnih članova u istoriji", color = Color.Black) }
                        } else {
                            items(uniqueMembers) { member ->
                                Text(text = "• $member", fontSize = 16.sp, color = Color.Black, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showMembersDialog = false }) { Text("Zatvori") }
            }
        )
    }
}
