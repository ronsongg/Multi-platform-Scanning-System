package com.scanner.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.scanner.app.R

class SoundHelper(context: Context) {

    private val soundPool: SoundPool
    private var warningBeepId: Int = 0
    private var loaded = false

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) loaded = true
        }

        warningBeepId = soundPool.load(context, R.raw.warning_beep, 1)
    }

    fun playWarning() {
        if (loaded) {
            soundPool.play(warningBeepId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }

    fun release() {
        soundPool.release()
    }
}
