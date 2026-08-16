package com.nabprize.play.ui.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.nabprize.play.data.UserProfile
import com.nabprize.play.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class UserUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile = UserProfile(),
    val error: String? = null,
    val successMessage: String? = null,
    val checkInDay: Int = 0,
    val isCheckedInToday: Boolean = false,
    val nextCheckInDay: Int = 1,
    val adsTowardNextTicket: Int = 0,
    val isDailyCapReached: Boolean = false,
    val todayMatchesPlayed: Int = 0,
    val todayCoinsEarned: Long = 0
)

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = UserRepository()
    private var profileListener: ListenerRegistration? = null

    private val _state = MutableStateFlow(UserUiState())
    val state: StateFlow<UserUiState> = _state

    init {
        fetchProfile()
    }

    fun fetchProfile() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repo.fetchProfile()
            if (result.isSuccess) {
                applyProfile(result.getOrNull() ?: UserProfile())
                startProfileListener()
            } else {
                Log.e("UserViewModel", "fetchProfile error: ${result.exceptionOrNull()?.message}")
                _state.value = UserUiState(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Data load nahi ho paya"
                )
            }
        }
    }

    private fun startProfileListener() {
        if (profileListener != null) return
        profileListener = repo.observeProfile { result ->
            if (result.isSuccess) {
                applyProfile(result.getOrNull() ?: UserProfile())
            } else {
                Log.e("UserViewModel", "live profile error: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun clearSession() {
        profileListener?.remove()
        profileListener = null
        _state.value = UserUiState(isLoading = false)
    }

    private fun applyProfile(profile: UserProfile) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = sdf.format(cal.time)
        val isCheckedInToday = profile.lastCheckInDate == today
        val nextDay = when {
            isCheckedInToday -> profile.checkInDay
            profile.lastCheckInDate == yesterday -> if (profile.checkInDay >= 7) 1 else profile.checkInDay + 1
            else -> 1
        }
        val todayMatches = if (profile.lastPlayDate == today) profile.todayMatchesPlayed else 0
        val todayCoins = if (profile.lastPlayDate == today) profile.todayCoinsEarned else 0L
        _state.value = UserUiState(
            isLoading = false,
            profile = profile,
            checkInDay = profile.checkInDay,
            isCheckedInToday = isCheckedInToday,
            nextCheckInDay = nextDay,
            todayMatchesPlayed = todayMatches,
            todayCoinsEarned = todayCoins
        )
    }

    override fun onCleared() {
        clearSession()
        super.onCleared()
    }

    fun dailyCheckIn() {
        viewModelScope.launch {
            _state.value = _state.value.copy(error = null, successMessage = null)
            val result = repo.dailyCheckIn()
            if (result.isSuccess) {
                val (day, isWeekComplete) = result.getOrNull() ?: Pair(1, false)
                val msg = if (isWeekComplete) {
                    "Hurray! 7 din ka streak complete! 50 coins mile!"
                } else {
                    "Day $day complete! ${getDayReward(day)} coins mile!"
                }
                _state.value = _state.value.copy(
                    checkInDay = day,
                    isCheckedInToday = true,
                    successMessage = msg
                )
                fetchProfile()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Check-in failed"
                Log.e("UserViewModel", "dailyCheckIn error: $err")
                _state.value = _state.value.copy(error = err)
            }
        }
    }

    fun watchRewardedAdForTicket(onRewardEarned: (isNewTicket: Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _state.value = _state.value.copy(error = null, successMessage = null)
            val result = repo.watchRewardedAdForTicket()
            if (result.isSuccess) {
                val (ads, isNewTicket) = result.getOrNull() ?: Pair(0, false)
                Log.d("UserViewModel", "watchRewardedAdForTicket success: ads=$ads, isNewTicket=$isNewTicket")
                if (isNewTicket) {
                    _state.value = _state.value.copy(successMessage = "Mubarak ho! 1 Ticket unlock ho gaya!")
                }
                onRewardEarned(isNewTicket)
                fetchProfile()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Ad progress record nahi ho saki"
                Log.e("UserViewModel", "watchRewardedAdForTicket error: $err")
                _state.value = _state.value.copy(error = err)
            }
        }
    }

    fun redeemReward(
        rewardId: String,
        rewardName: String,
        cost: Long,
        primaryDetail: String,
        secondaryDetail: String?,
        onComplete: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repo.redeemReward(rewardId, rewardName, cost, primaryDetail, secondaryDetail)
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    successMessage = "$rewardName claim request submit ho gayi!"
                )
                fetchProfile()
                onComplete(true, null)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Redemption failed"
                _state.value = _state.value.copy(isLoading = false, error = err)
                onComplete(false, err)
            }
        }
    }

    fun addCoins(amount: Long, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            Log.d("UserViewModel", "addCoins requested: +$amount")
            val res = repo.addCoins(amount)
            if (res.isSuccess) {
                Log.d("UserViewModel", "addCoins success: +$amount")
                fetchProfile()
                onComplete(true)
            } else {
                val error = res.exceptionOrNull()?.message ?: "Coins add nahi ho sake"
                Log.e("UserViewModel", "addCoins failed: $error")
                _state.value = _state.value.copy(error = error)
                onComplete(false)
            }
        }
    }

    fun recordPracticeResult(isWin: Boolean, boxesCaptured: Int, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repo.recordPracticeResult(isWin, boxesCaptured)
            if (result.isSuccess) {
                fetchProfile()
                onComplete(true)
            } else {
                val error = result.exceptionOrNull()?.message ?: "Practice reward add nahi ho saka"
                Log.e("UserViewModel", "recordPracticeResult failed: $error")
                _state.value = _state.value.copy(error = error)
                onComplete(false)
            }
        }
    }

    fun updateMatchResult(matchId: String, isWin: Boolean) {
        viewModelScope.launch {
            val result = repo.updateMatchResult(matchId, isWin)
            if (result.isSuccess) fetchProfile()
            else Log.e("UserViewModel", "updateMatchResult failed: ${result.exceptionOrNull()?.message}")
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
    fun clearSuccess() { _state.value = _state.value.copy(successMessage = null) }

    private fun getDayReward(day: Int): Int = when (day) {
        1 -> 5; 2 -> 10; 3 -> 15; 4 -> 20; 5 -> 25; 6 -> 30; 7 -> 50
        else -> 5
    }
}
