package com.example.chatterapp

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatterapp.screens.DashboardScreen
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class Screen { LOGIN, REGISTER, MAIN_APP }

class MainActivity : ComponentActivity() {

    // Ktor klijent sa Tailscale SSL bajpasom
    val client = HttpClient(Android) {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var currentScreen by remember { mutableStateOf(Screen.LOGIN) }
                var loggedInUser by remember { mutableStateOf("") }
                var authErrorMessage by remember { mutableStateOf<String?>(null) }
                val coroutineScope = rememberCoroutineScope()

                when (currentScreen) {
                    Screen.LOGIN -> {
                        AuthScreen(
                            isLogin = true,
                            errorMessage = authErrorMessage,
                            onActionClick = { user, pass ->
                                authErrorMessage = "Provera..."
                                coroutineScope.launch {
                                    if (handleAuth(user, pass, "login")) {
                                        loggedInUser = user
                                        authErrorMessage = null
                                        currentScreen = Screen.MAIN_APP
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
                                        loggedInUser = user
                                        authErrorMessage = null
                                        currentScreen = Screen.MAIN_APP
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
                    Screen.MAIN_APP -> {
                        // ČISTO POZIVANJE: MainActivity otvara Dashboard iz screens foldera!
                        DashboardScreen(username = loggedInUser)
                    }
                }
            }
        }
    }

    private suspend fun handleAuth(username: String, pass: String, actionType: String): Boolean {
        val url = "https://nikiclab01.tailfd4e2c.ts.net/php/chatter-app-3.0/api_auth.php"
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

    override fun onDestroy() {
        super.onDestroy()
        client.close()
    }
}

@Composable
fun AuthScreen(isLogin: Boolean, errorMessage: String?, onActionClick: (String, String) -> Unit, onSwitchScreen: () -> Unit) {
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = if (isLogin) "Dobrodošao nazad" else "Napravi nalog", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            errorMessage?.let { Text(text = it, color = if (it.contains("...")) Color.Gray else Color.Red, fontSize = 14.sp) }
            OutlinedTextField(value = usernameInput, onValueChange = { usernameInput = it }, label = { Text("Korisničko ime") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = passwordInput, onValueChange = { passwordInput = it }, label = { Text("Lozinka") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Button(onClick = { if (usernameInput.isNotBlank() && passwordInput.isNotBlank()) onActionClick(usernameInput, passwordInput) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text(text = if (isLogin) "Prijavi se" else "Registruj se", fontSize = 16.sp)
            }
            TextButton(onClick = onSwitchScreen) { Text(text = if (isLogin) "Nemaš nalog? Registruj se" else "Već imaš nalog? Prijavi se") }
        }
    }
}
