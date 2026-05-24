package com.example.chatterapp.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.example.chatterapp.data.NetworkConfig

// Modeli podataka koji preslikavaju tabele iz tvog PHP veb koda
data class AndroidAdminIpLog(
    val username: String,
    val ipAddress: String,
    val lastLogin: String
)

data class AndroidAdminUser(
    val id: Int,
    val username: String,
    val isBanned: Boolean
)

data class AndroidAdminChat(
    val nameA: String,
    val nameB: String,
    val lastMsg: String
)

data class AndroidAdminGroup(
    val id: Int,
    val name: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLogsScreen(
    currentUsername: String,
    client: HttpClient,
    onBackClick: () -> Unit
) {
    // Kontrola tabova na vrhu (IP Logovi = 0, Korisnici = 1, Četovi = 2, Grupe = 3)
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("IP Logovi", "Korisnička Kontrola", "Privatni Četovi", "Ghost Grupe")
    val coroutineScope = rememberCoroutineScope()

    // Reaktivna stanja koja pune LazyColumn elemente na ekranu
    var logsList by remember { mutableStateOf(listOf<AndroidAdminIpLog>()) }
    var usersList by remember { mutableStateOf(listOf<AndroidAdminUser>()) }
    var chatsList by remember { mutableStateOf(listOf<AndroidAdminChat>()) }
    var groupsList by remember { mutableStateOf(listOf<AndroidAdminGroup>()) }
    var bannedIpsList by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(true) }

    // Centralna funkcija koja osvežava sve tabele povlačenjem čistog JSON-a sa servera
    val ucitajSvePodatkeSaServera = {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // Koristimo tvoj NetworkConfig i dodajemo vremenski žig protiv Ktor keša!
                val url = NetworkConfig.getAdminLogsApiUrl(currentUsername) + "&_nocache=" + System.currentTimeMillis()
                val response = client.get(url)
                val responseText = response.bodyAsText()
                val json = JSONObject(responseText)

                if (json.optBoolean("success", false)) {
                    // 1. Parsiranje IP lokacija korisnika
                    val tempLogs = mutableListOf<AndroidAdminIpLog>()
                    val arrLogs = json.getJSONArray("logs")
                    for (i in 0 until arrLogs.length()) {
                        val obj = arrLogs.getJSONObject(i)
                        tempLogs.add(
                            AndroidAdminIpLog(
                                username = obj.getString("username"),
                                ipAddress = obj.getString("ip_address"),
                                lastLogin = obj.getString("last_login")
                            )
                        )
                    }

                    // 2. Parsiranje korisnika iz Ban liste
                    val tempUsers = mutableListOf<AndroidAdminUser>()
                    val arrUsers = json.getJSONArray("users")
                    for (i in 0 until arrUsers.length()) {
                        val obj = arrUsers.getJSONObject(i)
                        tempUsers.add(
                            AndroidAdminUser(
                                id = obj.getInt("id"),
                                username = obj.getString("username"),
                                isBanned = obj.optInt("is_banned", 0) == 1
                            )
                        )
                    }

                    // 3. Parsiranje monitoringa privatnih četova
                    val tempChats = mutableListOf<AndroidAdminChat>()
                    val arrChats = json.getJSONArray("chats")
                    for (i in 0 until arrChats.length()) {
                        val obj = arrChats.getJSONObject(i)
                        tempChats.add(
                            AndroidAdminChat(
                                nameA = obj.getString("name_a"),
                                nameB = obj.getString("name_b"),
                                lastMsg = obj.getString("last_msg")
                            )
                        )
                    }

                    // 4. Parsiranje aktivnih grupa baze
                    val tempGroups = mutableListOf<AndroidAdminGroup>()
                    val arrGroups = json.getJSONArray("groups")
                    for (i in 0 until arrGroups.length()) {
                        val obj = arrGroups.getJSONObject(i)
                        tempGroups.add(
                            AndroidAdminGroup(
                                id = obj.getInt("id"),
                                name = obj.getString("name")
                            )
                        )
                    }

                    // 5. Parsiranje banovanih IP adresa za provere na dugmićima
                    val tempBannedIps = mutableListOf<String>()
                    val arrIps = json.getJSONArray("banned_ips")
                    for (i in 0 until arrIps.length()) {
                        tempBannedIps.add(arrIps.getString(i))
                    }

                    withContext(Dispatchers.Main) {
                        logsList = tempLogs
                        usersList = tempUsers
                        chatsList = tempChats
                        groupsList = tempGroups
                        bannedIpsList = tempBannedIps
                    }
                }
            } catch (e: Exception) {
                Log.e("AdminTerminalBUG", "Greška pri osvežavanju terminala: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        ucitajSvePodatkeSaServera()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ROOT TERMINAL", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF6C5CE7))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF1E1E24))
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF2A2A35),
                contentColor = Color.White
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF6C5CE7))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            for (log in logsList) {
                                item {
                                    val ip = log.ipAddress
                                    val isIpBanned = bannedIpsList.contains(ip)

                                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A35))) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = log.username, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                                Text(text = "IP: $ip", color = Color(0xFFA29BFE), fontSize = 13.sp)
                                                Text(text = "Vreme: ${log.lastLogin}", color = Color.Gray, fontSize = 11.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        try {
                                                            val url = NetworkConfig.getAdminLogsApiUrl(currentUsername)
                                                            val jsonBody = JSONObject().apply {
                                                                put("action", "ip_ban_toggle")
                                                                put("username", currentUsername)
                                                                put("target_ip", ip)
                                                                put("is_already_banned", isIpBanned)
                                                            }.toString()

                                                            client.post(url) {
                                                                contentType(ContentType.Application.Json)
                                                                setBody(jsonBody)
                                                            }
                                                            ucitajSvePodatkeSaServera()
                                                        } catch (e: Exception) { Log.e("AdminIPBan", "Greška: ${e.message}") }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isIpBanned) Color(0xFF2ED573) else Color(0xFFFF4757)
                                                ),
                                                shape = RoundedCornerShape(20.dp)
                                            ) {
                                                Text(if (isIpBanned) "UNBAN" else "BAN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> { // === TAB 2: BAN LISTA (KORISNIČKA KONTROLA) ===
                            for (user in usersList) {
                                item {
                                    val uId = user.id
                                    val isBanned = user.isBanned

                                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A35))) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(text = user.username, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                                Text(
                                                    text = if (isBanned) "Banovan" else "Aktivan",
                                                    color = if (isBanned) Color(0xFFFF4757) else Color(0xFF2ED573),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        try {
                                                            val url = NetworkConfig.getAdminLogsApiUrl(currentUsername)
                                                            val jsonBody = JSONObject().apply {
                                                                put("action", "toggle_ban")
                                                                put("username", currentUsername)
                                                                put("target_user_id", uId)
                                                                put("current_status", if (isBanned) 1 else 0)
                                                            }.toString()

                                                            client.post(url) {
                                                                contentType(ContentType.Application.Json)
                                                                setBody(jsonBody)
                                                            }
                                                            ucitajSvePodatkeSaServera()
                                                        } catch (e: Exception) { Log.e("AdminUserBan", "Greška: ${e.message}") }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isBanned) Color(0xFF2ED573) else Color(0xFFFF4757)
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(if (isBanned) "UNBAN" else "BAN", fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> { // === TAB 3: PRIVATE CHAT MONITORING ===
                            for (chat in chatsList) {
                                item {
                                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A35))) {
                                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                            Text(
                                                text = "${chat.nameA} ↔ ${chat.nameB}",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 16.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Zadnji prenos: ${chat.lastMsg}",
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        3 -> { // === TAB 4: GHOST MONITORING GRUPE ===
                            for (group in groupsList) {
                                item {
                                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A35))) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = group.name,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFFD200),
                                                fontSize = 16.sp
                                            )
                                            Text(
                                                text = "GHOST ENTER ACTIVE",
                                                color = Color.Gray,
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
            }
        }
    }
}
