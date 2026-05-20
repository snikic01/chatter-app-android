package com.example.chatterapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

enum class AppTab { DASHBOARD, GROUPS, PRIVATE, FRIENDS }

@Composable
fun MainAppScreen(username: String) {
    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Početna") },
                    label = { Text("Početna") },
                    selected = currentTab == AppTab.DASHBOARD,
                    onClick = { currentTab = AppTab.DASHBOARD }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Grupe") },
                    label = { Text("Grupe") },
                    selected = currentTab == AppTab.GROUPS,
                    onClick = { currentTab = AppTab.GROUPS }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.MailOutline, contentDescription = "Privatno") },
                    label = { Text("Privatno") },
                    selected = currentTab == AppTab.PRIVATE,
                    onClick = { currentTab = AppTab.PRIVATE }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Prijatelji") },
                    label = { Text("Prijatelji") },
                    selected = currentTab == AppTab.FRIENDS,
                    onClick = { currentTab = AppTab.FRIENDS }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentTab) {
                AppTab.DASHBOARD -> DashboardScreen(username = username, onNavigateToGroups = { currentTab = AppTab.GROUPS })
                AppTab.GROUPS -> GroupsScreen(onGroupSelect = { /* Privremeno prazno */ })
                AppTab.PRIVATE -> PrivateChatScreen()
                AppTab.FRIENDS -> {
                    // Integrisana bazična komponenta za prijatelje
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("Lista prijatelja sa weba (Uskoro)", color = Color.Gray)
                    }
                }
            }
        }
    }
}