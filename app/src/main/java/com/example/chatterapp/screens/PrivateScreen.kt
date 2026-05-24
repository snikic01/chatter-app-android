package com.example.chatterapp.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatterapp.data.ChatMessage
import com.example.chatterapp.data.NetworkConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

// Podaci za listu privatnih četova / prijatelja
data class AndroidPrivateChat(
    val id: Int,
    val username: String,
    val isOnline: Boolean,
    val lastMessage: String,
    val unreadCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateScreen(
    currentUsername: String,
    client: HttpClient,
    onChatToggle: (Boolean) -> Unit
) {
    // Držanje stanja privatnog četa
    var chatsList by remember { mutableStateOf(listOf<AndroidPrivateChat>()) }
    var activeChatUserId by remember { mutableStateOf(0) }
    var activeChatUsername by remember { mutableStateOf("") }
    var activeChatUserOnline by remember { mutableStateOf(false) }

    var privateMessagesList by remember { mutableStateOf(listOf<ChatMessage>()) }
    var privateTextInput by remember { mutableStateOf("") }

    // Searchbar:
    var searchQuery by remember { mutableStateOf("") }
    val filtriraniČatovi = chatsList.filter {
        it.username.contains(searchQuery, ignoreCase = true)
    }


    val coroutineScope = rememberCoroutineScope()
    val privateListState = rememberLazyListState()

    // --- 1. POLING ZA LISTU PRIVATNIH ČETOVA (Svake 3 sekunde) ---
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val url = NetworkConfig.getPrivateChatsUrl(currentUsername)
                val response = withContext(Dispatchers.IO) { client.get(url) }
                val json = JSONObject(response.bodyAsText())

                if (json.optBoolean("success", false)) {
                    val array = json.getJSONArray("chats")
                    val tempList = mutableListOf<AndroidPrivateChat>()

                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val chatUserId = obj.getInt("id")

                        // Ako korisnik trenutno gleda ovaj čet, nepročitane poruke su 0
                        val stvarniUnread = if (chatUserId == activeChatUserId) 0 else obj.optInt("unread_count", 0)

                        tempList.add(
                            AndroidPrivateChat(
                                id = chatUserId,
                                username = obj.getString("username"),
                                isOnline = obj.optInt("is_online", 0) == 1,
                                lastMessage = obj.optString("last_message", "Nema poruka"),
                                unreadCount = stvarniUnread
                            )
                        )
                    }
                    chatsList = tempList

                    // Ako je otvoren čet, ažuriramo online status u gornjem baru uživo
                    val trenutniChat = tempList.find { it.id == activeChatUserId }
                    if (trenutniChat != null) {
                        activeChatUserOnline = trenutniChat.isOnline
                    }
                }
            } catch (e: Exception) {
                Log.e("PrivateChats", "Greška u poleru lista: ${e.message}")
            }
            delay(3000)
        }
    }
    // --- 1. POLING ZA LISTU PRIVATNIH ČETOVA (Svake 3 sekunde) ---
    LaunchedEffect(activeChatUserId) {
        while (true) {
            try {
                val response = withContext(Dispatchers.IO) {
                    client.post(NetworkConfig.getPrivateSendApiUrl()) {
                        contentType(ContentType.Application.Json)
                        setBody(
                            JSONObject().apply {
                                put("action", "list")
                                put("username", currentUsername)
                            }.toString()
                        )
                    }
                }
                val responseText = response.bodyAsText()

                val json = JSONObject(responseText)
                //if (json.optBoolean("success", false)) {
                //    val array = json.getJSONArray("chats")
                //    val tempList = mutableListOf<AndroidPrivateChat>()

                //    for (i in 0 until array.length()) {
                //        val obj = array.getJSONObject(i)
                //        val chatUserId = obj.getInt("id")

                //        val stvarniUnread = if (chatUserId == activeChatUserId) 0 else obj.optInt("unread_count", 0)

                //        tempList.add(
                //           AndroidPrivateChat(
                //                id = chatUserId,
                //                username = obj.getString("username"),
                //                isOnline = obj.optInt("is_online", 0) == 1,
                //                lastMessage = obj.optString("last_message", "Nema poruka"),
                //                unreadCount = stvarniUnread
                //            )
                //        )
                //    }
                //    chatsList = tempList

                //    val trenutniChat = tempList.find { it.id == activeChatUserId }
                //    if (trenutniChat != null) {
                //        activeChatUserOnline = trenutniChat.isOnline
                //    }
                //}
            } catch (e: Exception) {
                Log.e("PrivateChats", "Greška u poleru lista: ${e.message}")
            }
            delay(3000)
        }
    }

    // --- 2. POLING ZA ISTORIJU PORUKA (Radi samo kada uđeš u čet sa nekim) ---
    LaunchedEffect(activeChatUserId) {
        onChatToggle(activeChatUserId != 0)
        if (activeChatUserId != 0) {
            while (activeChatUserId != 0) {
                try {
                    val response = withContext(Dispatchers.IO) {
                        client.post(NetworkConfig.getPrivateSendApiUrl()) {
                            contentType(ContentType.Application.Json)
                            setBody(
                                JSONObject().apply {
                                    put("action", "fetch")
                                    put("username", currentUsername)
                                    put("chat_user_id", activeChatUserId)
                                }.toString()
                            )
                        }
                    }
                    val responseText = response.bodyAsText()

                    val json = JSONObject(responseText)
                    if (json.optBoolean("success", false) || json.has("messages")) {
                        val array = json.getJSONArray("messages")
                        val tempList = mutableListOf<ChatMessage>()

                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val daLiJeVidjeno = obj.optInt("seen", 0) == 1
                            val seenList = if (daLiJeVidjeno) listOf("Vidjeno") else emptyList()

                            tempList.add(
                                ChatMessage(
                                    username = obj.getString("username"),
                                    message = obj.getString("message"),
                                    date = obj.getString("date"),
                                    seenBy = seenList
                                )
                            )
                        }
                        privateMessagesList = tempList
                    }
                } catch (e: Exception) {
                    Log.e("PrivateMessages", "Greška u poleru poruka: ${e.message}")
                }
                delay(3000)
            }
        }
    }
    // --- AUTOMATSKI SKROL NA KRAJ PRIVATNOG ČETA ---
    LaunchedEffect(privateMessagesList.size) {
        if (privateMessagesList.isNotEmpty()) {
            privateListState.animateScrollToItem(privateMessagesList.size - 1)
        }
    }

    // --- GLAVNI UI RENDER ---
    if (activeChatUserId == 0) {
        // === PRIKAZ SPISKA PRIVATNIH ČETOVA (PRIJATELJA) ===
        Scaffold(
            topBar = { TopAppBar(title = { Text("Privatne Poruke", fontWeight = FontWeight.Bold) }) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF8F9FA))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // 🔍 SEARCH BAR NA VRHU LISTE
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Pretraži prijatelje...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                // LISTA KOJA SADA KORISTI FILTER U REALNOM VREMENU
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filtriraniČatovi.isEmpty() && searchQuery.isNotBlank()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                                Text("Nema pronađenih prijatelja", color = Color.Gray)
                            }
                        }
                    } else {
                        items(filtriraniČatovi) { chat ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeChatUserId = chat.id
                                        activeChatUsername = chat.username
                                        activeChatUserOnline = chat.isOnline
                                        privateMessagesList = emptyList()
                                        searchQuery = "" // Čistimo pretragu pri ulasku u čet

                                        coroutineScope.launch(Dispatchers.IO) {
                                            try {
                                                val jsonBody = JSONObject().apply {
                                                    put("action", "mark")
                                                    put("username", currentUsername)
                                                    put("chat_user_id", chat.id)
                                                }.toString()
                                                client.post(NetworkConfig.getPrivateSeenApiUrl()) {
                                                    contentType(ContentType.Application.Json)
                                                    setBody(jsonBody)
                                                }
                                            } catch (e: Exception) { Log.e("PrivateSeen", "Greška: ${e.message}") }
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(
                                                    color = if (chat.isOnline) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                                                    shape = CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(text = chat.username, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                            Text(text = chat.lastMessage, fontSize = 13.sp, color = Color.Gray, maxLines = 1)
                                        }
                                    }

                                    if (chat.unreadCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color.Red, shape = CircleShape)
                                                .padding(horizontal = 10.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = chat.unreadCount.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // === PRIKAZ ČETA ZA IZABRANOG PRIJATELJA ===
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
            // GORNJI PLAVI BAR PRIVATNOG ČETA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2196F3))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { activeChatUserId = 0 }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))

                // 🟢 ONLINE STATUS LAMPICA U BARU LEVO OD IMENA
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (activeChatUserOnline) Color(0xFFB2FF59) else Color(0xFFD6D6D6),
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))

                Text(text = activeChatUsername, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            // LISTA PORUKA (Pregradnice i oblačići)
            LazyColumn(
                state = privateListState,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(privateMessagesList) { chatMessage ->
                    val isMe = chatMessage.username == currentUsername
                    val trenutniDatum = chatMessage.date.substringBefore(" ")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Column(
                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isMe) Color(0xFF2196F3) else Color(0xFFE0E0E0),
                                        shape = RoundedCornerShape(
                                            topStart = 16.dp, topEnd = 16.dp,
                                            bottomStart = if (isMe) 16.dp else 2.dp,
                                            bottomEnd = if (isMe) 2.dp else 16.dp
                                        )
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Column {
                                    Text(text = chatMessage.message, color = if (isMe) Color.White else Color.Black, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        val vreme = chatMessage.date.substringAfter(" ").substringBeforeLast(":")
                                        Text(text = vreme.ifBlank { "00:00" }, fontSize = 10.sp, color = if (isMe) Color(0xFFBBDEFB) else Color.Gray)

                                        // 🔒 PRIVATNI SEEN STATUS: Ispisuje "Vidjeno" samo ako si poruku poslao ti i ako je pročitana
                                        if (isMe && chatMessage.seenBy.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "✓ Viđeno", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB2FF59))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // DONJE POLJE ZA UNOS I SLANJE PORUKE
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = privateTextInput,
                    onValueChange = { privateTextInput = it },
                    placeholder = { Text("Unesite poruku...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (privateTextInput.isNotBlank()) {
                            val porukaZaSlanje = privateTextInput
                            privateTextInput = ""

                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val jsonBody = JSONObject().apply {
                                        put("action", "send")
                                        put("username", currentUsername)
                                        put("chat_user_id", activeChatUserId)
                                        put("message", porukaZaSlanje.trim())
                                    }.toString()

                                    client.post(NetworkConfig.getPrivateSendApiUrl()) {
                                        contentType(ContentType.Application.Json)
                                        setBody(jsonBody)
                                    }
                                } catch (e: Exception) { Log.e("PrivateSend", "Greška: ${e.message}") }
                            }
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Pošalji", tint = Color(0xFF2196F3))
                }
            }
        }
    }
}
