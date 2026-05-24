package com.example.chatterapp.screens

import org.json.JSONObject
import io.ktor.client.statement.bodyAsText
import androidx.compose.runtime.setValue
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import android.util.Log
import kotlinx.coroutines.launch


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatterapp.data.ChatMessage

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add



// Tvoj lokalni model proširen poljem unreadCount za brojač sa sajta
data class AndroidChatGroup(
    val id: Int,
    val name: String,
    val isOwner: Boolean = false,
    val unreadCount: Int = 0,
    val ownerName: String = "" // DODATO za vlasnika grupe
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    currentUsername: String,
    messagesList: List<ChatMessage>,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    onSendMessageClick: () -> Unit,
    listState: LazyListState,
    activeGroupId: Int,
    onGroupChange: (Int) -> Unit,
    groupsList: List<AndroidChatGroup>,
    client: io.ktor.client.HttpClient // 👈 POSLEDNJI PARAMETAR JE KLIJENT!
) {

    // Dijalozi za kreiranje grupe:
    var prikaziDialogZaGrupu by remember { mutableStateOf(false) }
    var nazivNoveGrupe by remember { mutableStateOf("") }


    var showMembersDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val currentGroup = groupsList.find { it.id == activeGroupId }
    val isOwnerOfCurrentGroup = currentGroup?.isOwner ?: false

    // DODATA PROMENLJIVA ZA ČLANOVE KOJA JE FALILA (Linija 135 sa slike)
    // SADA KORISTIMO TRIPLE DA BI PAMTILI: ID, USERNAME I IS_ONLINE STATUS
    var groupMembers by remember { mutableStateOf<List<Triple<Int, String, Boolean>>>(emptyList()) }
    var newMemberUsername by remember { mutableStateOf("") } // Polje za unos novog člana u dijalogu
    val coroutineScope = rememberCoroutineScope()

    var userSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }

