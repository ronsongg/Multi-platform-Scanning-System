package com.scanner.app.data.remote.models

import com.google.gson.annotations.SerializedName

data class ScanRequest(
    @SerializedName("dataset_id") val datasetId: Int,
    @SerializedName("box_number") val boxNumber: String,
    @SerializedName("device_id") val deviceId: String = "android_scanner"
)

data class ScanResponse(
    val status: String,
    @SerializedName("box_number") val boxNumber: String?,
    val zone: String?,
    @SerializedName("store_address") val storeAddress: String?,
    @SerializedName("first_scan") val firstScan: Boolean?,
    val progress: ProgressInfo?,
    val message: String?
) {
    val isFound: Boolean get() = status == "found"
    val isNotFound: Boolean get() = status == "not_found"
    val isDuplicate: Boolean get() = isFound && firstScan == false
}

data class ProgressInfo(
    val scanned: Int,
    val total: Int,
    val percentage: Int
)

data class DatasetInfo(
    val id: Int,
    val name: String,
    @SerializedName("file_name") val fileName: String? = null,
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("scanned_count") val scannedCount: Int,
    @SerializedName("is_current") val isCurrent: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class ProgressResponse(
    val scanned: Int,
    val total: Int,
    val percentage: Int
)

data class RecentRecord(
    @SerializedName("box_number") val boxNumber: String,
    val zone: String?,
    @SerializedName("store_address") val storeAddress: String?,
    @SerializedName("scanned_at") val scannedAt: String?,
    @SerializedName("first_scan") val firstScan: Boolean?
)

data class RecentRecordsResponse(
    val records: List<RecentRecord>
)
