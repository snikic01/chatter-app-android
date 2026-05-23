package com.example.chatterapp.data // Prilagodi svom paketu ako treba

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val apiService: DashboardApiService,
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

    fun loadDashboardData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = apiService.getDashboardData(userId = currentUserId, username = currentUsername)
                if (response.success) {
                    _posts.value = response.posts ?: emptyList()
                    _isAdmin.value = response.isAdmin
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
                val response = apiService.toggleLike(userId = currentUserId, postId = postId)
                if (response.success) {
                    loadDashboardData() // Osveži listu da povuče novi broj lajkova
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadComments(postId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.getComments(userId = currentUserId, postId = postId)
                if (response.success) {
                    _comments.value = response.comments ?: emptyList()
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
                val response = apiService.addComment(userId = currentUserId, postId = postId, commentText = text)
                if (response.success) {
                    loadComments(postId) // Osveži komentare za taj post
                    loadDashboardData() // Osveži i glavnu listu zbog broja komentara
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun editComment(postId: Int, commentId: Int, newText: String) {
        viewModelScope.launch {
            try {
                val response = apiService.editComment(userId = currentUserId, commentId = commentId, commentText = newText)
                if (response.success) {
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
                val response = apiService.deleteComment(userId = currentUserId, commentId = commentId)
                if (response.success) {
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
                val response = apiService.getAdminLogs(userId = currentUserId)
                if (response.success) {
                    _logs.value = response.logs ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addNewPost(title: String, content: String, color: String) {
        viewModelScope.launch {
            try {
                val response = apiService.addPost(
                    userId = currentUserId,
                    username = currentUsername,
                    title = title,
                    content = content,
                    boardColor = color
                )
                if (response.success) {
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
                val response = apiService.deletePost(userId = currentUserId, username = currentUsername, postId = postId)
                if (response.success) {
                    loadDashboardData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
