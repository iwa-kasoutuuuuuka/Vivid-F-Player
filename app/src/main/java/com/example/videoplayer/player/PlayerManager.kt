package com.example.videoplayer.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * ExoPlayerのインスタンスを管理し、再生制御を行います。
 */
class PlayerManager(context: Context) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build()
    var onError: ((Exception) -> Unit)? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                onError?.invoke(error)
            }
        })
    }

    var speed: Float = 1.0f
        set(value) {
            field = value
            player.setPlaybackSpeed(value)
        }

    fun play(uri: android.net.Uri, position: Long = 0) {
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.seekTo(position)
        player.play()
    }

    fun release() {
        player.release()
    }
}
