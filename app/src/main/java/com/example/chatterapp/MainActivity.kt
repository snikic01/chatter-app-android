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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

data class ChatMessage(val username: String, val message: String, val date: String)

class MainActivity : ComponentActivity() {

    // Ktor klijent konfigurisan da dopušta Tailscale sertifikate
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

    private val currentUsername = "nikic" // Tvoj verifikovani username iz baze

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var messagesList by remember { mutableStateOf(listOf<ChatMessage>()) }
                var textInput by remember { mutableStateOf("") }
                val coroutineScope = rememberCoroutineScope()
                val listState = rememberLazyListState()

                // Funkcija za osvežavanje poruka
                fun refreshMessages() {
                    coroutineScope.launch {
                        val fetched = fetchChatMessages()
                        if (fetched != null) {
                            messagesList = fetched
                            if (messagesList.isNotEmpty()) {
                                listState.animateScrollToItem(messagesList.size - 1)
                            }
                        }
                    }
                }

                // Povuci poruke pri pokretanju
                LaunchedEffect(Unit) {
                    refreshMessages()
                }

                // Glavni UI Raspored
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF5F5F5))
                ) {
                    // 1. Lista poruka
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messagesList) { msg ->
                            ChatBubble(msg, isCurrentUser = msg.username == currentUsername)
                        }
                    }

                    // 2. Traka na dnu za kucanje i slanje poruka
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .navigationBarsPadding()
                                .imePadding(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                placeholder = { Text("Ukucaj poruku...") },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (textInput.isNotBlank()) {
                                        val messageToSend = textInput
                                        textInput = ""
                                        coroutineScope.launch {
                                            val success = sendMessageToServer(currentUsername, messageToSend)
                                            if (success) {
                                                refreshMessages()
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Pošalji",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun fetchChatMessages(): List<ChatMessage>? {
        val url = "https://ts.net"
        return try {
            val response: HttpResponse = withContext(Dispatchers.IO) { client.get(url) }
            val responseText = response.bodyAsText()
            val jsonObject = JSONObject(responseText)
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
            Log.e("ChatterAppAI", "Greška osvežavanja: ${e.localizedMessage}")
            null
        }
    }

    private suspend fun sendMessageToServer(username: String, message: String): Boolean {
        // ISPRAVLJENO: Vraćena ispravna Tailscale putanja do api_send.php ekrana
        val url = "https://ts.net"
        return try {
            val rawJson = "{\"username\":\"$username\",\"message\":\"$message\"}"

            val response: HttpResponse = withContext(Dispatchers.IO) {
                client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(rawJson)
                }
            }
            val responseText = response.bodyAsText()
            Log.d("ChatterAppAI", "Odgovor servera pri slanju: $responseText")

            JSONObject(responseText).getString("status") == "success"
        } catch (e: Exception) {
            Log.e("ChatterAppAI", "Greška pri slanju sa telefona: ${e.localizedMessage}")
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client.close()
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