// Skeniramo šta korisnik kuca i na svaku promenu slova pitamo server za predloge
    LaunchedEffect(newMemberUsername) {
        if (newMemberUsername.length >= 2) { // Pokreće pretragu tek kad ukucaš bar 2 slova
            try {
                val url = com.example.chatterapp.data.NetworkConfig.getSearchUsersUrl(
                    activeGroupId,
                    newMemberUsername.trim()
                )
                val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    client.get(url)
                }
                val json = JSONObject(response.bodyAsText())
                if (json.optBoolean("success", false)) {
                    val array = json.getJSONArray("users")
                    val tempList = mutableListOf<String>()
                    for (i in 0 until array.length()) {
                        tempList.add(array.getString(i))
                    }
                    userSuggestions = tempList
                }
            } catch (e: Exception) {
                userSuggestions = emptyList()
            }
        } else {
            userSuggestions = emptyList() // Ako obriše tekst, praznimo predloge
        }
    }


    // --- OSVEŽEN POZIV: Čita i is_online status sa tvog modularnog backenda ---
    LaunchedEffect(showMembersDialog) {
        if (showMembersDialog && activeGroupId != 0) {
            try {
                val url = com.example.chatterapp.data.NetworkConfig.getMembersUrl(activeGroupId)
                val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    client.get(url)
                }

                val json = JSONObject(response.bodyAsText())
                if (json.optBoolean("success", false) || json.has("members")) {
                    val array = json.getJSONArray("members")
                    val tempList = mutableListOf<Triple<Int, String, Boolean>>()

                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)

                        val idString = obj.optString("id", "0")
                        val realId = idString.toIntOrNull() ?: 0
                        val realName = obj.optString("username", "Korisnik")
                        val isOnline = obj.optInt("is_online", 0) == 1

                        tempList.add(Triple(realId, realName, isOnline))
                    }

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        groupMembers = tempList
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatterMembers", "Greška pri učitavanju članova: ${e.message}")
            }
        }
    }

    if (activeGroupId == 0) {
        // --- 1. PRIKAZ LISTE TVOJIH GRUPA (SA REPLICIRANIM WEB FILTEROM) ---
        Scaffold(
            topBar = {
                TopAppBar(title = {
                    Text(
                        "Izaberi Čet Grupu",
                        fontWeight = FontWeight.Bold
                    )
                })
            },
            //  DOPUNA: Plutajuće plavo "+" dugme ubačeno peške unutar Scaffold-a
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { prikaziDialogZaGrupu = true },
                    containerColor = Color(0xFF2196F3),
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nova grupa"
                    )
                }
            }
        ) { paddingValues ->

            // 🛠️ KONAČNA POPRAVKA: Dijalog je ubačen TAČNO ovde unutar paddingValues-a!
            // Sada će se garantovano rendersovati preko celog ekrana tvog Samsunga S9+
            if (prikaziDialogZaGrupu) {
                AlertDialog(
                    onDismissRequest = {
                        prikaziDialogZaGrupu = false
                        nazivNoveGrupe = ""
                    },
                    title = { Text("Kreiraj novu grupu", fontWeight = FontWeight.Bold) },
                    text = {
                        OutlinedTextField(
                            value = nazivNoveGrupe,
                            onValueChange = { nazivNoveGrupe = it },
                            placeholder = { Text("Unesite ime grupe...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (nazivNoveGrupe.isNotBlank()) {
                                    val imeGrupe = nazivNoveGrupe.trim()
                                    prikaziDialogZaGrupu = false
                                    nazivNoveGrupe = ""

                                    // Okidamo kreiranje grupe u pozadinskoj niti na serveru
                                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        try {
                                            val jsonBody = JSONObject().apply {
                                                put("action", "create")
                                                put("username", currentUsername)
                                                put("group_name", imeGrupe) // Šaljemo tačan ključ za tvoj group-actions/create.php
                                            }.toString()

                                            // Šaljemo zahtev na tvoju centralnu rutu za grupe
                                            client.post(com.example.chatterapp.data.NetworkConfig.getMembersApiUrl()) {
                                                contentType(io.ktor.http.ContentType.Application.Json)
                                                setBody(jsonBody)
                                            }

                                            // Tvoj PHP 'create.php' uspešno upisuje novu grupu u bazu pod novim najvećim ID-jem!
                                            // Pozadinski poler u MainScaffold-u će u sledećoj sekundi automatski povući podatke,
                                            // i pošto tvoj 'list.php' sortira po ID DESC, nova grupa će sama od sebe skočiti na VRH!

                                        } catch (e: Exception) {
                                            android.util.Log.e("GroupsCreate", "Greška pri kreiranju: ${e.message}")
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("Napravi")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                prikaziDialogZaGrupu = false
                                nazivNoveGrupe = ""
                            }
                        ) {
                            Text("Poništi")
                        }
                    }
                )
            }

            // Tvoja lista grupa kreće odmah ispod dijaloga
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF8F9FA))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groupsList) { group ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGroupChange(group.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = group.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2196F3)
                                )
                                Text(
                                    text = if (group.isOwner) "Ti si vlasnik" else "Član si grupe",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            // --- CRVENI BADGE ZA NEPROČITANE PORUKE (Isto kao na veb aplikaciji) ---
                            if (group.unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.Red, shape = CircleShape)
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = group.unreadCount.toString(),
                                        color = Color.White,
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
    } else {
        // Čim se lista poruka promeni ili napuni, automatski skrolujemo na poslednju stavku
        LaunchedEffect(messagesList.size) {
            if (messagesList.isNotEmpty()) {
                listState.animateScrollToItem(messagesList.size - 1)
            }
        }
        // --- POPRAVLJENO: Poler koji automatski osvežava lampice na 3 sekunde dok je dijalog otvoren ---
        LaunchedEffect(showMembersDialog) {
            if (showMembersDialog && activeGroupId != 0) {
                while (showMembersDialog) { // Vrti petlju sve dok korisnik ne zatvori info panel
                    try {
                        val url = com.example.chatterapp.data.NetworkConfig.getMembersUrl(activeGroupId)
                        val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            client.get(url)
                        }

                        val json = JSONObject(response.bodyAsText())
                        Log.d("ChatterMembersBUG", "Automatsko osvežavanje članova: ${response.bodyAsText()}")

                        if (json.optBoolean("success", false) || json.has("members")) {
                            val array = json.getJSONArray("members")
                            val tempList = mutableListOf<Triple<Int, String, Boolean>>()

                            for (i in 0 until array.length()) {
                                val obj = array.getJSONObject(i)

                                val idString = if (obj.has("id")) obj.getString("id") else obj.optString("Id", "0")
                                val realId = idString.toIntOrNull() ?: 0
                                val realName = if (obj.has("username")) obj.getString("username") else obj.optString("Username", "Nepoznato")
                                val isOnline = obj.optInt("is_online", 0) == 1

                                tempList.add(Triple(realId, realName, isOnline))
                            }

                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                groupMembers = tempList
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ChatterMembersBUG", "Greška u poleru članova: ${e.message}", e)
                    }

                    // Sačekaj 3 sekunde pre sledeće provere baze podataka
                    kotlinx.coroutines.delay(3000)
                }
            }
        }

        // --- 2. PRIKAZ ČETA UNUTAR SELEKTOVANE GRUPE (POPRAVLJENA STRUKTURA BARA) ---
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2196F3))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onGroupChange(0) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Nazad",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentGroup?.name ?: "Grupa ID: $activeGroupId",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showMembersDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Članovi", tint = Color.White)
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Opcije",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            // 1. OPCIJA ZA SVE (I vlasnik i običan član mogu uvek da napuste grupu i prenesu vlast)
                            DropdownMenuItem(
                                text = { Text("Napusti grupu", color = Color.DarkGray) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.ExitToApp,
                                        contentDescription = null,
                                        tint = Color.DarkGray
                                    )
                                },
                                onClick = {
                                    onGroupChange(-activeGroupId * 100)
                                    showMenu = false
                                }
                            )

                            // 2. DODATNA OPCIJA (Garantovano se iscrtava vlasniku na tri tačke jer je struktura popravljena)
                            if (isOwnerOfCurrentGroup) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Obriši grupu (Za sve)",
                                            color = Color.Red,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = Color.Red
                                        )
                                    },
                                    onClick = {
                                        onGroupChange(-activeGroupId)
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    } // Zatvara Box ispravno
                } // Zatvara desni Row dugmića ispravno
            } // Preostali kod ispod nastavlja nesmetano...
            // Zatvara gornji plavi Row ispravno

