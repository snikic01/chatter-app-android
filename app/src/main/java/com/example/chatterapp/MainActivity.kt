package com.example.chatterapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ChatMessage(val username: String, val message: String, val date: String)

enum class Screen { LOGIN, REGISTER, CHAT }

class MainActivity : ComponentActivity() {

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
        setContent {
            MaterialTheme {
                var currentScreen by remember { mutableStateOf(Screen.LOGIN) }
                var messagesList by remember { mutableStateOf(listOf<ChatMessage>()) }
                var textInput by remember { mutableStateOf("") }
                var authErrorMessage by remember { mutableStateOf<String?>(null) }
                val coroutineScope = rememberCoroutineScope()
                val listState = rememberLazyListState()

                LaunchedEffect(currentScreen) {
                    if (currentScreen == Screen.CHAT) {
                        while (true) {
                            val fetched = fetchChatMessages()
                            if (fetched != null) {
                                messagesList = fetched
                            }
                            kotlinx.coroutines.delay(3000)
                        }
                    }
                }

                when (currentScreen) {
                    Screen.LOGIN -> {
                        AuthScreen(
                            isLogin = true,
                            errorMessage = authErrorMessage,
                            onActionClick = { user, pass ->
                                authErrorMessage = "Provera..."
                                coroutineScope.launch {
                                    if (handleAuth(user, pass, "login")) {
                                        currentUsername.value = user
                                        authErrorMessage = null
                                        currentScreen = Screen.CHAT
                                    } else {
                                        authErrorMessage = "Pogrešna šifra ili korisnik."
                                    }
                                }
                            },
                            onSwitchScreen = {
                                authErrorMessage = null
                                currentScreen = Screen.REGISTER
                            }
                        )
                    }
                    Screen.REGISTER -> {
                        AuthScreen(
                            isLogin = false,
                            errorMessage = authErrorMessage,
                            onActionClick = { user, pass ->
                                authErrorMessage = "Registracija..."
                                coroutineScope.launch {
                                    if (handleAuth(user, pass, "register")) {
                                        currentUsername.value = user
                                        authErrorMessage = null
                                        currentScreen = Screen.CHAT
                                    } else {
                                        authErrorMessage = "Greška! Ime zauzeto."
                                    }
                                }
                            },
                            onSwitchScreen = {
                                authErrorMessage = null
                                currentScreen = Screen.LOGIN
                            }
                        )
                    }
                    Screen.CHAT -> {
                        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
                            // Popravljen TopAppBar za Material 3 standard
                            TopAppBar(
                                title = { Text("Korisnik: ${currentUsername.value}", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            )

                            LazyColumn(
                                state = listState,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(messagesList) { msg ->
                                    ChatBubble(msg, isCurrentUser = msg.username == currentUsername.value)
                                }
                            }

                            Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 8.dp) {
                                Row(
                                    modifier = Modifier.padding(12.dp).navigationBarsPadding().imePadding(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextField(
                                        value = textInput,
                                        onValueChange = { textInput = it },
                                        placeholder = { Text("Ukucaj poruku...") },
                                        modifier = Modifier.weight(1f),
                                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            if (textInput.isNotBlank()) {
                                                val messageToSend = textInput
                                                textInput = ""
                                                coroutineScope.launch {
                                                    sendMessageToServer(currentUsername.value, messageToSend)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Pošalji", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun handleAuth(username: String, pass: String, actionType: String): Boolean {
        val url = "https://ts.net"
        return try {
            val rawJson = JSONObject().apply {
                put("action", actionType)
                put("username", username)
                put("password", pass)
            }.toString()

            val response: HttpResponse = withContext(Dispatchers.IO) {
                client.post(url) {
                    headers.append("Content-Type", "application/json")
                    setBody(rawJson)
                }
            }
            JSONObject(response.bodyAsText()).getString("status") == "success"
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun fetchChatMessages(): List<ChatMessage>? {
        val url = "https://ts.net"
        return try {
            val response: HttpResponse = withContext(Dispatchers.IO) { client.get(url) }
            val jsonObject = JSONObject(response.bodyAsText())
            if (jsonObject.getString("status") == "success") {
                val jsonArray = jsonObject.getJSONArray("messages")
                val parsed = mutableListOf<ChatMessage>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    parsed.add(ChatMessage(item.getString("username"), item.getString("message"), item.getString("date")))
                }
                parsed
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun sendMessageToServer(username: String, message: String): Boolean {
        val url = "https://ts.net"
        return try {
            val rawJson = JSONObject().apply {
                put("username", username)
                put("message", message)
            }.toString()

            val response: HttpResponse = withContext(Dispatchers.IO) {
                client.post(url) {
                    headers.append("Content-Type", "application/json")
                    setBody(rawJson)
                }
            }
            JSONObject(response.bodyAsText()).getString("status") == "success"
        } catch (e: Exception) {
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client.close()
    }
}

@Composable
fun AuthScreen(
    isLogin: Boolean,
    errorMessage: String?,
    onActionClick: (String, String) -> Unit,
    onSwitchScreen: () -> Unit
) {
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isLogin) "Dobrodošao nazad" else "Napravi nalog",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            errorMessage?.let {
                Text(text = it, color = if (it.contains("...")) Color.Gray else Color.Red, fontSize = 14.sp)
            }

            OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                label = { Text("Korisničko ime") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("Lozinka") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { if (usernameInput.isNotBlank() && passwordInput.isNotBlank()) onActionClick(usernameInput, passwordInput) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(text = if (isLogin) "Prijavi se" else "Registruj se", fontSize = 16.sp)
            }

            TextButton(onClick = onSwitchScreen) {
                Text(text = if (isLogin) "Nemaš nalog? Registruj se" else "Već imaš nalog? Prijavi se")
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage, isCurrentUser: Boolean) {
    val alignment = if (isCurrentUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer else Color.White
    val textColor = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer else Color.Black

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!isCurrentUser) {
            Text(
                text = msg.username,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
        Box(
            modifier = Modifier
                .background(bubbleColor, shape = RoundedCornerShape(12.dp))
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            Column {
                Text(text = msg.message, fontSize = 15.sp, color = textColor)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (msg.date.contains(" ")) msg.date.substringAfter(" ") else msg.date,
                    fontSize = 9.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
