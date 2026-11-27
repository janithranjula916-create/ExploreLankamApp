package com.example.explorelanka.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    var user: FirebaseUser? by mutableStateOf(auth.currentUser)
        private set
    var displayName by mutableStateOf("")
        private set
    var busy by mutableStateOf(false)
        private set
    var notice by mutableStateOf<String?>(null)
        private set

    private val authListener = FirebaseAuth.AuthStateListener { instance ->
        user = instance.currentUser
        val uid = instance.currentUser?.uid
        displayName = ""
        if (uid != null) loadProfile(uid)
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    private fun loadProfile(uid: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users").document(uid).get().await()
                if (user?.uid == uid) {
                    displayName = snapshot.getString("displayName").orEmpty()
                }
            } catch (_: Exception) {
                // Authentication still works if a profile cannot be loaded offline.
            }
        }
    }

    fun signIn(email: String, password: String) = perform {
        require(email.isNotBlank() && password.isNotBlank()) { "Enter email and password." }
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        notice = null
    }

    fun register(name: String, email: String, password: String) = perform {
        require(name.trim().length in 2..60) { "Name must contain 2–60 characters." }
        require(email.contains('@')) { "Enter a valid email address." }
        require(password.length >= 6) { "Password must have at least 6 characters." }
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val uid = checkNotNull(result.user?.uid)
        firestore.collection("users").document(uid).set(
            mapOf(
                "displayName" to name.trim(),
                "email" to email.trim(),
                "createdAt" to FieldValue.serverTimestamp()
            ), SetOptions.merge()
        ).await()
        displayName = name.trim()
        notice = null
    }

    fun resetPassword(email: String) = perform {
        require(email.isNotBlank()) { "Enter your email address first." }
        auth.sendPasswordResetEmail(email.trim()).await()
        notice = "If your email is registered, check your inbox for reset instructions."
    }

    fun updateDisplayName(newName: String) = perform {
        val uid = requireNotNull(user?.uid) { "Please sign in." }
        require(newName.trim().length in 2..60) { "Name must contain 2–60 characters." }
        firestore.collection("users").document(uid).set(
            mapOf("displayName" to newName.trim(), "email" to (user?.email ?: "")),
            SetOptions.merge()
        ).await()
        displayName = newName.trim()
        notice = "Profile saved."
    }

    private fun perform(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        notice = null
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notice = e.localizedMessage ?: "Request failed. Check your connection."
            } finally {
                busy = false
            }
        }
    }

    fun clearNotice() { notice = null }
    fun signOut() { auth.signOut(); displayName = ""; notice = null }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }
}
