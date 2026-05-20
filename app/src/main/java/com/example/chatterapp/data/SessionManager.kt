package com.example.chatterapp.data

import android.content.Context

class SessionManager(context: Context) {
    // Kreira lokalnu bazu ključ-vrednost unutar telefona
    private val prefs = context.getSharedPreferences("chatter_prefs", Context.MODE_PRIVATE)

    // Čuva korisničko ime i privremeni token nakon uspešnog Login-a/Register-a
    fun saveSession(username: String, token: String) {
        prefs.edit().apply {
            putString("auth_token", token)
            putString("username", username)
            apply()
        }
    }

    // Proverava da li token postoji (ako postoji, korisnik je ulogovan)
    fun isLoggedIn(): Boolean {
        return prefs.getString("auth_token", null) != null
    }

    // Vraća sačuvano ime korisnika za prikaz na Dashboard-u
    fun getSavedUsername(): String? {
        return prefs.getString("username", null)
    }

    // Briše sve podatke iz memorije kada korisnik klikne na Logout dugme
    fun logout() {
        prefs.edit().clear().apply()
    }
}
