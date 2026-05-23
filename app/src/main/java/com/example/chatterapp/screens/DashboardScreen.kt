package com.example.chatterapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatterapp.data.AuthViewModel
import com.example.chatterapp.data.DashboardViewModel
import androidx.compose.ui.window.Dialog
import com.example.chatterapp.data.DashboardPost

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel,
    dashboardViewModel: DashboardViewModel, // DODATO
    onLogoutSuccess: () -> Unit
) {
    val posts by dashboardViewModel.posts.collectAsState()
    val comments by dashboardViewModel.comments.collectAsState()
    val logs by dashboardViewModel.logs.collectAsState()
    val isAdmin by dashboardViewModel.isAdmin.collectAsState()
    val isLoading by dashboardViewModel.isLoading.collectAsState()

    var showAddPostDialog by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }
    var activeCommentsPostId by remember { mutableStateOf<Int?>(null) }
    var editingPost by remember { mutableStateOf<DashboardPost?>(null) }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current




    // Učitaj podatke sa PHP servera čim se ekran otvori
    LaunchedEffect(Unit) {
        dashboardViewModel.loadDashboardData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Chatter Dashboard", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                actions = {
                    IconButton(onClick = {
                        authViewModel.logoutUser()
                        onLogoutSuccess()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Odjavi se",
                            tint = Color.Red
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            // Samo korisnik snikic01 ili admin može kreirati novu objavu
            if (dashboardViewModel.currentUsername == "snikic01" || isAdmin) {
                FloatingActionButton(
                    onClick = { showAddPostDialog = true },
                    containerColor = Color(0xFF2196F3),
                    contentColor = Color.White
                ) {
                    Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F9FA))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                // Ulogovani korisnik pozdravna poruka
                Text(
                    text = "Ulogovan si kao: ${dashboardViewModel.currentUsername} 👋",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 1. ADMIN LOGS Dugme - Prikazuje se samo administratorima
                if (isAdmin) {
                    Button(
                        onClick = {
                            dashboardViewModel.loadAdminLogs()
                            showLogsDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("ADMIN LOGS", fontWeight = FontWeight.Bold)
                    }
                }

                // Lista objava
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(posts) { post ->
                            // DINAMIČKA BOJA: 'urgent' crvena, 'standard' ili bilo šta drugo plava
                            val cardColor =
                                if (post.boardColor == "urgent") Color(0xFFFFEBEE) else Color(
                                    0xFFE3F2FD
                                )

                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = post.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        // POPRAVLJENO: Prikazuje i Olovku i Kantu za brisanje samo adminima / snikic01
                                        if (dashboardViewModel.currentUsername == "snikic01" || isAdmin) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // 1. Olovka za Izmenu (Edit) Objave na oglasnoj tabli
                                                IconButton(onClick = { editingPost = post }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Create,
                                                        contentDescription = "Izmeni objavu",
                                                        tint = Color.Gray
                                                    )
                                                }


                                                // 2. Kanta za Brisanje Objave
                                                IconButton(onClick = {
                                                    dashboardViewModel.deletePost(
                                                        post.postId
                                                    )
                                                }) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Obriši",
                                                        tint = Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = post.createdAt,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = post.content,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "👤 Autor: ${post.authorName}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // POPRAVLJENO: Koristimo post.isLiked za tačan prikaz tvog ličnog lajka
                                            IconButton(onClick = { dashboardViewModel.toggleLike(post.postId) }) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        // Puno srce (Favorite) ako si TI lajkovao (1), inače prazno sivo (FavoriteBorder) ako nisi (0)
                                                        imageVector = if (post.isLiked == 1) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                        contentDescription = "Lajk",
                                                        // Crveno ako si TI lajkovao, sivo ako nisi (čak i ako objava ima tuđe lajkove)
                                                        tint = if (post.isLiked == 1) Color.Red else Color.Gray,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "${post.totalLikes}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }



                                            // POPRAVLJENO: Veća zona klika za Komentare 💬
                                            IconButton(onClick = {
                                                dashboardViewModel.loadComments(post.postId)
                                                activeCommentsPostId = post.postId
                                            }) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = "💬", fontSize = 16.sp)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "${post.totalComments}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
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
            }
        }
    }

    // --- DIJALOG ZA KREIRANJE NOVE OBJAVE ---
    if (showAddPostDialog) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var isUrgent by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddPostDialog = false },
            title = { Text("Nova Objava") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Naslov") })
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Sadržaj") })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isUrgent, onCheckedChange = { isUrgent = it })
                        Text("Hitna objava (Crvena tabla)")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val color = if (isUrgent) "urgent" else "standard"
                    dashboardViewModel.addNewPost(title, content, color)
                    showAddPostDialog = false
                }) { Text("Objavi") }
            },
            dismissButton = { TextButton(onClick = { showAddPostDialog = false }) { Text("Otkaži") } }
        )
    }

    // --- DIJALOG ZA ADMIN LOGOVE ---
    if (showLogsDialog) {
        AlertDialog(
            onDismissRequest = { showLogsDialog = false },
            title = { Text("Admin Login Logs") },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp).fillMaxWidth()) {
                    items(logs) { log ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text("Korisnik: ${log.userName}", fontWeight = FontWeight.Bold)
                            Text("IP: ${log.ipAddress} | Vreme: ${log.loginTime}", fontSize = 12.sp, color = Color.Gray)
                            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { showLogsDialog = false }) { Text("Zatvori") } }
        )
    }

    // --- DIJALOG ZA IZMENU OBJAVE PREKO OLOVKE ---
    editingPost?.let { post ->
        var editTitle by remember { mutableStateOf(post.title) }
        var editContent by remember { mutableStateOf(post.content) }

        AlertDialog(
            onDismissRequest = { editingPost = null },
            title = { Text("Izmeni objavu") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Naslov") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text("Sadržaj") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    // Prvo obrišemo staru verziju posta, a onda upišemo novu sa ažuriranim tekstom
                    dashboardViewModel.deletePost(post.postId)
                    dashboardViewModel.addNewPost(editTitle, editContent, post.boardColor)
                    editingPost = null // Zatvara dijalog nakon klika
                }) { Text("Sačuvaj izmene") }
            },
            dismissButton = {
                TextButton(onClick = { editingPost = null }) { Text("Otkaži") }
            }
        )
    }


    // --- DIJALOG ZA LISTANJE I PISANJE KOMENTARA ---
    activeCommentsPostId?.let { postId ->
        var newCommentText by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { activeCommentsPostId = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(450.dp).padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                    Text("Komentari", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                        items(comments) { comment ->
                            var isEditing by remember { mutableStateOf(false) }
                            var editText by remember { mutableStateOf(comment.commentText) }

                            // POPRAVLJENO: Poredimo tekstualno ime autora komentara sa tvojim trenutno ulogovanim imenom!
                            val isMyComment = (comment.commenterName == dashboardViewModel.currentUsername)

                            // Admin provera (da li si ulogovan kao snikic01)
                            val isAdmin = (dashboardViewModel.currentUsername == "snikic01")

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                // --- PRVI RED: IME AUTORA I AKCIJE (OLOVKA/KANTA) ---
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Prikazujemo ime korisnika koji je ostavio komentar
                                    Text(
                                        text = comment.commenterName,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2196F3)
                                    )

                                    // Horizontalni niz za akcije brisanja i izmene
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 1. OLOVKA (IZMENA): Vidi je samo autor komentara (bilo admin ili običan korisnik)
                                        if (isMyComment) {
                                            IconButton(
                                                onClick = { isEditing = !isEditing },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = androidx.compose.material.icons.Icons.Default.Create,
                                                    contentDescription = "Izmeni",
                                                    tint = Color.Gray,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        // 2. KANTA (BRISANJE): Vidi je autor komentara ILI ulogovani administrator
                                        if (isMyComment || isAdmin) {
                                            IconButton(
                                                onClick = { dashboardViewModel.deleteComment(postId, comment.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Obriši",
                                                    tint = Color.Red,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // --- DRUGI RED: TEKST KOMENTARA ILI POLJE ZA IZMENU ---
                                if (isEditing) {
                                    // Ako je korisnik kliknuo na olovku, otvara se polje da prepravi tekst
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = editText,
                                            onValueChange = { editText = it },
                                            modifier = Modifier.weight(1f),
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Button(
                                            onClick = {
                                                dashboardViewModel.editComment(postId, comment.id, editText)
                                                isEditing = false
                                            },
                                            modifier = Modifier.height(38.dp)
                                        ) {
                                            Text("Sačuvaj", fontSize = 11.sp)
                                        }
                                    }
                                } else {
                                    // POPRAVLJENO: Prikazujemo stvarni tekst komentara ispod imena!
                                    Text(
                                        text = comment.commentText,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(top = 2.dp, start = 2.dp)
                                    )
                                }

                                // Horizontalna linija između dva komentara
                                HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))
                            }
                        }

                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCommentText,
                            onValueChange = { newCommentText = it },
                            placeholder = { Text("Napiši komentar...") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(onClick = {
                            dashboardViewModel.addComment(postId, newCommentText)
                            newCommentText = ""
                        }) {
                            Text("Pošalji")
                        }
                    }
                }
            }
        }
    }
}
