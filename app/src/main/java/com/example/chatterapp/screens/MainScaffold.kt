package com.example.chatterapp.screens

import com.example.chatterapp.data.DashboardViewModel

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.chatterapp.Tab
import com.example.chatterapp.data.ChatMessage
import com.example.chatterapp.data.AuthViewModel
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun MainScaffold(
    currentTab: Tab,
    onTabChange: (Tab) -> Unit,
    activeGroupId: Int,
    currentUsername: String,
    onLogoutClick: () -> Unit,
    messagesList: List<ChatMessage>,
    onMessagesListChange: (List<ChatMessage>) -> Unit,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    groupsList: List<com.example.chatterapp.screens.AndroidChatGroup>,
    onGroupChange: (Int) -> Unit,
    client: HttpClient,
    authViewModel: AuthViewModel,
    coroutineScope: CoroutineScope,
    listState: androidx.compose.foundation.lazy.LazyListState,
    sendChatMessage: suspend (String, String, Int) -> Boolean,
    dashboardViewModel: DashboardViewModel
) {
    // 🛠️ FIX 1: Promenljiva je vraćena na sam početak funkcije, pre Scaffold-a!
    var isInsidePrivateChat by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            val prikaziMeniZaGrupe = currentTab != Tab.GROUPS || activeGroupId == 0
            val prikaziMeniZaPrivatne = !isInsidePrivateChat

            if (prikaziMeniZaGrupe && prikaziMeniZaPrivatne) {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Početna") },
                        label = { Text("Početna") },
                        selected = currentTab == Tab.DASHBOARD,
                        onClick = { onTabChange(Tab.DASHBOARD) }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.List, contentDescription = "Grupe") },
                        label = { Text("Grupe") },
                        selected = currentTab == Tab.GROUPS,
                        onClick = { onTabChange(Tab.GROUPS) }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.MailOutline, contentDescription = "Poruke") },
                        label = { Text("Poruke") },
                        selected = currentTab == Tab.PRIVATE,
                        onClick = { onTabChange(Tab.PRIVATE) }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Prijatelji") },
                        label = { Text("Prijatelji") },
                        selected = currentTab == Tab.FRIENDS,
                        onClick = { onTabChange(Tab.FRIENDS) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentTab) {
                Tab.DASHBOARD -> {
                    DashboardScreen(
                        authViewModel = authViewModel,
                        onLogoutSuccess = onLogoutClick,
                        dashboardViewModel = dashboardViewModel
                    )
                }
                Tab.GROUPS -> {
                    var lokalniTextInput by remember(textInput) { mutableStateOf(textInput) }

                    GroupsScreen(
                        currentUsername = currentUsername,
                        messagesList = messagesList,
                        textInput = textInput,
                        onTextInputChange = onTextInputChange,
                        onSendMessageClick = {
                            if (textInput.isNotBlank() && activeGroupId != 0) {
                                coroutineScope.launch {
                                    val success = sendChatMessage(currentUsername, textInput, activeGroupId)
                                    if (success) {
                                        onTextInputChange("")
                                        try {
                                            val url = com.example.chatterapp.data.NetworkConfig.getChatUrl(activeGroupId)
                                            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                client.get(url)
                                            }
                                            val jsonResponse = JSONObject(response.bodyAsText())
                                            val jsonArray = jsonResponse.getJSONArray("messages")
                                            val list = mutableListOf<ChatMessage>()

                                            for (i in 0 until jsonArray.length()) {
                                                val obj = jsonArray.getJSONObject(i)
                                                val seenArray = obj.optJSONArray("seen_by")
                                                val seenList = mutableListOf<String>()
                                                if (seenArray != null) {
                                                    for (j in 0 until seenArray.length()) {
                                                        seenList.add(seenArray.getString(j))
                                                    }
                                                }
                                                list.add(
                                                    ChatMessage(
                                                        username = obj.getString("username"),
                                                        message = obj.getString("message"),
                                                        date = obj.getString("sent_at"),
                                                        seenBy = seenList
                                                    )
                                                )
                                            }
                                            onMessagesListChange(list)
                                            listState.animateScrollToItem(list.size)
                                        } catch (e: Exception) {
                                            Log.e("ChatterApp", "Greška pri osvežavanju nakon slanja: ${e.message}")
                                        }
                                    }
                                }
                            }
                        },
                        listState = listState,
                        activeGroupId = activeGroupId,
                        onGroupChange = onGroupChange,
                        groupsList = groupsList,
                        client = client
                    )
                }
                Tab.PRIVATE -> {
                    // 🛠️ FIX 2: Povezujemo onChatToggle callback koji direktno kontroliše sakrivanje menija!
                    PrivateScreen(
                        currentUsername = currentUsername,
                        client = client,
                        onChatToggle = { isInsidePrivateChat = it }
                    )
                }
                // 🛠️ FIX 3: Vraćena FRIENDS grana koja je falila i rušila celi build!
                Tab.FRIENDS -> {
                    FriendsScreen(
                        currentUsername = currentUsername,
                        client = client
                    )
                }
            }
        }
    }
}
