package com.example.chatterapp.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

class DashboardViewModel(
    private val authViewModel: AuthViewModel
) : ViewModel() {

    private val _posts = MutableStateFlow<List<DashboardPost>>(emptyList())
    val posts: StateFlow<List<DashboardPost>> = _posts

    private val _comments = MutableStateFlow<List<DashboardComment>>(emptyList())
    val comments: StateFlow<List<DashboardComment>> = _comments

    private val _logs = MutableStateFlow<List<AdminLog>>(emptyList())
    val logs: StateFlow<List<AdminLog>> = _logs

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val currentUserId: Int
        get() = authViewModel.getSavedUserId() ?: 0

    val currentUsername: String
        get() = authViewModel.getSavedUsername() ?: ""

    // Pomoćna funkcija za izvršavanje HTTP GET/POST zahteva na serveru u pozadini
    // Popravljeno: Potpuno asinhroni mrežni poziv sa timeout-om koji ne blokira telefon
    private suspend fun sendHttpRequest(urlString: String): String = withContext(Dispatchers.IO) {
        var connection: java.net.HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000  // Maksimalno 5 sekundi za povezivanje
            connection.readTimeout = 5000     // Maksimalno 5 sekundi za čitanje podataka
            connection.doInput = true

            // Čitanje odgovora u baferu
            val reader = java.io.BufferedReader(java.io.InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()
            response.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            // Vraćamo prazan JSON uspeh kako aplikacija ne bi ostala u zamrznutom stanju
            "{\"success\":false, \"posts\":[], \"comments\":[], \"message\":\"Greska u mrezi\"}"
        } finally {
            connection?.disconnect()
        }
    }


    fun loadDashboardData(isSilent: Boolean = false) {
        viewModelScope.launch {
            // Ako je zahtev "tih" (silent), NE palimo kružić za učitavanje i lista NE nestaje
            if (!isSilent) {
                _isLoading.value = true
            }
            try {
                val url = NetworkConfig.getDashboardDataUrl(currentUserId, currentUsername)
                val jsonResponse = sendHttpRequest(url)
                android.util.Log.d("ChatterBUG", "Sirovi JSON sa servera: $jsonResponse")
                val jsonObject = JSONObject(jsonResponse)

                if (jsonObject.optBoolean("success", false)) {
                    _isAdmin.value = jsonObject.optBoolean("is_admin", false)

                    val postsArray = jsonObject.optJSONArray("posts")
                    val parsedPosts = mutableListOf<DashboardPost>()

                    if (postsArray != null) {
                        for (i in 0 until postsArray.length()) {
                            val pObj = postsArray.getJSONObject(i)
                            parsedPosts.add(
                                DashboardPost(
                                    postId = pObj.optInt("id", 0),
                                    title = pObj.optString("title", ""),
                                    content = pObj.optString("content", ""),
                                    createdAt = pObj.optString("created_at", ""),
                                    boardColor = pObj.optString("type", "standard"),
                                    totalLikes = pObj.optInt("likes_count", 0),
                                    totalComments = pObj.optInt("comments_count", 0),
                                    authorName = "Admin",
                                    isLiked = pObj.optInt("is_liked", 0)
                                )
                            )
                        }
                    }
                    _posts.value = parsedPosts
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Gasimo učitavanje samo ako smo ga i upalili
                if (!isSilent) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun toggleLike(postId: Int) {
        viewModelScope.launch {
            try {
                // POPRAVLJENO: Dodat currentUsername u sredinu
                val url = NetworkConfig.getToggleLikeUrl(currentUserId, currentUsername, postId)
                android.util.Log.d("ChatterBUG", "Poziv za Lajk URL: $url")
                val jsonResponse = sendHttpRequest(url)
                android.util.Log.d("ChatterBUG", "Odgovor za Lajk: $jsonResponse")
                val jsonObject = JSONObject(jsonResponse)
                if (jsonObject.optBoolean("success", false)) {
                    loadDashboardData(isSilent = true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadComments(postId: Int) {
        viewModelScope.launch {
            try {
                // POPRAVLJENO: Dodat currentUsername u sredinu
                val url = NetworkConfig.getCommentsUrl(currentUserId, currentUsername, postId)
                val jsonResponse = sendHttpRequest(url)
                android.util.Log.d("ChatterBUG", "Poziv za Komentare URL: $url")
                val jsonObject = JSONObject(jsonResponse)

                if (jsonObject.optBoolean("success", false)) {
                    val commentsArray = jsonObject.optJSONArray("comments")
                    val parsedComments = mutableListOf<DashboardComment>()

                    if (commentsArray != null) {
                        for (i in 0 until commentsArray.length()) {
                            val cObj = commentsArray.getJSONObject(i)
                            parsedComments.add(
                                DashboardComment(
                                    id = cObj.optInt("id", 0),
                                    postId = cObj.optInt("news_id", 0),
                                    userId = cObj.optInt("user_id", 0),
                                    commentText = cObj.optString("comment_text", ""),
                                    createdAt = cObj.optString("created_at", ""),
                                    commenterName = cObj.optString("commenter_name", "Korisnik")
                                )
                            )
                        }
                    }
                    _comments.value = parsedComments
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addComment(postId: Int, text: String) {
        viewModelScope.launch {
            if (text.isBlank()) return@launch
            try {
                val url = NetworkConfig.getAddCommentUrl(currentUserId, currentUsername, postId, text)
                val jsonResponse = sendHttpRequest(url)
                android.util.Log.d("ChatterBUG", "Komentari JSON: $jsonResponse")
                val jsonObject = JSONObject(jsonResponse)
                if (jsonObject.optBoolean("success", false)) {
                    loadComments(postId)
                    loadDashboardData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun editComment(postId: Int, commentId: Int, newText: String) {
        viewModelScope.launch {
            try {
                val url = NetworkConfig.getEditCommentUrl(currentUserId, currentUsername, commentId, newText);                val jsonResponse = sendHttpRequest(url)
                val jsonObject = JSONObject(jsonResponse)
                if (jsonObject.optBoolean("success", false)) {
                    loadComments(postId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteComment(postId: Int, commentId: Int) {
        viewModelScope.launch {
            try {
                val url = NetworkConfig.getDeleteCommentUrl(currentUserId, currentUsername, commentId);                val jsonResponse = sendHttpRequest(url)
                val jsonObject = JSONObject(jsonResponse)
                if (jsonObject.optBoolean("success", false)) {
                    loadComments(postId)
                    loadDashboardData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadAdminLogs() {
        viewModelScope.launch {
            try {
                val url = NetworkConfig.getAdminLogsUrl(currentUserId)
                val jsonResponse = sendHttpRequest(url)
                val jsonObject = JSONObject(jsonResponse)

                if (jsonObject.optBoolean("success", false)) {
                    val logsArray = jsonObject.optJSONArray("logs")
                    val parsedLogs = mutableListOf<AdminLog>()

                    if (logsArray != null) {
                        for (i in 0 until logsArray.length()) {
                            val lObj = logsArray.getJSONObject(i)
                            parsedLogs.add(
                                AdminLog(
                                    id = lObj.optInt("id", 0),
                                    userName = lObj.optString("username", ""),
                                    ipAddress = lObj.optString("ip_address", "0.0.0.0"),
                                    loginTime = lObj.optString("login_time", "")
                                )
                            )
                        }
                    }
                    _logs.value = parsedLogs
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addNewPost(title: String, content: String, color: String) {
        viewModelScope.launch {
            try {
                val url = NetworkConfig.getAddPostUrl(currentUserId, currentUsername, title, content, color)
                val jsonResponse = sendHttpRequest(url)
                val jsonObject = JSONObject(jsonResponse)
                if (jsonObject.optBoolean("success", false)) {
                    loadDashboardData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deletePost(postId: Int) {
        viewModelScope.launch {
            try {
                val url = NetworkConfig.getDeletePostUrl(currentUserId, currentUsername, postId)
                val jsonResponse = sendHttpRequest(url)
                val jsonObject = JSONObject(jsonResponse)
                if (jsonObject.optBoolean("success", false)) {
                    loadDashboardData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun editPost(postId: Int, title: String, content: String) {
        viewModelScope.launch {
            try {
                // Sklapamo URL za izmenu objave sa serverom
                val url = "${NetworkConfig.BASE_URL}api_dashboard.php?action=post_edit&user_id=$currentUserId&username=$currentUsername&post_id=$postId&title=${java.net.URLEncoder.encode(title, "UTF-8")}&content=${java.net.URLEncoder.encode(content, "UTF-8")}"
                val jsonResponse = sendHttpRequest(url)
                val jsonObject = JSONObject(jsonResponse)

                if (jsonObject.optBoolean("success", false)) {
                    // Osvežavamo oglasnu tablu da odmah prikaže novi tekst
                    loadDashboardData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}
