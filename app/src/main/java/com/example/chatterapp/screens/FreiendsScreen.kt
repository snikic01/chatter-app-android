package com.example.chatterapp.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// Model podataka za stabilno parsiranje prijatelja i zahteva
data class AndroidFriend(
    val id: Int,
    val username: String,
    val isOnline: Boolean
)

data class AndroidFriendRequest(
    val id: Int,
    val username: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    currentUsername: String,
    client: HttpClient
) {
    var friendsList by remember { mutableStateOf(listOf<AndroidFriend>()) }
    var requestsList by remember { mutableStateOf(listOf<AndroidFriendRequest>()) }
    var suggestionsList by remember { mutableStateOf(listOf<AndroidFriend>()) }

    var localSearchQuery by remember { mutableStateOf("") } // Filter trenutnih prijatelja
    var newFriendQuery by remember { mutableStateOf("") }     // Unos za slanje novog zahteva
    var userSuggestions by remember { mutableStateOf<List<String>>(emptyList()) } // Sugestije u searchbaru

    var infoMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // Saltanje predloga
    var trenutnaCetiriPredloga by remember { mutableStateOf(listOf<AndroidFriend>()) }

    val coroutineScope = rememberCoroutineScope()

    // Pametni lokalni filter za trenutne prijatelje
    val filtriraniPrijatelji = friendsList.filter {
        it.username.contains(localSearchQuery, ignoreCase = true)
    }
    // 🛠 PANCIRNI TAJMER ZA ŠALTANJE (Na svake 3.5 sekunde):
    // Ovaj blok radi potpuno samostalno u RAM memoriji i garantuje konstantno mešanje!
    LaunchedEffect(suggestionsList) {
        if (suggestionsList.isNotEmpty()) {
            while (true) {
                // Uzimamo trenutno stanje liste iz memorije, promešamo ga i izvučemo TAČNO 4 čoveka
                trenutnaCetiriPredloga = suggestionsList.shuffled().take(4)

                // ⏱️ Tajmer kuca na svake 3.5 sekunde
                delay(3500)
            }
        } else {
            trenutnaCetiriPredloga = emptyList()
        }
    }

    // --- 1. JEDINI I STABILNI POLING ZA PRIJATELJE I AUTOMATSKO ŠALTANJE PREPORUKA ---
    //  REŠENJE: Spajamo mrežni poler i tajmer za šaltanje u jednu nit kako se rute ne bi sudarale!
    LaunchedEffect(Unit) {
        while (true) {
            try {
                // 1. Poziv za prijatelje i zahteve
                val url = NetworkConfig.getFriendsUrl(currentUsername)
                val response = withContext(Dispatchers.IO) { client.get(url) }
                val json = JSONObject(response.bodyAsText())

                if (json.optBoolean("success", false)) {
                    val arrFriends = json.getJSONArray("friends")
                    val tempFriends = mutableListOf<AndroidFriend>()
                    for (i in 0 until arrFriends.length()) {
                        val obj = arrFriends.getJSONObject(i)
                        val fIdStr = obj.optString("id", "0")
                        tempFriends.add(
                            AndroidFriend(
                                id = fIdStr.toIntOrNull() ?: 0,
                                username = obj.getString("username"),
                                isOnline = obj.optInt("is_online", 0) == 1
                            )
                        )
                    }

                    val arrReq = json.getJSONArray("requests")
                    val tempReq = mutableListOf<AndroidFriendRequest>()
                    for (i in 0 until arrReq.length()) {
                        val obj = arrReq.getJSONObject(i)
                        val rIdStr = obj.optString("id", "0")
                        tempReq.add(
                            AndroidFriendRequest(
                                id = rIdStr.toIntOrNull() ?: 0,
                                username = obj.getString("username")
                            )
                        )
                    }

                    friendsList = tempFriends
                    requestsList = tempReq
                }

                // 2. POZIV ZA PREPORUKE (LJUDE KOJE MOŽDA POZNAJEŠ)
                val urlSugg = NetworkConfig.getFriendSuggestionsUrl(currentUsername)
                val responseSugg = withContext(Dispatchers.IO) { client.get(urlSugg) }
                val jsonSugg = JSONObject(responseSugg.bodyAsText())

                if (jsonSugg.optBoolean("success", false)) {
                    val arrSugg = jsonSugg.getJSONArray("suggestions")
                    val tempSugg = mutableListOf<AndroidFriend>()

                    for (i in 0 until arrSugg.length()) {
                        val obj = arrSugg.getJSONObject(i)
                        val sIdStr = obj.optString("id", "0")

                        tempSugg.add(
                            AndroidFriend(
                                id = sIdStr.toIntOrNull() ?: 0,
                                username = obj.getString("username"),
                                isOnline = obj.optInt("is_online", 0) == 1
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        suggestionsList = tempSugg

                    }
                }
            } catch (e: Exception) {
                Log.e("ChatterFriends", "Greška u poleru prijatelja: ${e.message}")
            }
            delay(3000) // Okretanje ciklusa i novo šaltanje ljudi na svake 3 sekunde!
        }
    }

    LaunchedEffect(infoMessage) {
        if (infoMessage != null) {
            delay(4000)
            infoMessage = null
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Prijatelji", fontWeight = FontWeight.Bold) }) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F9FA))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === DEO 1: PRETRAGA I SLANJE NOVOG ZAHTEVA SA AUTODOPUNOM ===
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Pronađi novog prijatelja", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = newFriendQuery,
                                    onValueChange = { unetiTekst ->
                                        newFriendQuery = unetiTekst

                                        if (unetiTekst.trim().length >= 2) {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                try {
                                                    val url = NetworkConfig.getSearchUsersUrl(0, unetiTekst.trim()) + "&username=$currentUsername"
                                                    val response = client.get(url)
                                                    val json = JSONObject(response.bodyAsText())

                                                    if (json.optBoolean("success", false)) {
                                                        val array = json.getJSONArray("users")
                                                        val tempList = mutableListOf<String>()
                                                        for (i in 0 until array.length()) {
                                                            tempList.add(array.getString(i))
                                                        }
                                                        withContext(Dispatchers.Main) {
                                                            userSuggestions = tempList
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) { userSuggestions = emptyList() }
                                                }
                                            }
                                        } else {
                                            userSuggestions = emptyList()
                                        }
                                    },
                                    placeholder = { Text("Unesi korisničko ime...") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (newFriendQuery.isNotBlank()) {
                                            val targetUser = newFriendQuery.trim()
                                            coroutineScope.launch(Dispatchers.IO) {
                                                try {
                                                    val jsonBody = JSONObject().apply {
                                                        put("action", "add")
                                                        put("username", currentUsername)
                                                        put("friend_username", targetUser)
                                                    }.toString()

                                                    val response = client.post(NetworkConfig.getFriendsApiUrl()) {
                                                        contentType(ContentType.Application.Json)
                                                        setBody(jsonBody)
                                                    }
                                                    val res = JSONObject(response.bodyAsText())
                                                    isError = !res.optBoolean("success", false)
                                                    infoMessage = res.getString("message")
                                                    if (!isError) {
                                                        withContext(Dispatchers.Main) {
                                                            newFriendQuery = ""
                                                            userSuggestions = emptyList()
                                                        }
                                                    }
                                                } catch (e: Exception) { isError = true; infoMessage = "Greška na mreži." }
                                            }
                                        }
                                    }
                                ) { Icon(Icons.Default.Add, contentDescription = "Slanje", tint = Color(0xFF2196F3)) }
                            }

                            // Sugestije u vidu sive kartice ispod polja
                            if (userSuggestions.isNotEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEEEEE)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(4.dp)) {
                                        userSuggestions.forEach { predlozenoIme ->
                                            Text(
                                                text = predlozenoIme,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        newFriendQuery = predlozenoIme
                                                        userSuggestions = emptyList()
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                fontSize = 15.sp,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        infoMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = msg, color = if (isError) Color.Red else Color(0xFF4CAF50), fontSize = 14.sp)
                        }
                    }
                }
            }

            // === DEO 2: PREDLOŽENI PRIJATELJI (FRIEND SUGGESTIONS) ===
            // 1. MESTO: PromenisuggestionsList u trenutnaCetiriPredloga
            if (trenutnaCetiriPredloga.isNotEmpty()) {
                item { Text(text = "Ljudi koje možda poznaješ", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Gray) }

                // 2. MESTO: Promeni i ovde unutar items-a
                items(trenutnaCetiriPredloga) { sugg ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(color = if (sugg.isOnline) Color(0xFF4CAF50) else Color(0xFF9E9E9E), shape = CircleShape))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = sugg.username, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                            }
                            TextButton(
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        try {
                                            val jsonBody = JSONObject().apply {
                                                put("action", "add")
                                                put("username", currentUsername)
                                                put("friend_username", sugg.username)
                                            }.toString()
                                            val response = client.post(NetworkConfig.getFriendsApiUrl()) {
                                                contentType(ContentType.Application.Json)
                                                setBody(jsonBody)
                                            }
                                            val res = JSONObject(response.bodyAsText())
                                            if (res.optBoolean("success", false)) {
                                                suggestionsList = suggestionsList.filter { it.id != sugg.id }

                                                trenutnaCetiriPredloga = trenutnaCetiriPredloga.filter { it.id != sugg.id }
                                            }
                                        } catch (e: Exception) { Log.e("ChatterFriends", "Sugg add error: ${e.message}") }
                                    }
                                }
                            ) { Text(text = "Dodaj", color = Color(0xFF2196F3), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }

            // === DEO 3: ZAHTEVI NA ČEKANJU ===
            if (requestsList.isNotEmpty()) {
                item { Text(text = "Zahtevi za prijateljstvo (${requestsList.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Gray) }
                items(requestsList) { request ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = request.username, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Row {
                                IconButton(onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        try {
                                            val jsonBody = JSONObject().apply { put("action", "accept"); put("username", currentUsername); put("friend_id", request.id) }.toString()
                                            client.post(NetworkConfig.getFriendsApiUrl()) { contentType(ContentType.Application.Json); setBody(jsonBody) }
                                        } catch (e: Exception) { Log.e("ChatterFriends", "Error: ${e.message}") }
                                    }
                                }) { Icon(Icons.Default.Check, contentDescription = "Accept", tint = Color(0xFF4CAF50)) }
                                IconButton(onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        try {
                                            val jsonBody = JSONObject().apply { put("action", "unfriend"); put("username", currentUsername); put("friend_id", request.id) }.toString()
                                            client.post(NetworkConfig.getFriendsApiUrl()) { contentType(ContentType.Application.Json); setBody(jsonBody) }
                                        } catch (e: Exception) { Log.e("ChatterFriends", "Error: ${e.message}") }
                                    }
                                }) { Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Red) }
                            }
                        }
                    }
                }
            }

            // === DEO 4: TVOJI PRIJATELJI ===
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Tvoji prijatelji (${friendsList.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = localSearchQuery,
                        onValueChange = { localSearchQuery = it },
                        placeholder = { Text("Pretraži listu prijatelja...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                    )
                }
            }

            if (filtriraniPrijatelji.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                        Text(text = if (localSearchQuery.isNotBlank()) "Nema pronađenih" else "Lista prijatelja je prazna", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                items(filtriraniPrijatelji) { friend ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(12.dp).background(color = if (friend.isOnline) Color(0xFF4CAF50) else Color(0xFF9E9E9E), shape = CircleShape))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = friend.username, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            TextButton(onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val jsonBody = JSONObject().apply { put("action", "unfriend"); put("username", currentUsername); put("friend_id", friend.id) }.toString()
                                        client.post(NetworkConfig.getFriendsApiUrl()) { contentType(ContentType.Application.Json); setBody(jsonBody) }
                                    } catch (e: Exception) { Log.e("ChatterFriends", "Error: ${e.message}") }
                                }
                            }) { Text(text = "Ukloni", color = Color.Red, fontSize = 14.sp) }
                        }
                    }
                }
            }
        }
    }
}
