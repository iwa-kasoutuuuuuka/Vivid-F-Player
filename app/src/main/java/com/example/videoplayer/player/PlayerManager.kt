package com.example.videoplayer.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession

/**
 * ExoPlayerのインスタンスを管理し、再生制御を行います。
 */
class PlayerManager(context: Context) {
    val player: ExoPlayer = PlayerHolder.getPlayer(context)
    var mediaSession: MediaSession? = null
    var onError: ((Exception) -> Unit)? = null

    init {
        mediaSession = PlayerHolder.getMediaSession(context)
    }

    var speed: Float = 1.0f
        set(value) {
            field = value
            player.setPlaybackSpeed(value)
        }

    fun play(video: com.example.videoplayer.data.model.VideoFile, subtitleConfigs: List<MediaItem.SubtitleConfiguration> = emptyList(), startPosition: Long = 0) {
        val mediaItem = MediaItem.Builder()
            .setUri(video.uri)
            .setSubtitleConfigurations(subtitleConfigs)
            .build()
        player.setMediaItem(mediaItem)
        if (startPosition > 0) {
            player.seekTo(startPosition)
        }
        player.prepare()
        player.play()
    }

    fun play(uri: android.net.Uri, position: Long = 0) {
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.seekTo(position)
        player.play()
    }

    fun release() {
        // 全体のプレイヤー/セッションを解放 / Release global player/session
        PlayerHolder.release()
    }
}
