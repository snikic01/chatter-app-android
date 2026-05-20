package com.example.chatterapp.data

import androidx.lifecycle.ViewModel

class AuthViewModel(private val sessionManager: SessionManager) : ViewModel() {

    // Uzima ime korisnika iz sesije za prikaz na DashboardScreen-u
    fun getSavedUsername(): String? {
        return sessionManager.getSavedUsername()
    }

    // Poziva se kada klikneš na Logout dugme da se obrišu podaci
    fun logoutUser() {
        sessionManager.logout()
    }
}