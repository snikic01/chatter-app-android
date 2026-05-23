package com.example.chatterapp.data

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("chatter_prefs", Context.MODE_PRIVATE)

    // Ažurirano: Sada prima i čuva i userId (kao Int)
    fun saveSession(userId: Int, username: String, token: String) {
        prefs.edit().apply {
            putInt("user_id", userId)
            putString("auth_token", token)
            putString("username", username)
            apply()
        }
    }

    // Proverava da li token postoji
    fun isLoggedIn(): Boolean {
        return prefs.getString("auth_token", null) != null
    }

    // Vraća sačuvano ime korisnika
    fun getSavedUsername(): String? {
        return prefs.getString("username", null)
    }

    // Dodato: Vraća sačuvani ID korisnika (Vraća 0 ako ne postoji)
    fun getSavedUserId(): Int {
        return prefs.getInt("user_id", 0)
    }

    // Briše sve podatke iz memorije
    fun logout() {
        prefs.edit().clear().apply()
    }
}
