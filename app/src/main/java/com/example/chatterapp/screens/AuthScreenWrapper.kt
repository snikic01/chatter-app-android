package com.example.chatterapp.screens

import androidx.compose.runtime.*
import com.example.chatterapp.Screen
import com.example.chatterapp.Tab
import com.example.chatterapp.data.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.example.chatterapp.data.NetworkConfig

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
                            try {
                                // POPRAVLJENO: Izvlačenje user_id iz PHP-a za Login
                                val url = "${NetworkConfig.BASE_URL}api_auth.php?action=login&username=$user&password=$pass"
                                val responseText = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    java.net.URL(url).readText()
                                }
                                val jsonObject = org.json.JSONObject(responseText)
                                val fetchedUserId = jsonObject.optInt("user_id", 0)

                                // Uspešno prosleđivanje ispravnog Int ID-ja
                                sessionManager.saveSession(userId = fetchedUserId, username = user, token = "generisani_token")
                                onUsernameChange(user)
                                authErrorMessage = null
                                onScreenChange(Screen.CHAT)
                                onTabChange(Tab.DASHBOARD)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                authErrorMessage = "Greška pri autorizaciji na serveru."
                            }
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
                            try {
                                // Šaljemo upit da bismo izvukli generisani JSON sa novim user_id
                                val url = "${NetworkConfig.BASE_URL}api_auth.php?action=login&username=$user&password=$pass"
                                val responseText = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    java.net.URL(url).readText()
                                }
                                val jsonObject = org.json.JSONObject(responseText)
                                val fetchedUserId = jsonObject.optInt("user_id", 0)

                                // POPRAVLJENO: Prosleđujemo parametre tačnim imenom kako ih SessionManager traži
                                sessionManager.saveSession(userId = fetchedUserId, username = user, token = "generisani_token")
                                onUsernameChange(user)
                                authErrorMessage = null
                                onScreenChange(Screen.CHAT)
                                onTabChange(Tab.DASHBOARD)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                authErrorMessage = "Greška pri čitanju ID-ja sa servera."
                            }
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
