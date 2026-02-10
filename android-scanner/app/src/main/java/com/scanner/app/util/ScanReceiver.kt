package com.scanner.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class ScanReceiver(private val onScanResult: (String) -> Unit) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val scanResult = intent?.let { extractScanData(it) }
        if (!scanResult.isNullOrBlank()) {
            onScanResult(scanResult.trim())
        }
    }

    private fun extractScanData(intent: Intent): String? {
        // Try common scan data extras from various hardware scanners
        val keys = listOf(
            "SCAN_RESULT",
            "scannerdata",
            "barcode_string",
            "decode_data",
            "com.symbol.datawedge.data_string",
            "com.motorolasolutions.emdk.datawedge.data_string"
        )
        for (key in keys) {
            val value = intent.getStringExtra(key)
            if (!value.isNullOrBlank()) return value
        }
        // Try byte array extras
        val byteKeys = listOf("barcode", "decode_byte")
        for (key in byteKeys) {
            val bytes = intent.getByteArrayExtra(key)
            if (bytes != null) return String(bytes)
        }
        return null
    }

    companion object {
        fun createIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction("com.android.server.scannerservice.broadcast")
                addAction("android.intent.ACTION_DECODE_DATA")
                addAction("com.symbol.datawedge.api.RESULT_ACTION")
                addAction("com.honeywell.decode.intent.action.EDIT_DATA")
                addAction("nlscan.action.SCANNER_RESULT")
                addAction("com.barcode.sendBroadcast")
            }
        }
    }
}
