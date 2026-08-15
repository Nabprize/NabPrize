package com.nabprize.play.ui.auth

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nabprize.play.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AuthRepository(application)

    private val _state = MutableStateFlow(AuthState(isLoggedIn = repo.isLoggedIn))
    val state: StateFlow<AuthState> = _state

    init {
        // Auto-login check
        if (repo.isLoggedIn) {
            _state.value = AuthState(isLoggedIn = true)
        }
    }

    fun signInWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = _state.value.copy(error = "Email aur password dono zaroori hain")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repo.signInWithEmail(email, password)
            _state.value = if (result.isSuccess) {
                AuthState(isLoggedIn = true)
            } else {
                AuthState(error = result.exceptionOrNull()?.localizedMessage ?: "Login failed")
            }
        }
    }

    fun signUpWithEmail(email: String, password: String, fullName: String, username: String) {
        if (fullName.isBlank()) {
            _state.value = _state.value.copy(error = "Full name zaroori hai")
            return
        }
        if (username.isBlank()) {
            _state.value = _state.value.copy(error = "Username zaroori hai")
            return
        }
        if (!username.matches(Regex("^[a-zA-Z0-9._]+$"))) {
            _state.value = _state.value.copy(error = "Username mein sirf letters, numbers, dots aur underscores hain")
            return
        }
        if (username.length < 3) {
            _state.value = _state.value.copy(error = "Username kam se kam 3 characters ka hona chahiye")
            return
        }
        if (email.isBlank()) {
            _state.value = _state.value.copy(error = "Email zaroori hai")
            return
        }
        if (password.isBlank()) {
            _state.value = _state.value.copy(error = "Password zaroori hai")
            return
        }
        if (password.length < 6) {
            _state.value = _state.value.copy(error = "Password kam se kam 6 characters ka hona chahiye")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repo.signUpWithEmail(email, password, fullName, username)
            _state.value = if (result.isSuccess) {
                AuthState(isLoggedIn = true, successMessage = "Account ban gaya! Welcome!")
            } else {
                AuthState(error = result.exceptionOrNull()?.message ?: "Signup mein problem aa rahi hai")
            }
        }
    }

    fun getGoogleSignInIntent(): Intent = repo.getGoogleSignInIntent()

    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repo.handleGoogleSignInResult(data)
            _state.value = if (result.isSuccess) {
                AuthState(isLoggedIn = true)
            } else {
                AuthState(error = result.exceptionOrNull()?.localizedMessage ?: "Google sign-in failed")
            }
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _state.value = _state.value.copy(error = "Email daalo pehle")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repo.sendPasswordReset(email)
            _state.value = if (result.isSuccess) {
                AuthState(isLoading = false, successMessage = "Password reset email bhej diya!")
            } else {
                AuthState(error = result.exceptionOrNull()?.localizedMessage ?: "Reset email failed")
            }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
    fun clearSuccess() { _state.value = _state.value.copy(successMessage = null) }

    fun signOut() {
        repo.signOut()
        _state.value = AuthState(isLoggedIn = false)
    }
}
