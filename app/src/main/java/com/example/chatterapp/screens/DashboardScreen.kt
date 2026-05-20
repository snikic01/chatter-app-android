package com.example.chatterapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatterapp.data.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel,
    onLogoutSuccess: () -> Unit
) {
    // Uzimamo sačuvano korisničko ime iz lokalne sesije preko ViewModel-a
    val username = authViewModel.getSavedUsername() ?: "Korisnik"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chatter Dashboard",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                ),
                actions = {
                    // Logout dugme u gornjem desnom uglu ekrana
                    IconButton(onClick = {
                        authViewModel.logoutUser() // Prazni SharedPreferences keš
                        onLogoutSuccess()          // Menja ekran na LOGIN u MainActivity
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Odjavi se",
                            tint = Color.Red // Crvena boja za jasnu akciju izlaza
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        // paddingValues sprečava da gornja traka prekrije tvoj sadržaj
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F9FA))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Ulogovan si kao: $username 👋",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Izaberi opciju u donjoj navigaciji da započneš dopisivanje.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }
    }
}
