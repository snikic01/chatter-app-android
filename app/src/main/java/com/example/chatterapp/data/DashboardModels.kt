package com.example.chatterapp.data

// Glavni odgovor sa API-ja za Dashboard podatke
data class DashboardDataResponse(
    val success: Boolean,
    val posts: List<DashboardPost>?,
    val isAdmin: Boolean
)

// Generički odgovor za akcije (lajk, brisanje, dodavanje)
data class BaseResponse(
    val success: Boolean,
    val message: String?
)

// Odgovor za komentare
data class CommentsResponse(
    val success: Boolean,
    val comments: List<DashboardComment>?
)

// Odgovor za admin logove
data class AdminLogsResponse(
    val success: Boolean,
    val logs: List<AdminLog>?
)

// Model za Objave - Nazivi se poklapaju sa tvojim DashboardScreen-om
data class DashboardPost(
    val postId: Int,
    val title: String,
    val content: String,
    val createdAt: String,
    val boardColor: String = "standard",
    val totalLikes: Int = 0,
    val totalComments: Int = 0,
    val authorName: String = "Admin",
    val isLiked: Int = 0
)

// Model za Komentare
data class DashboardComment(
    val id: Int,
    val postId: Int,
    val userId: Int,
    val commentText: String,
    val createdAt: String,
    val commenterName: String = "Korisnik"
)

// Model za Admin Logove
data class AdminLog(
    val id: Int,
    val userName: String,
    val ipAddress: String = "100.109.16.77",
    val loginTime: String
)
