package com.example.chatterapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.chatterapp.screens.AuthScreenWrapper
import com.example.chatterapp.screens.MainScaffold
import com.example.chatterapp.data.SessionManager
import com.example.chatterapp.data.AuthViewModel
import com.example.chatterapp.data.ChatMessage
import com.example.chatterapp.data.NetworkConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class Screen { LOGIN, REGISTER, CHAT }
enum class Tab { DASHBOARD, GROUPS, PRIVATE, FRIENDS }

class MainActivity : ComponentActivity() {

    // Globalni mrežni klijent sa Tailscale SSL bajpasom
    private val client = HttpClient(Android) {
        engine {
            sslManager = { httpsURLConnection ->
                val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(
                    object : javax.net.ssl.X509TrustManager {
                        override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                        override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                    }
                )
                val sslContext = javax.net.ssl.SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                httpsURLConnection.sslSocketFactory = sslContext.socketFactory
                httpsURLConnection.hostnameVerifier = javax.net.ssl.HostnameVerifier { _, _ -> true }
            }
        }
    }

    private var currentUsername = mutableStateOf("")

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager = SessionManager(applicationContext)
        val authViewModel = AuthViewModel(sessionManager)
        val dashboardViewModel = DashboardViewModel(authViewModel)

        setContent {
            MaterialTheme {
                var currentScreen by remember {
                    mutableStateOf(if (sessionManager.isLoggedIn()) Screen.CHAT else Screen.LOGIN)
                }

                LaunchedEffect(Unit) {
                    if (sessionManager.isLoggedIn()) {
                        currentUsername.value = sessionManager.getSavedUsername() ?: ""
                    }
                }

                var currentTab by remember { mutableStateOf(Tab.DASHBOARD) }
                var messagesList by remember { mutableStateOf(listOf<ChatMessage>()) }
                var groupsList by remember { mutableStateOf(listOf<com.example.chatterapp.screens.AndroidChatGroup>()) }
                var textInput by remember { mutableStateOf("") }
                var activeGroupId by remember { mutableStateOf(0) }

                val coroutineScope = rememberCoroutineScope()
                val listState = rememberLazyListState()

                // Pozadinski poller (mrežna petlja) koji na 3 sekunde osvežava UI
                LaunchedEffect(currentScreen, currentTab, activeGroupId) {
                    if (currentScreen == Screen.CHAT && currentTab == Tab.GROUPS) {
                        withContext(Dispatchers.IO) {
                            while (true) {
                                if (currentScreen != Screen.CHAT || currentTab != Tab.GROUPS) break

                                try {
                                    val savedUser = sessionManager.getSavedUsername() ?: currentUsername.value

                                    // --- 1. OSVEŽAVANJE LISTE GRUPA ---
                                    val responseGroups = client.get(NetworkConfig.getGroupsUrl(savedUser))
                                    val jsonGroups = JSONObject(responseGroups.bodyAsText())

                                    if (jsonGroups.optBoolean("success", false)) {
                                        val array = jsonGroups.getJSONArray("groups")
                                        val list = mutableListOf<com.example.chatterapp.screens.AndroidChatGroup>()
                                        for (i in 0 until array.length()) {
                                            val obj = array.getJSONObject(i)
                                            val tvojaGrupaId = obj.getInt("id")
                                            val stvarniUnreadCount = if (tvojaGrupaId == activeGroupId) 0 else obj.optInt("unread_count", 0)

                                            list.add(com.example.chatterapp.screens.AndroidChatGroup(
                                                id = tvojaGrupaId,
                                                name = obj.getString("name"),
                                                isOwner = obj.optInt("is_owner", 0) == 1,
                                                unreadCount = stvarniUnreadCount,
                                                ownerName = obj.optString("owner_name", "")
                                            ))
                                        }
                                        withContext(Dispatchers.Main) { groupsList = list }
                                    }

                                    // --- 2. OSVEŽAVANJE PORUKA UNUTAR ČETA ---
                                    if (activeGroupId != 0) {
                                        val responseChat = client.get(NetworkConfig.getChatUrl(activeGroupId))
                                        val jsonChat = JSONObject(responseChat.bodyAsText())

                                        if (jsonChat.optBoolean("success", true) || jsonChat.has("messages")) {
                                            val jsonArray = jsonChat.getJSONArray("messages")
                                            val listMsg = mutableListOf<ChatMessage>()
                                            for (i in 0 until jsonArray.length()) {
                                                val obj = jsonArray.getJSONObject(i)
                                                val seenArray = obj.optJSONArray("seen_by")
                                                val seenList = mutableListOf<String>()
                                                if (seenArray != null) {
                                                    for (j in 0 until seenArray.length()) seenList.add(seenArray.getString(j))
                                                }
                                                listMsg.add(ChatMessage(
                                                    username = obj.optString("username", "Anonimno"),
                                                    message = obj.optString("message", ""),
                                                    date = obj.optString("sent_at", ""),
                                                    seenBy = seenList
                                                ))
                                            }
                                            withContext(Dispatchers.Main) { messagesList = listMsg }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("ChatterPolling", "Greška u petlji: ${e.message}")
                                }
                                kotlinx.coroutines.delay(3000)
                            }
                        }
                    }
                }

                // === ČIST ARHITEKTONSKI RUTIRANJE EKRANA ===
                when (currentScreen) {
                    Screen.LOGIN, Screen.REGISTER -> {
                        AuthScreenWrapper(
                            currentScreen = currentScreen,
                            onScreenChange = { currentScreen = it },
                            onTabChange = { currentTab = it },
                            onUsernameChange = { currentUsername.value = it },
                            sessionManager = sessionManager,
                            coroutineScope = coroutineScope,
                            handleAuth = ::handleAuth
                        )
                    }

                    Screen.CHAT -> {
                        MainScaffold(
                            currentTab = currentTab,
                            onTabChange = { currentTab = it },
                            activeGroupId = activeGroupId,
                            currentUsername = currentUsername.value,
                            onLogoutClick = {
                                currentUsername.value = ""
                                currentScreen = Screen.LOGIN
                            },
                            messagesList = messagesList,
                            onMessagesListChange = { messagesList = it },
                            textInput = textInput,
                            onTextInputChange = { textInput = it },
                            groupsList = groupsList,
                            onGroupChange = { idSign ->
                                coroutineScope.launch {
                                    val baseUrl = NetworkConfig.BASE_URL

                                    if (idSign == 0) {
                                        activeGroupId = 0
                                        messagesList = emptyList()
                                    } else if (idSign < 0) {
                                        val actualGroupId = kotlin.math.abs(idSign)
                                        val url = baseUrl + "api_groups.php"

                                        withContext(Dispatchers.IO) {
                                            try {
                                                val akcija =
                                                    if (actualGroupId >= 100) "leave" else "delete"
                                                val realId =
                                                    if (actualGroupId >= 100) actualGroupId / 100 else actualGroupId
                                                val jsonBody = JSONObject().apply {
                                                    put("action", akcija)
                                                    put("group_id", realId)
                                                    put("username", currentUsername.value)
                                                }.toString()

                                                client.post(url) {
                                                    contentType(ContentType.Application.Json)
                                                    setBody(jsonBody)
                                                }
                                            } catch (e: Exception) {
                                                Log.e("ChatterBUG", "Greška: ${e.message}")
                                            }
                                        }
                                        activeGroupId = 0
                                    } else {
                                        activeGroupId = idSign
                                        messagesList = emptyList()
                                        withContext(Dispatchers.IO) {
                                            try {
                                                val jsonBody = JSONObject().apply {
                                                    put("action", "mark")
                                                    put("username", currentUsername.value)
                                                    put("group_id", idSign)
                                                }.toString()

                                                client.post(NetworkConfig.getSeenUrl()) {
                                                    contentType(ContentType.Application.Json)
                                                    setBody(jsonBody)
                                                }
                                            } catch (e: Exception) {
                                                Log.e("ChatterSeen", "Greška: ${e.message}")
                                            }
                                        }
                                    }
                                }
                            },
                            client = client,
                            authViewModel = authViewModel,
                            coroutineScope = coroutineScope,
                            listState = listState,
                            sendChatMessage = ::sendChatMessage
                        )
                    }
                }
            }
        }
    }

    // Pomoćne funkcije za Ktor koje ruter poziva preko referenci
    private suspend fun handleAuth(user: String, pass: String, type: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    put("action", type)
                    put("username", user)
                    put("password", pass)
                }.toString()

                val response: HttpResponse = client.post("${NetworkConfig.BASE_URL}api_auth.php") {
                    contentType(ContentType.Application.Json)
                    setBody(jsonBody)
                }
                JSONObject(response.bodyAsText()).optBoolean("success", false)
            } catch (e: Exception) { false }
        }
    }

    private suspend fun sendChatMessage(user: String, msg: String, groupId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    put("group_id", groupId)
                    put("username", user)
                    put("message", msg)
                }.toString()

                val response: HttpResponse = client.post("${NetworkConfig.BASE_URL}api_send.php") {
                    contentType(ContentType.Application.Json)
                    setBody(jsonBody)
                }
                JSONObject(response.bodyAsText()).optBoolean("success", false)
            } catch (e: Exception) { false }
        }
    }
}

