package com.example.videoplayer.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession

import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl

object PlayerHolder {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    fun getPlayer(context: Context): ExoPlayer {
        if (player == null) {
            val appContext = context.applicationContext

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_ALL)
                .build()

            // B: Hardware Decoding Optimization
            val renderersFactory = DefaultRenderersFactory(appContext)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                .setEnableDecoderFallback(true)

            // B: Buffering Optimization for stability
            val loadControl: LoadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    30_000, // Min buffer
                    60_000, // Max buffer
                    2_500,  // Buffer for playback
                    5_000   // Buffer for playback after rebuffer
                )
                .build()

            // Support for SMB and other protocols
            val baseDataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(appContext)
            val smbDataSourceFactory = SmbDataSource.Factory()
            
            val dataSourceFactory = androidx.media3.datasource.DataSource.Factory {
                object : androidx.media3.datasource.DataSource {
                    private var activeDataSource: androidx.media3.datasource.DataSource? = null
                    private val baseDataSource = baseDataSourceFactory.createDataSource()
                    private val smbDataSource = smbDataSourceFactory.createDataSource()

                    override fun addTransferListener(transferListener: androidx.media3.datasource.TransferListener) {
                        baseDataSource.addTransferListener(transferListener)
                        smbDataSource.addTransferListener(transferListener)
                    }

                    override fun open(dataSpec: androidx.media3.datasource.DataSpec): Long {
                        activeDataSource = if (dataSpec.uri.scheme == "smb") {
                            smbDataSource
                        } else {
                            baseDataSource
                        }
                        return activeDataSource!!.open(dataSpec)
                    }

                    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                        return activeDataSource?.read(buffer, offset, length) ?: -1
                    }

                    override fun getUri(): android.net.Uri? = activeDataSource?.getUri()

                    override fun getResponseHeaders(): Map<String, List<String>> = 
                        activeDataSource?.getResponseHeaders() ?: emptyMap()

                    override fun close() {
                        activeDataSource?.close()
                        activeDataSource = null
                    }
                }
            }

            player = ExoPlayer.Builder(appContext, renderersFactory)
                .setAudioAttributes(audioAttributes, true)
                .setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory))
                .setLoadControl(loadControl)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .setSeekBackIncrementMs(10000)
                .setSeekForwardIncrementMs(10000)
                .build()
        }
        return player!!
    }

    fun getMediaSession(context: Context): MediaSession {
        if (mediaSession == null) {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, 
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            mediaSession = MediaSession.Builder(context.applicationContext, getPlayer(context))
                .setSessionActivity(pendingIntent)
                .build()
        }
        return mediaSession!!
    }

    fun release() {
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
    }
}
