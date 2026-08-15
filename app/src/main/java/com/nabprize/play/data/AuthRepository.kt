package com.nabprize.play.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val context: Context
) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("966943519687-k7f3rb9r08e7v7c0uug2jqefj1mj9t0k.apps.googleusercontent.com")
        .requestEmail()
        .build()

    private val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    val currentUser get() = auth.currentUser
    val isLoggedIn get() = currentUser != null

    fun getGoogleSignInIntent(): Intent = googleSignInClient.signInIntent

    // ─── Email/Password Sign In ──────────────────────────────

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("Ye email register nahi hai. Pehle account banao."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Email ya password galat hai. Dobara check karo."))
        } catch (e: Exception) {
            Result.failure(Exception("Login mein problem aa rahi hai. Internet check karo."))
        }
    }

    // ─── Email/Password Sign Up ──────────────────────────────

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String,
        username: String
    ): Result<Unit> {
        return try {
            // 1. Check email uniqueness
            val emailExists = db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
                .documents.isNotEmpty()

            if (emailExists) {
                return Result.failure(Exception("Ye email pehle se registered hai. Login karo."))
            }

            // 2. Check username uniqueness
            val usernameExists = db.collection("users")
                .whereEqualTo("username", username.lowercase().trim())
                .limit(1)
                .get()
                .await()
                .documents.isNotEmpty()

            if (usernameExists) {
                return Result.failure(Exception("Ye username kisi ne le liya hai. Doosra try karo."))
            }

            // 3. Create account
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("Account banane mein problem aa rahi hai."))

            // 4. Update display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            user.updateProfile(profileUpdates).await()

            // 5. Create user document in Firestore
            createUserDocument(user.uid, displayName, email, username)

            Result.success(Unit)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("Ye email pehle se registered hai. Login karo."))
        } catch (e: Exception) {
            // If it's our custom exception, pass it through
            if (e.message?.contains("register") == true || e.message?.contains("username") == true) {
                Result.failure(e)
            } else {
                Result.failure(Exception("Account banane mein problem aa rahi hai. Dobara try karo."))
            }
        }
    }

    // ─── Google Sign In ──────────────────────────────────────

    suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: return Result.failure(Exception("Google sign-in mein problem aa rahi hai."))

            // Create user document if new user
            val doc = db.collection("users").document(user.uid).get().await()
            if (!doc.exists()) {
                val username = user.email?.substringBefore("@") ?: "user_${user.uid.take(8)}"
                createUserDocument(user.uid, user.displayName ?: "", user.email ?: "", username)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Google sign-in mein problem aa rahi hai. Internet check karo."))
        }
    }

    suspend fun handleGoogleSignInResult(data: Intent?): Result<Unit> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            signInWithGoogle(account.idToken ?: return Result.failure(Exception("Google sign-in incomplete hai.")))
        } catch (e: ApiException) {
            when (e.statusCode) {
                12501 -> Result.failure(Exception("Google sign-in cancel kiya."))
                12500 -> Result.failure(Exception("Google sign-in mein error aaya. Dobara try karo."))
                7 -> Result.failure(Exception("Network problem hai. Internet check karo."))
                else -> Result.failure(Exception("Google sign-in mein problem aa rahi hai."))
            }
        }
    }

    // ─── Password Reset ──────────────────────────────────────

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            // Check if email exists first
            val emailExists = db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
                .documents.isNotEmpty()

            if (!emailExists) {
                return Result.failure(Exception("Ye email register nahi hai."))
            }

            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e.message?.contains("register") == true) {
                Result.failure(e)
            } else {
                Result.failure(Exception("Reset email bhejne mein problem aa rahi hai."))
            }
        }
    }

    // ─── Sign Out ────────────────────────────────────────────

    fun signOut() {
        auth.signOut()
        googleSignInClient.signOut()
    }

    // ─── Create User Document ────────────────────────────────

    private suspend fun createUserDocument(
        uid: String,
        displayName: String,
        email: String,
        username: String
    ) {
        val userDoc = hashMapOf(
            "uid" to uid,
            "displayName" to displayName,
            "username" to username.lowercase().trim(),
            "email" to email.lowercase().trim(),
            "photoUrl" to (auth.currentUser?.photoUrl?.toString() ?: ""),
            "npCoins" to 0,
            "totalWins" to 0,
            "totalLosses" to 0,
            "tickets" to 0,
            "dailyAdsWatched" to 0,
            "lastAdWatchDate" to "",
            "checkInDay" to 0,
            "lastCheckInDate" to "",
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        db.collection("users").document(uid).set(userDoc).await()
    }
}
