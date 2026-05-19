package com.example.chatterapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    // Kreiramo mrežni klijent koristeći Android engine iz uvezene Ktor biblioteke
    private val client = HttpClient(Android)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Povezivanje sa NikicLab serverom...")
                    }
                }
            }

            // Pokrećemo asinhroni mrežni poziv u Compose životnom ciklusu
            LaunchedEffect(Unit) {
                fetchChatMessages()
            }
        }
    }

    private suspend fun fetchChatMessages() {
        val url = "https://ts.net"

        try {
            // Eksplicitno prebacujemo rad na IO nit namenjenu mrežnim zahtevima
            val response: HttpResponse = withContext(Dispatchers.IO) {
                client.get(url)
            }

            val responseText = response.bodyAsText()
            Log.d("ChatterAppAI", "Uspešan odgovor sa servera: $responseText")

        } catch (e: Exception) {
            Log.e("ChatterAppAI", "Greška pri povezivanju na Tailscale: ${e.localizedMessage}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client.close() // Zatvaramo konekciju radi oslobađanja RAM memorije
    }
}
