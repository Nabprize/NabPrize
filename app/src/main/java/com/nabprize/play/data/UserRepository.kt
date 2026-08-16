package com.nabprize.play.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val npCoins: Long = 0,
    val totalWins: Long = 0,
    val totalLosses: Long = 0,
    val totalPracticeMatches: Long = 0,
    val lifetimeCoinsEarned: Long = 0,
    val tickets: Int = 0,
    val dailyAdsWatched: Int = 0,
    val lastAdWatchDate: String = "",
    val checkInDay: Int = 0,
    val lastCheckInDate: String = "",
    val dailyTicketsEarned: Int = 0,
    val lastResetDate: String = "",
    val currentTicketUsage: Int = 0,
    // Today's stats (reset daily)
    val todayMatchesPlayed: Int = 0,
    val todayCoinsEarned: Long = 0,
    val lastPlayDate: String = "",
    val createdAt: Timestamp? = null
)

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val usersCol = db.collection("users")
    private val redemptionsCol = db.collection("redemptions")

    private fun uid() = auth.currentUser?.uid

    // ─── Fetch User Profile (with Midnight Reset) ─────────────

    suspend fun fetchProfile(): Result<UserProfile> {
        return try {
            val id = uid() ?: return Result.failure(Exception("Login nahi hai"))
            val doc = usersCol.document(id).get().await()
            if (!doc.exists()) {
                return Result.failure(Exception("User data nahi mila"))
            }

            var profile = doc.toObject(UserProfile::class.java) ?: UserProfile()
            val today = getTodayDate()

            // ── Midnight Reset Logic ──
            if (profile.lastResetDate.isNotEmpty() && profile.lastResetDate != today) {
                val expiredTickets = profile.tickets
                val bonusCoins = expiredTickets * 20L
                val updates = hashMapOf<String, Any>(
                    "tickets" to 0,
                    "dailyAdsWatched" to 0,
                    "dailyTicketsEarned" to 0,
                    "currentTicketUsage" to 0,
                    "lastResetDate" to today
                )
                if (bonusCoins > 0) {
                    updates["npCoins"] = FieldValue.increment(bonusCoins)
                }
                usersCol.document(id).set(updates, SetOptions.merge()).await()

                // Fetch refreshed profile after reset
                val updatedDoc = usersCol.document(id).get().await()
                profile = updatedDoc.toObject(UserProfile::class.java) ?: profile
            } else if (profile.lastResetDate.isEmpty()) {
                usersCol.document(id).set(mapOf("lastResetDate" to today), SetOptions.merge()).await()
                profile = profile.copy(lastResetDate = today)
            }

            // ── Daily stats reset if not today ──
            if (profile.lastPlayDate.isNotEmpty() && profile.lastPlayDate != today) {
                val updates = mapOf(
                    "todayMatchesPlayed" to 0,
                    "todayCoinsEarned" to 0,
                    "lastPlayDate" to today
                )
                usersCol.document(id).set(updates, SetOptions.merge()).await()
                profile = profile.copy(todayMatchesPlayed = 0, todayCoinsEarned = 0, lastPlayDate = today)
            }

            Result.success(profile)
        } catch (e: Exception) {
            Log.e("UserRepository", "fetchProfile error: ${e.message}", e)
            Result.failure(Exception("Data load karne mein problem aa rahi hai: ${e.message}"))
        }
    }

    // ─── Update Display Name ─────────────────────────────────

    suspend fun updateDisplayName(name: String): Result<Unit> {
        return try {
            val id = uid() ?: return Result.failure(Exception("Login nahi hai"))
            usersCol.document(id).set(mapOf("displayName" to name), SetOptions.merge()).await()
            auth.currentUser?.let { user ->
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user.updateProfile(profileUpdates).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "updateDisplayName error: ${e.message}", e)
            Result.failure(Exception("Name update karne mein problem aa rahi hai"))
        }
    }

    // ─── Add NP-Coins ────────────────────────────────────────

    suspend fun addCoins(amount: Long): Result<Unit> {
        return try {
            val id = uid() ?: return Result.failure(Exception("Login nahi hai"))
            val today = getTodayDate()
            val updates = hashMapOf<String, Any>(
                "npCoins" to FieldValue.increment(amount),
                "todayCoinsEarned" to FieldValue.increment(amount),
                "lifetimeCoinsEarned" to FieldValue.increment(amount),
                "lastPlayDate" to today
            )
            usersCol.document(id).set(updates, SetOptions.merge()).await()
            Log.d("UserRepository", "addCoins +$amount success for uid=$id")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "addCoins error: ${e.message}", e)
            Result.failure(Exception("Coins add karne mein problem aa rahi hai: ${e.message}"))
        }
    }

    /** Records every completed bot-practice game, including losses. */
    suspend fun recordPracticeResult(isWin: Boolean, boxesCaptured: Int): Result<Unit> {
        return try {
            val id = uid() ?: return Result.failure(Exception("Login nahi hai"))
            val today = getTodayDate()
            val updates = hashMapOf<String, Any>(
                "todayMatchesPlayed" to FieldValue.increment(1L),
                "totalPracticeMatches" to FieldValue.increment(1L),
                "lastPlayDate" to today
            )
            // Practice rewards are based on skill: every captured box is 1 NP-Coin,
            // whether the player wins or loses the game.
            val rewardCoins = boxesCaptured.coerceAtLeast(0).toLong()
            updates["npCoins"] = FieldValue.increment(rewardCoins)
            updates["todayCoinsEarned"] = FieldValue.increment(rewardCoins)
            updates["lifetimeCoinsEarned"] = FieldValue.increment(rewardCoins)
            usersCol.document(id).set(updates, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "recordPracticeResult error: ${e.message}", e)
            Result.failure(Exception("Practice result save nahi ho saka"))
        }
    }

    // ─── Update Match Result ─────────────────────────────────

    suspend fun updateMatchResult(matchId: String, isWin: Boolean): Result<Unit> {
        return try {
            val id = uid() ?: return Result.failure(Exception("Login nahi hai"))
            val today = getTodayDate()
            val coinReward = if (isWin) 40L else 2L
            if (matchId.isBlank()) return Result.failure(Exception("Match ID missing hai"))
            val userRef = usersCol.document(id)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val processed = (snapshot.get("processedMatchIds") as? List<*>)
                    ?.filterIsInstance<String>()
                    .orEmpty()
                if (processed.contains(matchId)) return@runTransaction

                val tickets = (snapshot.getLong("tickets") ?: 0L).toInt()
                val usage = (snapshot.getLong("currentTicketUsage") ?: 0L).toInt()
                val ticketUpdates = if (isWin && usage + 1 < 15) {
                    mapOf("currentTicketUsage" to usage + 1)
                } else {
                    mapOf(
                        "tickets" to maxOf(0, tickets - 1),
                        "currentTicketUsage" to 0
                    )
                }
                val updates = hashMapOf<String, Any>(
                    (if (isWin) "totalWins" else "totalLosses") to FieldValue.increment(1L),
                    "npCoins" to FieldValue.increment(coinReward),
                    "todayMatchesPlayed" to FieldValue.increment(1L),
                    "todayCoinsEarned" to FieldValue.increment(coinReward),
                    "lifetimeCoinsEarned" to FieldValue.increment(coinReward),
                    "lastPlayDate" to today,
                    "processedMatchIds" to FieldValue.arrayUnion(matchId)
                )
                updates.putAll(ticketUpdates)
                transaction.set(userRef, updates, SetOptions.merge())
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "updateMatchResult error: ${e.message}", e)
            Result.failure(Exception("Match result update karne mein problem aa rahi hai"))
        }
    }

    // ─── Ticket Reusability & Consumption Rules ──────────────

    suspend fun consumeOrReuseTicket(isWinner: Boolean): Result<Unit> {
        return try {
            val id = uid() ?: return Result.failure(Exception("Login nahi hai"))
            val doc = usersCol.document(id).get().await()
            val currentTickets = (doc.getLong("tickets") ?: 0).toInt()
            val currentUsage = (doc.getLong("currentTicketUsage") ?: 0).toInt()

            if (isWinner) {
                if (currentUsage + 1 >= 15) {
                    usersCol.document(id).set(
                        mapOf("tickets" to maxOf(0, currentTickets - 1), "currentTicketUsage" to 0),
                        SetOptions.merge()
                    ).await()
                } else {
                    usersCol.document(id).set(
                        mapOf("currentTicketUsage" to currentUsage + 1),
                        SetOptions.merge()
                    ).await()
                }
            } else {
                usersCol.document(id).set(
                    mapOf("tickets" to maxOf(0, currentTickets - 1), "currentTicketUsage" to 0),
                    SetOptions.merge()
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "consumeOrReuseTicket error: ${e.message}", e)
            Result.failure(Exception("Ticket status update nahi ho saka"))
        }
    }

    // ─── Watch Rewarded Ad for Ticket Progress ───────────────

    suspend fun watchRewardedAdForTicket(): Result<Pair<Int, Boolean>> {
        return try {
            val id = uid() ?: return Result.failure(Exception("Login nahi hai"))
            val today = getTodayDate()
            val userRef = usersCol.document(id)
            val outcome = db.runTransaction { transaction ->
                val doc = transaction.get(userRef)
                val lastAdDate = doc.getString("lastAdWatchDate") ?: ""
                val dailyAds = if (lastAdDate == today) (doc.getLong("dailyAdsWatched") ?: 0).toInt() else 0
                val dailyTickets = if (lastAdDate == today) (doc.getLong("dailyTicketsEarned") ?: 0).toInt() else 0
                val currentTickets = (doc.getLong("tickets") ?: 0).toInt()
                if (dailyTickets >= 8) throw IllegalStateException("Aaj ke 8 tickets ka cap complete ho chuka hai. Kal try karein!")

                val newAdsWatched = dailyAds + 1
                val earnedNewTicket = newAdsWatched % 10 == 0
                val updates = hashMapOf<String, Any>(
                    "dailyAdsWatched" to newAdsWatched,
                    "lastAdWatchDate" to today
                )
                if (earnedNewTicket) {
                    updates["tickets"] = currentTickets + 1
                    updates["dailyTicketsEarned"] = dailyTickets + 1
                }
                transaction.set(userRef, updates, SetOptions.merge())
                Pair(newAdsWatched, earnedNewTicket)
            }.await()
            val (newAdsWatched, earnedNewTicket) = outcome
            Log.d("UserRepository", "watchRewardedAdForTicket: adsWatched=$newAdsWatched, earnedNewTicket=$earnedNewTicket")
            Result.success(outcome)
        } catch (e: Exception) {
            Log.e("UserRepository", "watchRewardedAdForTicket error: ${e.message}", e)
            if (e.message?.contains("cap complete") == true) Result.failure(e)
            else Result.failure(Exception("Ad progress update nahi ho saki: ${e.message}"))
        }
    }

    // ─── Redeem Reward ────────────────────────────────────────

    suspend fun redeemReward(
        rewardId: String,
        rewardName: String,
        cost: Long,
        primaryDetail: String,
        secondaryDetail: String?
    ): Result<Unit> {
        return try {
            val id = uid() ?: return Result.failure(Exception("Login nahi hai"))
            val userRef = usersCol.document(id)
            val redemptionRef = redemptionsCol.document()
            // Deducting the balance and creating the claim must be one atomic operation.
            // Otherwise a network failure between the two writes could take coins without a claim.
            db.runTransaction { transaction ->
                val doc = transaction.get(userRef)
                val currentCoins = doc.getLong("npCoins") ?: 0L
                if (currentCoins < cost) {
                    throw IllegalStateException("Aapke paas balance kam hai ($currentCoins/$cost NP-Coins)")
                }
                val redemptionDoc = hashMapOf<String, Any>(
                    "userId" to id,
                    "username" to (doc.getString("username") ?: ""),
                    "email" to (doc.getString("email") ?: (auth.currentUser?.email ?: "")),
                    "rewardId" to rewardId,
                    "rewardName" to rewardName,
                    "cost" to cost,
                    "primaryDetail" to primaryDetail,
                    "secondaryDetail" to (secondaryDetail ?: ""),
                    "status" to "PENDING",
                    "requestedAt" to Timestamp.now()
                )
                transaction.update(userRef, "npCoins", currentCoins - cost)
                transaction.set(redemptionRef, redemptionDoc)
            }.await()

            Log.d("UserRepository", "Redemption successful: $rewardName cost=$cost")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "redeemReward error: ${e.message}", e)
            Result.failure(Exception("Redemption request fail ho gayi: ${e.message}"))
        }
    }

    // ─── Daily Check-In ──────────────────────────────────────

    suspend fun dailyCheckIn(): Result<Pair<Int, Boolean>> {
        return try {
            val id = uid() ?: return Result.failure(Exception("Login nahi hai"))
            val today = getTodayDate()
            val userRef = usersCol.document(id)
            val outcome = db.runTransaction { transaction ->
                val doc = transaction.get(userRef)
                val lastCheckIn = doc.getString("lastCheckInDate") ?: ""
                val currentDay = (doc.getLong("checkInDay") ?: 0).toInt()
                if (lastCheckIn == today) throw IllegalStateException("Aaj ka check-in ho gaya hai!")

                val newDay = if (lastCheckIn == getYesterdayDate()) {
                    if (currentDay >= 7) 1 else currentDay + 1
                } else 1
                val rewardCoins = when (newDay) {
                    1 -> 5L; 2 -> 10L; 3 -> 15L; 4 -> 20L
                    5 -> 25L; 6 -> 30L; 7 -> 50L; else -> 5L
                }
                transaction.set(userRef, mapOf(
                    "checkInDay" to newDay,
                    "lastCheckInDate" to today,
                    "npCoins" to FieldValue.increment(rewardCoins),
                    "todayCoinsEarned" to FieldValue.increment(rewardCoins),
                    "lifetimeCoinsEarned" to FieldValue.increment(rewardCoins),
                    "lastPlayDate" to today
                ), SetOptions.merge())
                Pair(newDay, rewardCoins)
            }.await()
            val (newDay, rewardCoins) = outcome

            Log.d("UserRepository", "dailyCheckIn: day=$newDay, reward=$rewardCoins")
            Result.success(Pair(newDay, newDay == 7))
        } catch (e: Exception) {
            Log.e("UserRepository", "dailyCheckIn error: ${e.message}", e)
            if (e.message?.contains("Aaj ka") == true) Result.failure(e)
            else Result.failure(Exception("Check-in mein problem aa rahi hai: ${e.message}"))
        }
    }

    // ─── Helpers ─────────────────────────────────────────────

    fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getYesterdayDate(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(cal.time)
    }
}
