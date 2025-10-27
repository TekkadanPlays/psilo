package com.example.views.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.views.data.UserRelay
import com.example.views.data.RelayConnectionStatus
import com.example.views.data.RelayCategory
import com.example.views.data.DefaultRelayCategories
import com.example.views.repository.RelayRepository
import com.example.views.repository.RelayStorageManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RelayManagementUiState(
    val relays: List<UserRelay> = emptyList(),
    val connectionStatus: Map<String, RelayConnectionStatus> = emptyMap(),
    val showAddRelayDialog: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val relayCategories: List<RelayCategory> = DefaultRelayCategories.getAllDefaultCategories(),
    val outboxRelays: List<UserRelay> = emptyList(),
    val inboxRelays: List<UserRelay> = emptyList(),
    val cacheRelays: List<UserRelay> = emptyList()
)

class RelayManagementViewModel(
    private val relayRepository: RelayRepository,
    private val storageManager: RelayStorageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RelayManagementUiState())
    val uiState: StateFlow<RelayManagementUiState> = _uiState.asStateFlow()

    // Current user's pubkey - set when loading data
    private var currentPubkey: String? = null

    // Expose categories separately for easy access from other screens
    val relayCategories: StateFlow<List<RelayCategory>> = _uiState
        .map { it.relayCategories }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DefaultRelayCategories.getAllDefaultCategories())

    init {
        // Collect relay updates from repository
        viewModelScope.launch {
            relayRepository.relays.collect { relays ->
                _uiState.value = _uiState.value.copy(relays = relays)
            }
        }

        // Collect connection status updates from repository
        viewModelScope.launch {
            relayRepository.connectionStatus.collect { connectionStatus ->
                _uiState.value = _uiState.value.copy(connectionStatus = connectionStatus)
            }
        }
    }

    /**
     * Load relay data for a specific user (pubkey)
     * Call this when user logs in or switches accounts
     */
    fun loadUserRelays(pubkey: String) {
        currentPubkey = pubkey

        viewModelScope.launch {
            // Load categories
            val categories = storageManager.loadCategories(pubkey)

            // Load personal relays
            val outbox = storageManager.loadOutboxRelays(pubkey)
            val inbox = storageManager.loadInboxRelays(pubkey)
            val cache = storageManager.loadCacheRelays(pubkey)

            _uiState.value = _uiState.value.copy(
                relayCategories = categories,
                outboxRelays = outbox,
                inboxRelays = inbox,
                cacheRelays = cache
            )
        }
    }

    /**
     * Save current relay data to storage
     */
    private fun saveToStorage() {
        currentPubkey?.let { pubkey ->
            storageManager.saveCategories(pubkey, _uiState.value.relayCategories)
            storageManager.saveOutboxRelays(pubkey, _uiState.value.outboxRelays)
            storageManager.saveInboxRelays(pubkey, _uiState.value.inboxRelays)
            storageManager.saveCacheRelays(pubkey, _uiState.value.cacheRelays)
        }
    }

    fun showAddRelayDialog() {
        _uiState.value = _uiState.value.copy(showAddRelayDialog = true)
    }

    fun hideAddRelayDialog() {
        _uiState.value = _uiState.value.copy(showAddRelayDialog = false, errorMessage = null)
    }

    fun addRelay(url: String, read: Boolean, write: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            relayRepository.addRelay(url, read, write)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showAddRelayDialog = false
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to add relay"
                    )
                }
        }
    }

    fun removeRelay(url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            relayRepository.removeRelay(url)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to remove relay"
                    )
                }
        }
    }

    fun refreshRelayInfo(url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            relayRepository.refreshRelayInfo(url)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to refresh relay info"
                    )
                }
        }
    }

    fun testRelayConnection(url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            relayRepository.testRelayConnection(url)
                .onSuccess { isConnected ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    // Connection status will be updated via the repository's StateFlow
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to test connection"
                    )
                }
        }
    }

    fun updateRelaySettings(url: String, read: Boolean, write: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            relayRepository.updateRelaySettings(url, read, write)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to update relay settings"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // Category management methods
    fun addCategory(category: RelayCategory) {
        val currentCategories = _uiState.value.relayCategories
        _uiState.value = _uiState.value.copy(
            relayCategories = currentCategories + category
        )
        saveToStorage()
    }

    fun updateCategory(categoryId: String, updatedCategory: RelayCategory) {
        val currentCategories = _uiState.value.relayCategories
        _uiState.value = _uiState.value.copy(
            relayCategories = currentCategories.map {
                if (it.id == categoryId) updatedCategory else it
            }
        )
        saveToStorage()
    }

    fun deleteCategory(categoryId: String) {
        val currentCategories = _uiState.value.relayCategories
        _uiState.value = _uiState.value.copy(
            relayCategories = currentCategories.filter { it.id != categoryId }
        )
        saveToStorage()
    }

    fun addRelayToCategory(categoryId: String, relay: UserRelay) {
        val currentCategories = _uiState.value.relayCategories
        _uiState.value = _uiState.value.copy(
            relayCategories = currentCategories.map { category ->
                if (category.id == categoryId) {
                    category.copy(relays = category.relays + relay)
                } else {
                    category
                }
            }
        )
        saveToStorage()
    }

    fun removeRelayFromCategory(categoryId: String, relayUrl: String) {
        val currentCategories = _uiState.value.relayCategories
        _uiState.value = _uiState.value.copy(
            relayCategories = currentCategories.map { category ->
                if (category.id == categoryId) {
                    category.copy(relays = category.relays.filter { it.url != relayUrl })
                } else {
                    category
                }
            }
        )
        saveToStorage()
    }

    // Personal relay management methods
    fun addOutboxRelay(relay: UserRelay) {
        _uiState.value = _uiState.value.copy(
            outboxRelays = _uiState.value.outboxRelays + relay
        )
        saveToStorage()
    }

    fun removeOutboxRelay(url: String) {
        _uiState.value = _uiState.value.copy(
            outboxRelays = _uiState.value.outboxRelays.filter { it.url != url }
        )
        saveToStorage()
    }

    fun addInboxRelay(relay: UserRelay) {
        _uiState.value = _uiState.value.copy(
            inboxRelays = _uiState.value.inboxRelays + relay
        )
        saveToStorage()
    }

    fun removeInboxRelay(url: String) {
        _uiState.value = _uiState.value.copy(
            inboxRelays = _uiState.value.inboxRelays.filter { it.url != url }
        )
        saveToStorage()
    }

    fun addCacheRelay(relay: UserRelay) {
        _uiState.value = _uiState.value.copy(
            cacheRelays = _uiState.value.cacheRelays + relay
        )
        saveToStorage()
    }

    fun removeCacheRelay(url: String) {
        _uiState.value = _uiState.value.copy(
            cacheRelays = _uiState.value.cacheRelays.filter { it.url != url }
        )
        saveToStorage()
    }

    /**
     * Set a category as favorite (for home feed)
     * Only one category can be favorite at a time
     */
    fun setFavoriteCategory(categoryId: String) {
        val currentCategories = _uiState.value.relayCategories
        _uiState.value = _uiState.value.copy(
            relayCategories = currentCategories.map { category ->
                category.copy(isFavorite = category.id == categoryId)
            }
        )
        saveToStorage()
    }

    /**
     * Get the favorite category (for home feed)
     */
    fun getFavoriteCategory(): RelayCategory? {
        return _uiState.value.relayCategories.firstOrNull { it.isFavorite }
    }

    /**
     * Get all relay URLs from the favorite category
     */
    fun getFavoriteCategoryRelayUrls(): List<String> {
        return getFavoriteCategory()?.relays?.map { it.url } ?: emptyList()
    }

    /**
     * Get all relay URLs from all General categories
     */
    fun getAllGeneralRelayUrls(): List<String> {
        return _uiState.value.relayCategories
            .filter { it.name.contains("General", ignoreCase = true) }
            .flatMap { category -> category.relays.map { it.url } }
            .distinct()
    }

    /**
     * Fetch user's relay list from the network using NIP-65
     */
    fun fetchUserRelaysFromNetwork(pubkey: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            relayRepository.fetchUserRelayList(pubkey)
                .onSuccess { relays ->
                    // Categorize relays based on read/write permissions
                    val outbox = relays.filter { it.write }
                    val inbox = relays.filter { it.read }

                    _uiState.value = _uiState.value.copy(
                        outboxRelays = outbox,
                        inboxRelays = inbox,
                        isLoading = false
                    )
                    saveToStorage()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to fetch relay list from network"
                    )
                }
        }
    }
}
