package com.example.views.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.views.auth.AmberSignerManager
import com.example.views.auth.AmberState
import com.example.views.data.AuthState
import com.example.views.data.UserProfile
import com.example.views.data.GUEST_PROFILE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.vitorpamplona.quartz.nip19Bech32.Nip19Parser
import com.vitorpamplona.quartz.nip19Bech32.entities.NPub

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val amberSignerManager = AmberSignerManager(application)
    
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    init {
        // Start with guest mode
        _authState.value = AuthState(
            isAuthenticated = false,
            isGuest = true,
            userProfile = GUEST_PROFILE,
            isLoading = false
        )
        
        // Observe Amber state changes
        viewModelScope.launch {
            combine(
                amberSignerManager.state,
                _authState
            ) { amberState, currentAuthState ->
                when (amberState) {
                    is AmberState.NotInstalled -> {
                        currentAuthState.copy(
                            isAuthenticated = false,
                            isGuest = true,
                            userProfile = GUEST_PROFILE,
                            error = "Amber Signer not installed"
                        )
                    }
                    is AmberState.NotLoggedIn -> {
                        currentAuthState.copy(
                            isAuthenticated = false,
                            isGuest = true,
                            userProfile = GUEST_PROFILE,
                            error = null
                        )
                    }
                    is AmberState.LoggingIn -> {
                        currentAuthState.copy(
                            isLoading = true,
                            error = null
                        )
                    }
                    is AmberState.LoggedIn -> {
                        // Use pubkey directly for now - npub conversion will be implemented later
                        val npub = amberState.pubKey
                        
                        val userProfile = UserProfile(
                            pubkey = amberState.pubKey,
                            displayName = "Nostr User",
                            about = "Signed in with Amber Signer",
                            createdAt = System.currentTimeMillis()
                        )
                        
                        currentAuthState.copy(
                            isAuthenticated = true,
                            isGuest = false,
                            userProfile = userProfile,
                            isLoading = false,
                            error = null
                        )
                    }
                    is AmberState.Error -> {
                        currentAuthState.copy(
                            isAuthenticated = false,
                            isGuest = true,
                            userProfile = GUEST_PROFILE,
                            isLoading = false,
                            error = amberState.message
                        )
                    }
                }
            }.collect { newState ->
                _authState.value = newState
            }
        }
    }
    
    fun loginWithAmber(): android.content.Intent {
        return amberSignerManager.createLoginIntent()
    }
    
    fun handleLoginResult(resultCode: Int, data: android.content.Intent?) {
        amberSignerManager.handleLoginResult(resultCode, data)
    }
    
    fun logout() {
        amberSignerManager.logout()
        _authState.value = AuthState(
            isAuthenticated = false,
            isGuest = true,
            userProfile = GUEST_PROFILE,
            isLoading = false,
            error = null
        )
    }
    
    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }
    
    fun getCurrentSigner() = amberSignerManager.getCurrentSigner()
    fun getCurrentPubKey() = amberSignerManager.getCurrentPubKey()
}
