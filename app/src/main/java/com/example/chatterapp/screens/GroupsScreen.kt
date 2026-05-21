package com.example.chatterapp.screens

import org.json.JSONObject
import io.ktor.client.statement.bodyAsText
import androidx.compose.runtime.setValue
import io.ktor.client.request.get

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatterapp.data.ChatMessage

import androidx.compose.foundation.lazy.itemsIndexed

// Tvoj lokalni model proširen poljem unreadCount za brojač sa sajta
data class AndroidChatGroup(
    val id: Int,
    val name: String,
    val isOwner: Boolean = false,
    val unreadCount: Int = 0,
    val ownerName: String = "" // DODATO za vlasnika grupe
)

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
    groupsList: List<AndroidChatGroup>,
    client: io.ktor.client.HttpClient // DODAT PARAMETAR KLIJENTA
) {
    var showMembersDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val currentGroup = groupsList.find { it.id == activeGroupId }
    val isOwnerOfCurrentGroup = currentGroup?.isOwner ?: false

    // DODATA PROMENLJIVA ZA ČLANOVE KOJA JE FALILA (Linija 135 sa slike)
    var groupMembers by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()


    if (activeGroupId == 0) {
        // --- 1. PRIKAZ LISTE TVOJIH GRUPA (SA REPLICIRANIM WEB FILTEROM) ---
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
                                Text(text = group.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                                Text(text = if (group.isOwner) "Ti si vlasnik" else "Član si grupe", fontSize = 12.sp, color = Color.Gray)
                            }

                            // --- CRVENI BADGE ZA NEPROČITANE PORUKE (Isto kao na veb aplikaciji) ---
                            if (group.unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.Red, shape = CircleShape)
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = group.unreadCount.toString(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Čim se lista poruka promeni ili napuni, automatski skrolujemo na poslednju stavku
        LaunchedEffect(messagesList.size) {
            if (messagesList.isNotEmpty()) {
                listState.animateScrollToItem(messagesList.size - 1)
            }
        }

        // --- DODATO: POZIV ZA ČLANOVE (Ujedinjuje vlasnika i korisnike preko api_groups.php) ---
        LaunchedEffect(showMembersDialog) {
            if (showMembersDialog && activeGroupId != 0) {
                try {
                    val url = com.example.chatterapp.data.NetworkConfig.getMembersUrl(activeGroupId)

                    // POPRAVLJENO: Tačan naziv je withContext (sa malim w i velikim C)
                    val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        // POPRAVLJENO: Tačan poziv funkcije je client.get(url)
                        client.get(url)
                    }

                    val json = JSONObject(response.bodyAsText())
                    if (json.optBoolean("success", false)) {
                        val array = json.getJSONArray("members")
                        val tempList = mutableListOf<Pair<Int, String>>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            tempList.add(Pair(obj.getInt("id"), obj.getString("username")))
                        }
                        groupMembers = tempList
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ChatterMembers", "Greška pri učitavanju članova: ${e.message}")
                }
            }
        }


        // --- 2. PRIKAZ ČETA UNUTAR SELEKTOVANE GRUPE (POPRAVLJENA STRUKTURA BARA) ---
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
                    Text(text = currentGroup?.name ?: "Grupa ID: $activeGroupId", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showMembersDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Članovi", tint = Color.White)
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opcije", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            // 1. OPCIJA ZA SVE (I vlasnik i običan član mogu uvek da napuste grupu i prenesu vlast)
                            DropdownMenuItem(
                                text = { Text("Napusti grupu", color = Color.DarkGray) },
                                leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.DarkGray) },
                                onClick = {
                                    onGroupChange(-activeGroupId * 100)
                                    showMenu = false
                                }
                            )

                            // 2. DODATNA OPCIJA (Garantovano se iscrtava vlasniku na tri tačke jer je struktura popravljena)
                            if (isOwnerOfCurrentGroup) {
                                DropdownMenuItem(
                                    text = { Text("Obriši grupu (Za sve)", color = Color.Red, fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                                    onClick = {
                                        onGroupChange(-activeGroupId)
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    } // Zatvara Box ispravno
                } // Zatvara desni Row dugmića ispravno
            } // Zatvara gornji plavi Row ispravno

// --- SREDIŠNJI DEO: MODERNI BALONČIĆI SA AUTOMATSKIM SKROLOM ---
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Zauzima sav prostor između bara i polja za unos
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // POPRAVLJENO: Koristimo itemsIndexed da imamo pristup index-u za promenu datuma
                itemsIndexed(messagesList) { index, chatMessage ->
                    val isMe = chatMessage.username == currentUsername

                    // Izvlačimo datum (npr. "2026-05-21") iz formata "YYYY-MM-DD HH:MM:SS"
                    val trenutniDatum = chatMessage.date.substringBefore(" ")

                    // --- 1. PREGRADNICA ZA DATUM ---
                    // Prikazuje se ako je prva poruka u četu ili ako je datum drugačiji od prethodne poruke
                    val prikaziPregradnicu = index == 0 || messagesList[index - 1].date.substringBefore(" ") != trenutniDatum

                    if (prikaziPregradnicu) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Leva linija pregradnice
                            Spacer(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFDDDDDD)))

                            // Tekst sa datumom
                            Text(
                                text = trenutniDatum.ifBlank { "Istorija" },
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            // Desna linija pregradnice
                            Spacer(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFDDDDDD)))
                        }
                    }

                    // --- GLAVNI BALONČIĆ ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Column(
                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            if (!isMe) {
                                Text(
                                    text = chatMessage.username,
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isMe) Color(0xFF2196F3) else Color(0xFFE0E0E0),
                                        // OBRISANA POGREŠNA PUTANJA: Koristimo čist import sa vrha fajla!
                                        shape = RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isMe) 16.dp else 2.dp,
                                            bottomEnd = if (isMe) 2.dp else 16.dp
                                        )
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = chatMessage.message,
                                        color = if (isMe) Color.White else Color.Black,
                                        fontSize = 16.sp
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        // Izvlačimo samo sate i minute iz formata datuma
                                        val vreme = chatMessage.date.substringAfter(" ").substringBeforeLast(":")
                                        Text(
                                            text = vreme.ifBlank { "00:00" },
                                            fontSize = 10.sp,
                                            color = if (isMe) Color(0xFFBBDEFB) else Color.Gray
                                        )

                                        // --- 2. SKUPNI SEEN STATUS ZA IMENA ---
                                        if (isMe && chatMessage.seenBy.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(4.dp))

                                            // Filtriramo tvoje ime (da ne piše da si ti video svoju poruku)
                                            val ostaliKorisnici = chatMessage.seenBy.filter { it != currentUsername }

                                            if (ostaliKorisnici.isNotEmpty()) {
                                                // Spajamo sva imena u jedan string razdvojen zarezom (npr. "nikic, petar")
                                                val imenaKojiSuVideli = ostaliKorisnici.joinToString(", ")

                                                Text(
                                                    text = "✓ ($imenaKojiSuVideli)",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB2FF59) // Prelepa svetlo zelena boja
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = textInput, onValueChange = onTextInputChange, placeholder = { Text("Upiši poruku...") }, modifier = Modifier.weight(1f).padding(end = 8.dp), shape = RoundedCornerShape(24.dp))
                IconButton(onClick = onSendMessageClick, enabled = textInput.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Pošalji", tint = Color(0xFF2196F3))
                }
            }
        }
    }

    if (showMembersDialog) {
        AlertDialog(
            onDismissRequest = { showMembersDialog = false },
            title = { Text("Članovi grupe", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text("Korisnici sa sajta koji su u ovom četu:", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))

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
