package com.scanner.app.util

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.scanner.app.R
import java.io.IOException

class AudioHelper(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var playbackSpeed: Float = 1.0f

    fun setSpeed(speed: Float) {
        playbackSpeed = speed.coerceIn(0.5f, 2.0f)
    }

    private fun applyPlaybackParams() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                // PlaybackParams cannot be set if MediaPlayer is not prepared, 
                // but create() prepares it synchronously.
                val params = mediaPlayer?.playbackParams ?: android.media.PlaybackParams()
                params.speed = playbackSpeed
                mediaPlayer?.playbackParams = params
            } catch (e: Exception) {
                Log.e("AudioHelper", "Error setting playback speed", e)
            }
        }
    }

    fun playZone(zone: String) {
        try {
            releaseMediaPlayer()
            val resourceId = getZoneResourceId(zone)
            if (resourceId != 0) {
                mediaPlayer = MediaPlayer.create(context, resourceId)
                applyPlaybackParams()
                mediaPlayer?.setOnCompletionListener {
                    releaseMediaPlayer()
                }
                mediaPlayer?.start()
                Log.d("AudioHelper", "Playing zone: $zone, resource: $resourceId")
            } else {
                Log.w("AudioHelper", "No audio resource found for zone: $zone")
            }
        } catch (e: Exception) {
            Log.e("AudioHelper", "Error playing zone audio: ${e.message}", e)
            releaseMediaPlayer()
        }
    }

    fun playSuccess() {
        try {
            releaseMediaPlayer()
            mediaPlayer = MediaPlayer.create(context, R.raw.success_scanned)
            applyPlaybackParams()
            mediaPlayer?.setOnCompletionListener {
                releaseMediaPlayer()
            }
            mediaPlayer?.start()
            Log.d("AudioHelper", "Playing success sound")
        } catch (e: Exception) {
            Log.e("AudioHelper", "Error playing success sound: ${e.message}", e)
            releaseMediaPlayer()
        }
    }

    fun playError() {
        try {
            releaseMediaPlayer()
            mediaPlayer = MediaPlayer.create(context, R.raw.error_not_found)
            applyPlaybackParams()
            mediaPlayer?.setOnCompletionListener {
                releaseMediaPlayer()
            }
            mediaPlayer?.start()
            Log.d("AudioHelper", "Playing error sound")
        } catch (e: Exception) {
            Log.e("AudioHelper", "Error playing error sound: ${e.message}", e)
            releaseMediaPlayer()
        }
    }

    fun playDuplicate() {
        try {
            releaseMediaPlayer()
            mediaPlayer = MediaPlayer.create(context, R.raw.error_duplicate_scan)
            applyPlaybackParams()
            mediaPlayer?.setOnCompletionListener {
                releaseMediaPlayer()
            }
            mediaPlayer?.start()
            Log.d("AudioHelper", "Playing duplicate sound")
        } catch (e: Exception) {
            Log.e("AudioHelper", "Error playing duplicate sound: ${e.message}", e)
            releaseMediaPlayer()
        }
    }

    fun playWelcome() {
        try {
            releaseMediaPlayer()
            mediaPlayer = MediaPlayer.create(context, R.raw.other_welcome)
            applyPlaybackParams()
            mediaPlayer?.setOnCompletionListener {
                releaseMediaPlayer()
            }
            mediaPlayer?.start()
            Log.d("AudioHelper", "Playing welcome sound")
        } catch (e: Exception) {
            Log.e("AudioHelper", "Error playing welcome sound: ${e.message}", e)
            releaseMediaPlayer()
        }
    }

    fun playWaiting() {
        try {
            releaseMediaPlayer()
            mediaPlayer = MediaPlayer.create(context, R.raw.other_waiting)
            applyPlaybackParams()
            mediaPlayer?.setOnCompletionListener {
                releaseMediaPlayer()
            }
            mediaPlayer?.start()
            Log.d("AudioHelper", "Playing waiting sound")
        } catch (e: Exception) {
            Log.e("AudioHelper", "Error playing waiting sound: ${e.message}", e)
            releaseMediaPlayer()
        }
    }

    fun playCompleted() {
        try {
            releaseMediaPlayer()
            mediaPlayer = MediaPlayer.create(context, R.raw.success_completed)
            applyPlaybackParams()
            mediaPlayer?.setOnCompletionListener {
                releaseMediaPlayer()
            }
            mediaPlayer?.start()
            Log.d("AudioHelper", "Playing completed sound")
        } catch (e: Exception) {
            Log.e("AudioHelper", "Error playing completed sound: ${e.message}", e)
            releaseMediaPlayer()
        }
    }

    private fun getZoneResourceId(zone: String): Int {
        val parts = zone.split("-")
        if (parts.size == 2) {
            val firstPart = parts[0].trim()
            val secondPart = parts[1].trim()
            val resourceName = "zone_${firstPart}_${secondPart}"
            return context.resources.getIdentifier(resourceName, "raw", context.packageName)
        }
        return 0
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("AudioHelper", "Error releasing MediaPlayer: ${e.message}", e)
        }
    }

    fun release() {
        releaseMediaPlayer()
    }
}
