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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Uvozi tvoja dva ekrana i nove klase iz data foldera
import com.example.chatterapp.screens.DashboardScreen
import com.example.chatterapp.screens.GroupsScreen
import com.example.chatterapp.data.SessionManager
import com.example.chatterapp.data.AuthViewModel

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
import com.example.chatterapp.data.ChatMessage

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

        // Inicijalizujemo SessionManager i AuthViewModel
        val sessionManager = SessionManager(applicationContext)
        val authViewModel = AuthViewModel(sessionManager)

        setContent {
            MaterialTheme {
                // Ako menadžer kaže da imamo sačuvan token/korisnika, preskačemo Login ekran automatski
                var currentScreen by remember {
                    mutableStateOf(if (sessionManager.isLoggedIn()) Screen.CHAT else Screen.LOGIN)
                }

                // Ako je korisnik već ulogovan, povlačimo njegovo sačuvano ime u stanje aplikacije
                LaunchedEffect(Unit) {
                    if (sessionManager.isLoggedIn()) {
                        currentUsername.value = sessionManager.getSavedUsername() ?: ""
                    }
                }

                var currentTab by remember { mutableStateOf(Tab.DASHBOARD) }
                var messagesList by remember { mutableStateOf(listOf<ChatMessage>()) }
                var groupsList by remember { mutableStateOf(listOf<com.example.chatterapp.screens.AndroidChatGroup>()) }
                var textInput by remember { mutableStateOf("") }
                var authErrorMessage by remember { mutableStateOf<String?>(null) }
                var activeGroupId by remember { mutableStateOf(0) }

                val coroutineScope = rememberCoroutineScope()
                val listState = rememberLazyListState()

                // Polling za automatsko osvežavanje poruka na svake 3 sekunde
                // Polling za automatsko osvežavanje poruka osiguran od pucanja
                LaunchedEffect(currentScreen, currentTab, activeGroupId) {
                    if (currentScreen == Screen.CHAT && currentTab == Tab.GROUPS) {
                        while (true) {
                            try {
                                val baseUrl = "http://" + "nikiclab01.tailfd4e2c.ts.net:8080/chatter-app-3.0/"
                                // Čitamo direktno iz sesije da ime nikada ne ode prazno na server!
                                val savedUser = sessionManager.getSavedUsername() ?: currentUsername.value

                                if (activeGroupId == 0) {
                                    // 1. Povlačimo sve grupe sa servera
                                    val url = baseUrl + "api_groups.php?action=list&username=" + savedUser
                                    val response = client.get(url)
                                    val jsonResponse = JSONObject(response.bodyAsText())

                                    if (jsonResponse.optBoolean("success", false)) {
                                        val array = jsonResponse.getJSONArray("groups")
                                        val filteredList = mutableListOf<com.example.chatterapp.screens.AndroidChatGroup>()

                                        for (i in 0 until array.length()) {
                                            val obj = array.getJSONObject(i)

                                            // POPRAVLJENO MAPIRANJE VLASNIŠTVA: Čita 1 ili 0 iz MariaDB baze!
                                            val isOwner = obj.optInt("is_owner", 0) == 1

                                            filteredList.add(
                                                com.example.chatterapp.screens.AndroidChatGroup(
                                                    id = obj.getInt("id"),
                                                    name = obj.getString("name"),
                                                    isOwner = isOwner,
                                                    unreadCount = obj.optInt("unread_count", 0)
                                                )
                                            )
                                        }
                                        groupsList = filteredList
                                    }
                                } else {
                                    // 2. Ako je čet otvoren, osvežavamo poruke i seen status
                                    val url = baseUrl + "api_chat.php?group_id=" + activeGroupId
                                    val response = client.get(url)
                                    val jsonResponse = JSONObject(response.bodyAsText())
                                    if (jsonResponse.optBoolean("success", true) || jsonResponse.has("messages")) {
                                        val jsonArray = jsonResponse.getJSONArray("messages")
                                        val list = mutableListOf<com.example.chatterapp.data.ChatMessage>()
                                        for (i in 0 until jsonArray.length()) {
                                            val obj = jsonArray.getJSONObject(i)
                                            val seenArray = obj.optJSONArray("seen_by")
                                            val seenList = mutableListOf<String>()
                                            if (seenArray != null) {
                                                for (j in 0 until seenArray.length()) { seenList.add(seenArray.getString(j)) }
                                            }
                                            list.add(
                                                com.example.chatterapp.data.ChatMessage(
                                                    username = obj.optString("username", "Anonimno"),
                                                    message = obj.optString("message", ""),
                                                    date = obj.optString("sent_at", ""),
                                                    seenBy = seenList
                                                )
                                            )
                                        }
                                        messagesList = list
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("ChatterPolling", "Greška u mrežnoj petlji: ${e.message}")
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
                                        // Uspešan login: pamtimo sesiju lokalno
                                        sessionManager.saveSession(user, "generisani_token_ili_id")
                                        currentUsername.value = user
                                        authErrorMessage = null
                                        currentScreen = Screen.CHAT
                                        currentTab = Tab.DASHBOARD
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
                                        // Uspešna registracija: pamtimo sesiju lokalno
                                        sessionManager.saveSession(user, "generisani_token_ili_id")
                                        currentUsername.value = user
                                        authErrorMessage = null
                                        currentScreen = Screen.CHAT
                                        currentTab = Tab.DASHBOARD
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
                        Scaffold(
                            bottomBar = {
                                NavigationBar(containerColor = Color.White) {
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.Home, contentDescription = "Početna") },
                                        label = { Text("Početna") },
                                        selected = currentTab == Tab.DASHBOARD,
                                        onClick = { currentTab = Tab.DASHBOARD }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.List, contentDescription = "Grupe") },
                                        label = { Text("Grupe") },
                                        selected = currentTab == Tab.GROUPS,
                                        onClick = { currentTab = Tab.GROUPS }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.MailOutline, contentDescription = "Poruke") },
                                        label = { Text("Poruke") },
                                        selected = currentTab == Tab.PRIVATE,
                                        onClick = { currentTab = Tab.PRIVATE }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.Person, contentDescription = "Prijatelji") },
                                        label = { Text("Prijatelji") },
                                        selected = currentTab == Tab.FRIENDS,
                                        onClick = { currentTab = Tab.FRIENDS }
                                    )
                                }
                            }
                        ) { paddingValues ->
                            Box(modifier = Modifier.padding(paddingValues)) {
                                when (currentTab) {
                                    Tab.DASHBOARD -> {
                                        DashboardScreen(
                                            authViewModel = authViewModel,
                                            onLogoutSuccess = {
                                                currentUsername.value = ""
                                                currentScreen = Screen.LOGIN
                                            }
                                        )
                                    }
                                    Tab.GROUPS -> {
                                        GroupsScreen(
                                            currentUsername = currentUsername.value,
                                            messagesList = messagesList,
                                            textInput = textInput,
                                            onTextInputChange = { textInput = it },
                                            onSendMessageClick = {
                                                if (textInput.isNotBlank() && activeGroupId != 0) {
                                                    coroutineScope.launch {
                                                        val success = sendChatMessage(currentUsername.value, textInput, activeGroupId)
                                                        if (success) {
                                                            textInput = ""

                                                            // POPRAVLJENO: Koristimo $activeGroupId umesto fiksnog broja 8!
                                                            val url = "http://ts.net"
                                                            try {
                                                                val response = client.get(url)
                                                                val jsonResponse = JSONObject(response.bodyAsText())
                                                                val jsonArray = jsonResponse.getJSONArray("messages")
                                                                val list = mutableListOf<com.example.chatterapp.data.ChatMessage>()
                                                                for (i in 0 until jsonArray.length()) {
                                                                    val obj = jsonArray.getJSONObject(i)

                                                                    // Čitamo i viđeno listu da seen ne nestane pri brzom osvežavanju
                                                                    val seenArray = obj.optJSONArray("seen_by")
                                                                    val seenList = mutableListOf<String>()
                                                                    if (seenArray != null) {
                                                                        for (j in 0 until seenArray.length()) { seenList.add(seenArray.getString(j)) }
                                                                    }

                                                                    list.add(
                                                                        com.example.chatterapp.data.ChatMessage(
                                                                            username = obj.getString("username"),
                                                                            message = obj.getString("message"),
                                                                            date = obj.getString("sent_at"),
                                                                            seenBy = seenList
                                                                        )
                                                                    )
                                                                }
                                                                messagesList = list
                                                                listState.animateScrollToItem(messagesList.size)
                                                            } catch (e: Exception) { }
                                                        }
                                                    }
                                                }
                                            },
                                            listState = listState,
                                            activeGroupId = activeGroupId,
                                            onGroupChange = { idSign ->
                                                coroutineScope.launch {
                                                    // Čista, spojena HTTP putanja na portu 8080 bez dupliranja fajlova
                                                    val baseUrl = "http://" + "nikiclab01.tailfd4e2c.ts.net:8080/chatter-app-3.0/"

                                                    if (idSign == 0) {
                                                        // Korisnik je kliknuo na dugme "Nazad" unutar četa
                                                        activeGroupId = 0
                                                        messagesList = emptyList()
                                                    } else if (idSign < 0) {
                                                        // DETEKTOVANA AKCIJA NAD GRUPOM (BRISANJE ILI NAPUŠTANJE)
                                                        val actualGroupId = kotlin.math.abs(idSign)
                                                        val url = baseUrl + "api_groups.php"

                                                        withContext(Dispatchers.IO) {
                                                            try {
                                                                if (actualGroupId >= 100) {
                                                                    // KORISNIK NAPUŠTA GRUPU (Deli se sa 100 da se dobije realan ID)
                                                                    val realId = actualGroupId / 100
                                                                    val jsonBody = JSONObject().apply {
                                                                        put("action", "leave")
                                                                        put("group_id", realId)
                                                                        // POTPUNO DINAMIČKI: Šaljemo trenutno ime, server sam nalazi ID u bazi!
                                                                        put("username", currentUsername.value)
                                                                    }.toString()

                                                                    client.post(url) {
                                                                        contentType(ContentType.Application.Json)
                                                                        setBody(jsonBody)
                                                                    }
                                                                } else {
                                                                    // VLASNIK BRIŠE CELU GRUPU TRAJNO IZ BAZE
                                                                    val jsonBody = JSONObject().apply {
                                                                        put("action", "delete")
                                                                        put("group_id", actualGroupId)
                                                                        // POTPUNO DINAMIČKI: Šaljemo trenutno ime, server sam nalazi ID u bazi!
                                                                        put("username", currentUsername.value)
                                                                    }.toString()

                                                                    client.post(url) {
                                                                        contentType(ContentType.Application.Json)
                                                                        setBody(jsonBody)
                                                                    }
                                                                }
                                                            } catch (e: Exception) { }
                                                        }
                                                        activeGroupId = 0 // Osvežava ekran i vraća na listu grupa koja će sada biti očišćena!
                                                    } else {
                                                        // KORISNIK OTVARA OBIČAN ČET GRUPE (ID je pozitivan, npr. 8 ili 19)
                                                        activeGroupId = idSign
                                                        messagesList = emptyList()

                                                        // ŠALJEMO ISPRAVAN SEEN STATUS PREKO DINAMIČKOG IMENA KORISNIKA
                                                        withContext(Dispatchers.IO) {
                                                            try {
                                                                val seenUrl = baseUrl + "api_seen.php"
                                                                val jsonBody = JSONObject().apply {
                                                                    put("action", "mark")
                                                                    // POTPUNO DINAMIČKI: Šaljemo trenutno ime ulogovanog korisnika
                                                                    put("username", currentUsername.value)
                                                                    put("group_id", idSign)
                                                                }.toString()

                                                                client.post(seenUrl) {
                                                                    contentType(ContentType.Application.Json)
                                                                    setBody(jsonBody)
                                                                }
                                                            } catch (e: Exception) {
                                                                Log.e("ChatterSeen", "Greška pri slanju seen statusa: ${e.message}")
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            groupsList = groupsList
                                        )
                                    }
                                    Tab.PRIVATE -> {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("Privatne Poruke (U izradi)")
                                        }
                                    }
                                    Tab.FRIENDS -> {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("Prijatelji (U izradi)")
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

    // Pomoćna funkcija za autentifikaciju preko PHP backend-a
    private suspend fun handleAuth(user: String, pass: String, type: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://nikiclab01.tailfd4e2c.ts.net/php/chatter-app-3.0/api_auth.php"
                val jsonBody = JSONObject().apply {
                    put("action", type)
                    put("username", user)
                    put("password", pass)
                }.toString()

                val response: HttpResponse = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(jsonBody)
                }

                val responseText = response.bodyAsText()
                Log.d("ChatterAuth", "Response: $responseText")

                val jsonResponse = JSONObject(responseText)
                jsonResponse.optBoolean("success", false)
            } catch (e: Exception) {
                Log.e("ChatterAuth", "Error: ${e.message}")
                false
            }
        }
    }

    // Pomoćna funkcija za povlačenje poruka iz grupe preko ispravnog api_chat.php fajla
    private suspend fun fetchChatMessages(): List<ChatMessage>? {
        return withContext(Dispatchers.IO) {
            try {
                // PROMENJENO: putanja sada gadja tvoj ispravan api_chat.php fajl
                val url = "https://nikiclab01.tailfd4e2c.ts.net/php/chatter-app-3.0/api_chat.php"
                val response: HttpResponse = client.get(url)
                val responseText = response.bodyAsText()

                val jsonResponse = JSONObject(responseText)

                // Prilagodjavamo proveru: ako tvoj PHP vraca direktno niz ili proverava success
                if (jsonResponse.optBoolean("success", true) || jsonResponse.has("messages")) {
                    val jsonArray = jsonResponse.getJSONArray("messages")
                    val list = mutableListOf<ChatMessage>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        list.add(
                            ChatMessage(
                                username = obj.optString("username", "Anonimno"),
                                message = obj.optString("message", ""),
                                date = obj.optString("sent_at", obj.optString("date", ""))
                            )
                        )
                    }
                    list
                } else null
            } catch (e: Exception) {
                Log.e("ChatterChat", "Fetch error: ${e.message}")
                null
            }
        }
    }

    // Pomoćna funkcija za slanje poruke preko ispravnog api_send.php fajla
    private suspend fun sendChatMessage(user: String, msg: String, groupId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://nikiclab01.tailfd4e2c.ts.net/php/chatter-app-3.0/api_send.php"

                // 1. Pakujemo podatke u čist i pravilan JSON objekat koji server traži
                val jsonBody = JSONObject().apply {
                    put("group_id", groupId)
                    put("username", user)
                    put("message", msg)
                }.toString()

                // 2. Šaljemo ga kroz Ktor klijent uz eksplicitno postavljanje Content-Type-a
                val response: HttpResponse = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(jsonBody)
                }

                // 3. Čitamo odgovor sa servera i proveravamo uspeh
                val jsonResponse = JSONObject(response.bodyAsText())
                jsonResponse.optBoolean("success", false)
            } catch (e: Exception) {
                Log.e("ChatterSend", "Greška pri slanju poruke: ${e.message}")
                false
            }
        }
    }


}

// Jednostavan zajednički Composable za Login i Register ekrane
@Composable
fun AuthScreen(
    isLogin: Boolean,
    errorMessage: String?,
    onActionClick: (String, String) -> Unit,
    onSwitchScreen: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isLogin) "Prijavi se na Chatter" else "Kreiraj Chatter nalog",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Korisničko ime") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Lozinka") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = Color.Red, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onActionClick(username, password) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(if (isLogin) "Prijavi se" else "Registruj se")
        }

        TextButton(onClick = onSwitchScreen) {
            Text(if (isLogin) "Nemaš nalog? Registruj se" else "Već imaš nalog? Prijavi se")
        }
    }
}
