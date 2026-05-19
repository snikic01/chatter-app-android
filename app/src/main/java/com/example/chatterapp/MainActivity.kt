package com.example.chatterapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

// Klasa koja predstavlja jednu čet poruku na telefonu
data class ChatMessage(
    val username: String,
    val message: String,
    val date: String
)

class MainActivity : ComponentActivity() {

    private val client = HttpClient(Android)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // Stanje koje čuva listu poruka i status učitavanja
                var messagesList by remember { mutableStateOf(listOf<ChatMessage>()) }
                var isLoading by remember { mutableStateOf(true) }
                var errorMessage by remember { mutableStateOf<String?>(null) }

                // Pokretanje mrežnog zahteva pri paljenju ekrana
                LaunchedEffect(Unit) {
                    val url = "https://ts.net"
                    try {
                        val response: HttpResponse = withContext(Dispatchers.IO) {
                            client.get(url)
                        }
                        val responseText = response.bodyAsText()
                        Log.d("ChatterAppAI", "Server JSON: $responseText")

                        // Ručno parsujemo JSON preko ugrađene Android JSONObject klase
                        val jsonObject = JSONObject(responseText)
                        if (jsonObject.getString("status") == "success") {
                            val jsonArray = jsonObject.getJSONArray("messages")
                            val parsedMessages = mutableListOf<ChatMessage>()

                            for (i in 0 until jsonArray.length()) {
                                val item = jsonArray.getJSONObject(i)
                                parsedMessages.add(
                                    ChatMessage(
                                        username = item.getString("username"),
                                        message = item.getString("message"),
                                        date = item.getString("date")
                                    )
                                )
                            }
                            messagesList = parsedMessages
                        } else {
                            errorMessage = jsonObject.optString("message", "Nepoznata greška na serveru.")
                        }
                    } catch (e: Exception) {
                        Log.e("ChatterAppAI", "Mrežna greška: ${e.localizedMessage}")
                        errorMessage = "Greška pri povezivanju. Proveri Tailscale!"
                    } finally {
                        isLoading = false
                    }
                }

                // Glavni vizuelni raspored (UI)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF5F5F5) // Blago siva pozadina za moderan čet izgled
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        } else if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = Color.Red,
                                modifier = Modifier.align(Alignment.Center).padding(16.dp)
                            )
                        } else {
                            // Skrolujuća čet lista poruka
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(messagesList) { msg ->
                                    ChatBubble(msg)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client.close()
    }
}

// Komponenta za pojedinačni čet oblačić (Bubble)
@Composable
fun ChatBubble(msg: ChatMessage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Ime korisnika iznad oblačića
        Text(
            text = msg.username,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )
        // Oblačić sa tekstom poruke
        Box(
            modifier = Modifier
                .background(Color.White, shape = RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column {
                Text(text = msg.message, fontSize = 16.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                // Vreme slanja u donjem uglu oblačića
                Text(
                    text = msg.date,
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
