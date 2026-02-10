package com.scanner.app.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TTSHelper(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false

    private val digitMap = mapOf(
        '0' to "零", '1' to "一", '2' to "二", '3' to "三", '4' to "四",
        '5' to "五", '6' to "六", '7' to "七", '8' to "八", '9' to "九"
    )

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.CHINA)
                isReady = result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED
                if (!isReady) {
                    // Fallback to Simplified Chinese
                    tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                    isReady = true
                }
                Log.d("TTSHelper", "TTS initialized, ready=$isReady")
            } else {
                Log.e("TTSHelper", "TTS init failed: $status")
            }
        }
    }

    fun speakZone(zone: String) {
        if (!isReady) return
        val text = zoneToChineseText(zone)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "zone_$zone")
    }

    fun speak(text: String) {
        if (!isReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "custom")
    }

    private fun zoneToChineseText(zone: String): String {
        // Remove hyphens, convert each digit to Chinese
        val digits = zone.replace("-", "")
        val chineseDigits = digits.map { digitMap[it] ?: it.toString() }
        return "分区 ${chineseDigits.joinToString(" ")}"
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
