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

    val currentUserId: Int = authViewModel.getSavedUserId() ?: 0
    val currentUsername: String = authViewModel.getSavedUsername() ?: ""

    // Pomoćna funkcija za izvršavanje HTTP GET/POST zahteva na serveru u pozadini
    private suspend fun sendHttpRequest(urlString: String): String = withContext(Dispatchers.IO) {
        try {
            URL(urlString).readText()
        } catch (e: Exception) {
            e.printStackTrace()
            "{\"success\":false, \"message\":\"Greška u konekciji\"}"
        }
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val url = NetworkConfig.getDashboardDataUrl(currentUserId, currentUsername)
                val jsonResponse = sendHttpRequest(url)
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
                                    authorName = "Admin"
                                )
                            )
                        }
                    }
                    _posts.value = parsedPosts
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleLike(postId: Int) {
        viewModelScope.launch {
            try {
                val url = NetworkConfig.getToggleLikeUrl(currentUserId, postId)
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

    fun loadComments(postId: Int) {
        viewModelScope.launch {
            try {
                val url = NetworkConfig.getCommentsUrl(currentUserId, postId)
                val jsonResponse = sendHttpRequest(url)
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
                                    commenterName = cObj.optString("username", "Korisnik")
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
                val url = NetworkConfig.getAddCommentUrl(currentUserId, postId, text)
                val jsonResponse = sendHttpRequest(url)
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
                val url = NetworkConfig.getEditCommentUrl(currentUserId, commentId, newText)
                val jsonResponse = sendHttpRequest(url)
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
                val url = NetworkConfig.getDeleteCommentUrl(currentUserId, commentId)
                val jsonResponse = sendHttpRequest(url)
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
}