// --- SREDIŠNJI DEO: MODERNI BALONČIĆI SA AUTOMATSKIM SKROLOM ---
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Zauzima sav prostor između bara i polja za unos
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // POPRAVLJENO: Koristimo itemsIndexed da imamo pristup index-u za promenu datuma
                itemsIndexed(messagesList) { index, chatMessage ->
                    val isMe = chatMessage.username == currentUsername

                    // Izvlačimo datum (npr. "2026-05-21") iz formata "YYYY-MM-DD HH:MM:SS"
                    val trenutniDatum = chatMessage.date.substringBefore(" ")

                    // --- 1. PREGRADNICA ZA DATUM ---
                    // Prikazuje se ako je prva poruka u četu ili ako je datum drugačiji od prethodne poruke
                    val prikaziPregradnicu =
                        index == 0 || messagesList[index - 1].date.substringBefore(" ") != trenutniDatum

                    if (prikaziPregradnicu) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Leva linija pregradnice
                            Spacer(
                                modifier = Modifier.weight(1f).height(1.dp)
                                    .background(Color(0xFFDDDDDD))
                            )

                            // Tekst sa datumom
                            Text(
                                text = trenutniDatum.ifBlank { "Istorija" },
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            // Desna linija pregradnice
                            Spacer(
                                modifier = Modifier.weight(1f).height(1.dp)
                                    .background(Color(0xFFDDDDDD))
                            )
                        }
                    }

                    // --- GLAVNI BALONČIĆ ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Column(
                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            if (!isMe) {
                                Text(
                                    text = chatMessage.username,
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isMe) Color(0xFF2196F3) else Color(0xFFE0E0E0),
                                        // OBRISANA POGREŠNA PUTANJA: Koristimo čist import sa vrha fajla!
                                        shape = RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isMe) 16.dp else 2.dp,
                                            bottomEnd = if (isMe) 2.dp else 16.dp
                                        )
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = chatMessage.message,
                                        color = if (isMe) Color.White else Color.Black,
                                        fontSize = 16.sp
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        // Izvlačimo samo sate i minute iz formata datuma
                                        val vreme = chatMessage.date.substringAfter(" ")
                                            .substringBeforeLast(":")
                                        Text(
                                            text = vreme.ifBlank { "00:00" },
                                            fontSize = 10.sp,
                                            color = if (isMe) Color(0xFFBBDEFB) else Color.Gray
                                        )

                                        // --- 2. SKUPNI SEEN STATUS ZA IMENA ---
                                        if (isMe && chatMessage.seenBy.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(4.dp))

                                            // Filtriramo tvoje ime (da ne piše da si ti video svoju poruku)
                                            val ostaliKorisnici =
                                                chatMessage.seenBy.filter { it != currentUsername }

                                            if (ostaliKorisnici.isNotEmpty()) {
                                                // Spajamo sva imena u jedan string razdvojen zarezom (npr. "nikic, petar")
                                                val imenaKojiSuVideli =
                                                    ostaliKorisnici.joinToString(", ")

                                                Text(
                                                    text = "✓ ($imenaKojiSuVideli)",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB2FF59) // Prelepa svetlo zelena boja
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

            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = onTextInputChange,
                    placeholder = { Text("Upiši poruku...") },
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    shape = RoundedCornerShape(24.dp)
                )
                IconButton(onClick = onSendMessageClick, enabled = textInput.isNotBlank()) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Pošalji",
                        tint = Color(0xFF2196F3)
                    )
                }
            }
        }
    }

    if (showMembersDialog) {
        AlertDialog(
            onDismissRequest = {
                showMembersDialog = false
                newMemberUsername = "" // Čistimo polje pri zatvaranju
                userSuggestions = emptyList()
            },
            title = { Text("Upravljanje članovima", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {

                    // === 1. POLJE ZA DODAVANJE NOVOG ČLANA SA PRETRAGOM ===
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newMemberUsername,
                                onValueChange = { newMemberUsername = it },
                                label = { Text("Korisničko ime") },
                                placeholder = { Text("npr. petar") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (newMemberUsername.isNotBlank()) {
                                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                            try {
                                                val url =
                                                    com.example.chatterapp.data.NetworkConfig.getAddMemberApiUrl()
                                                val jsonBody = JSONObject().apply {
                                                    put("action", "add")
                                                    put("username", currentUsername)
                                                    put("group_id", activeGroupId)
                                                    put(
                                                        "new_member_username",
                                                        newMemberUsername.trim()
                                                    )
                                                }.toString()

                                                val response = client.post(url) {
                                                    contentType(io.ktor.http.ContentType.Application.Json)
                                                    setBody(jsonBody)
                                                }

                                                val jsonResult = JSONObject(response.bodyAsText())
                                                if (jsonResult.optBoolean("success", false)) {
                                                    val svezUrl =
                                                        com.example.chatterapp.data.NetworkConfig.getMembersUrl(
                                                            activeGroupId
                                                        )
                                                    val svezRes = client.get(svezUrl)
                                                    val svezJson = JSONObject(svezRes.bodyAsText())
                                                    if (svezJson.optBoolean("success", false)) {
                                                        val arr = svezJson.getJSONArray("members")
                                                        val osvezenSpisak =
                                                            mutableListOf<Triple<Int, String, Boolean>>()
                                                        for (i in 0 until arr.length()) {
                                                            val o = arr.getJSONObject(i)
                                                            val idStr = o.optString("id", "0")
                                                            osvezenSpisak.add(
                                                                Triple(
                                                                    idStr.toIntOrNull() ?: 0,
                                                                    o.optString(
                                                                        "username",
                                                                        "Korisnik"
                                                                    ),
                                                                    o.optInt("is_online", 0) == 1
                                                                )
                                                            )
                                                        }
                                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                            groupMembers = osvezenSpisak
                                                            newMemberUsername = ""
                                                            userSuggestions = emptyList()
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Log.e(
                                                    "ChatterMembers",
                                                    "Greška pri dodavanju: ${e.message}"
                                                )
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    text = "+",
                                    fontSize = 24.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = Color(0xFF2196F3)
                                )
                            }
                        }

                        // === SUGESTIJE (Iscrtavaju se dinamički ispod input polja) ===
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
                                                    newMemberUsername = predlozenoIme
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

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Svi registrovani članovi u ovoj grupi:",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // === 2. LISTA ČLANOVA SA LAMPICAMA I KANTOM ===
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (groupMembers.isEmpty()) {
                            item { Text("Učitavanje članova...", color = Color.Gray) }
                        } else {
                            items(groupMembers) { clan ->
                                val userId = clan.first
                                val username = clan.second
                                val isOnline = clan.third

                                val currentGroup = groupsList.find { it.id == activeGroupId }
                                val jesteVlasnikGrupe = currentGroup?.isOwner == true
                                val daLiSamToJa = username == currentUsername

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(
                                                    color = if (isOnline) Color(0xFF4CAF50) else Color(
                                                        0xFF9E9E9E
                                                    ),
                                                    shape = androidx.compose.foundation.shape.CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))

                                        Text(
                                            text = username,
                                            fontSize = 16.sp,
                                            color = Color.Black,
                                            fontWeight = if (daLiSamToJa) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }

                                    if (jesteVlasnikGrupe && !daLiSamToJa) {
                                        IconButton(onClick = {
                                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                try {
                                                    val url =
                                                        com.example.chatterapp.data.NetworkConfig.getMembersApiUrl()
                                                    val jsonBody = JSONObject().apply {
                                                        put("action", "kick")
                                                        put("username", currentUsername)
                                                        put("group_id", activeGroupId)
                                                        put("kick_user_id", userId)
                                                    }.toString()

                                                    val response = client.post(url) {
                                                        contentType(io.ktor.http.ContentType.Application.Json)
                                                        setBody(jsonBody)
                                                    }

                                                    val jsonResult =
                                                        JSONObject(response.bodyAsText())
                                                    if (jsonResult.optBoolean("success", false)) {
                                                        groupMembers =
                                                            groupMembers.filter { it.first != userId }
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e(
                                                        "ChatterMembers",
                                                        "Greška pri kikovnju: ${e.message}"
                                                    )
                                                }
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Izbaci",
                                                tint = Color.Red
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showMembersDialog = false
                    newMemberUsername = ""
                    userSuggestions = emptyList()
                }) { Text("Zatvori") }
            }
        )
    }
}
