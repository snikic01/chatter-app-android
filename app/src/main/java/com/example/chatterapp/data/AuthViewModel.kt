package com.example.chatterapp.data

import androidx.lifecycle.ViewModel

class AuthViewModel(private val sessionManager: SessionManager) : ViewModel() {

    // Dodato: Uzima ID korisnika iz sesije koji je neophodan za Dashboard API requests
    fun getSavedUserId(): Int? {
        // Pretpostavka je da tvoj SessionManager ima funkciju getSavedUserId ili slično.
        // Ako vraća String, dodaj .toIntOrNull() na kraj.
        return sessionManager.getSavedUserId()
    }

    // Uzima ime korisnika iz sesije za prikaz na DashboardScreen-u
    fun getSavedUsername(): String? {
        return sessionManager.getSavedUsername()
    }

    // Poziva se kada klikneš na Logout dugme da se obrišu podaci
    fun logoutUser() {
        sessionManager.logout()
    }
}
