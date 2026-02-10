package com.scanner.app.data.repository

import com.scanner.app.data.local.AppDatabase
import com.scanner.app.data.local.CachedScanRecord
import com.scanner.app.data.remote.ApiService
import com.scanner.app.data.remote.RetrofitClient
import com.scanner.app.data.remote.models.ScanRequest
import com.scanner.app.data.remote.models.ScanResponse
import kotlinx.coroutines.flow.Flow

class ScanRepository(private val db: AppDatabase) {

    private fun getApi(serverUrl: String): ApiService = RetrofitClient.getApi(serverUrl)

    suspend fun submitScan(
        serverUrl: String,
        datasetId: Int,
        boxNumber: String
    ): Result<ScanResponse> {
        return try {
            val api = getApi(serverUrl)
            val request = ScanRequest(
                datasetId = datasetId,
                boxNumber = boxNumber
            )
            val response = api.submitScan(request)
            if (response.isSuccessful && response.body() != null) {
                val scanResponse = response.body()!!
                // Cache locally
                val record = CachedScanRecord(
                    boxNumber = boxNumber,
                    zone = scanResponse.zone,
                    storeAddress = scanResponse.storeAddress,
                    firstScan = scanResponse.firstScan ?: true,
                    found = scanResponse.isFound,
                    datasetId = datasetId,
                    uploaded = true
                )
                db.scanRecordDao().insert(record)
                Result.success(scanResponse)
            } else {
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            // Save as pending upload for offline support
            val record = CachedScanRecord(
                boxNumber = boxNumber,
                datasetId = datasetId,
                uploaded = false,
                found = true // optimistic
            )
            db.scanRecordDao().insert(record)
            Result.failure(e)
        }
    }

    fun getRecentRecords(datasetId: Int, limit: Int = 20): Flow<List<CachedScanRecord>> {
        return db.scanRecordDao().getRecentRecords(datasetId, limit)
    }

    fun getSuccessRecords(datasetId: Int): Flow<List<CachedScanRecord>> {
        return db.scanRecordDao().getSuccessRecords(datasetId)
    }

    fun getErrorRecords(datasetId: Int): Flow<List<CachedScanRecord>> {
        return db.scanRecordDao().getErrorRecords(datasetId)
    }

    fun getPendingUploadCount(): Flow<Int> {
        return db.scanRecordDao().getPendingUploadCount()
    }

    suspend fun uploadPendingRecords(serverUrl: String, datasetId: Int): Int {
        val pending = db.scanRecordDao().getPendingUploads()
        var uploadedCount = 0
        val api = getApi(serverUrl)
        for (record in pending) {
            try {
                val request = ScanRequest(
                    datasetId = record.datasetId,
                    boxNumber = record.boxNumber
                )
                val response = api.submitScan(request)
                if (response.isSuccessful) {
                    db.scanRecordDao().markUploaded(record.id)
                    uploadedCount++
                }
            } catch (_: Exception) {
                break // Stop on first failure
            }
        }
        return uploadedCount
    }
}
