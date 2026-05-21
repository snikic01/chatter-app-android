package com.example.chatterapp.screens

import androidx.compose.runtime.*
import com.example.chatterapp.Screen
import com.example.chatterapp.Tab
import com.example.chatterapp.data.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AuthScreenWrapper(
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
    onTabChange: (Tab) -> Unit,
    onUsernameChange: (String) -> Unit,
    sessionManager: SessionManager,
    coroutineScope: CoroutineScope,
    handleAuth: suspend (String, String, String) -> Boolean
) {
    var authErrorMessage by remember { mutableStateOf<String?>(null) }

    when (currentScreen) {
        Screen.LOGIN -> {
            AuthScreen(
                isLogin = true,
                errorMessage = authErrorMessage,
                onActionClick = { user, pass ->
                    authErrorMessage = "Provera..."
                    coroutineScope.launch {
                        if (handleAuth(user, pass, "login")) {
                            sessionManager.saveSession(user, "generisani_token_ili_id")
                            onUsernameChange(user)
                            authErrorMessage = null
                            onScreenChange(Screen.CHAT)
                            onTabChange(Tab.DASHBOARD)
                        } else {
                            authErrorMessage = "Pogrešna šifra ili korisnik."
                        }
                    }
                },
                onSwitchScreen = {
                    authErrorMessage = null
                    onScreenChange(Screen.REGISTER)
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
                            sessionManager.saveSession(user, "generisani_token_ili_id")
                            onUsernameChange(user)
                            authErrorMessage = null
                            onScreenChange(Screen.CHAT)
                            onTabChange(Tab.DASHBOARD)
                        } else {
                            authErrorMessage = "Greška! Ime zauzeto."
                        }
                    }
                },
                onSwitchScreen = {
                    authErrorMessage = null
                    onScreenChange(Screen.LOGIN)
                }
            )
        }
        else -> {}
    }
}
