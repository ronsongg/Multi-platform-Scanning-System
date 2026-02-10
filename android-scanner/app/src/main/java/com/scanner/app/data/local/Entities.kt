package com.scanner.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_scan_records")
data class CachedScanRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "box_number")
    val boxNumber: String,

    @ColumnInfo(name = "zone")
    val zone: String? = null,

    @ColumnInfo(name = "store_address")
    val storeAddress: String? = null,

    @ColumnInfo(name = "first_scan")
    val firstScan: Boolean = true,

    @ColumnInfo(name = "found")
    val found: Boolean = true,

    @ColumnInfo(name = "dataset_id")
    val datasetId: Int,

    @ColumnInfo(name = "scanned_at")
    val scannedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "uploaded")
    val uploaded: Boolean = false
)

@Entity(tableName = "server_config")
data class ServerConfig(
    @PrimaryKey
    val id: Int = 1,

    @ColumnInfo(name = "server_url")
    val serverUrl: String,

    @ColumnInfo(name = "active_dataset_id")
    val activeDatasetId: Int? = null,

    @ColumnInfo(name = "active_dataset_name")
    val activeDatasetName: String? = null,

    @ColumnInfo(name = "last_connected")
    val lastConnected: Long = System.currentTimeMillis()
)
