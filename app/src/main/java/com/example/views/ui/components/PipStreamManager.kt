package com.example.views.ui.components

import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that holds an ExoPlayer instance and stream metadata for
 * Picture-in-Picture playback when the user navigates away from a live stream.
 *
 * The LiveStreamScreen hands off its player here on back-press instead of
 * releasing it, so playback continues in a mini overlay.
 */
object PipStreamManager {

    data class PipState(
        val player: ExoPlayer,
        val addressableId: String,
        val title: String?,
        val hostName: String?
    )

    private val _pipState = MutableStateFlow<PipState?>(null)
    val pipState: StateFlow<PipState?> = _pipState.asStateFlow()

    /** Start PiP — called by LiveStreamScreen when the user navigates back. */
    fun startPip(player: ExoPlayer, addressableId: String, title: String?, hostName: String?) {
        // Release any existing PiP player first
        _pipState.value?.player?.release()
        _pipState.value = PipState(player, addressableId, title, hostName)
    }

    /** Reclaim the player when the user taps PiP to return to the stream screen. */
    fun reclaimPlayer(): PipState? {
        val state = _pipState.value
        _pipState.value = null
        return state
    }

    /** Dismiss PiP and release the player (user swiped it away). */
    fun dismiss() {
        _pipState.value?.player?.release()
        _pipState.value = null
    }

    /** Check if PiP is active for a given addressableId. */
    fun isActiveFor(addressableId: String): Boolean =
        _pipState.value?.addressableId == addressableId
}
