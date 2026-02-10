package com.scanner.app.data.repository

import com.scanner.app.data.local.AppDatabase
import com.scanner.app.data.local.ServerConfig
import com.scanner.app.data.remote.RetrofitClient
import com.scanner.app.data.remote.models.DatasetInfo
import com.scanner.app.data.remote.models.ProgressInfo
import kotlinx.coroutines.flow.Flow

class ConnectionRepository(private val db: AppDatabase) {

    suspend fun testConnection(serverUrl: String): Result<DatasetInfo?> {
        return try {
            val api = RetrofitClient.getApi(serverUrl)
            
            // 1. Try Ping first (Base connectivity check)
            try {
                val pingResponse = api.ping()
                if (!pingResponse.isSuccessful) {
                    return Result.failure(Exception("Ping failed: ${pingResponse.code()}"))
                }
            } catch (e: Exception) {
                 return Result.failure(Exception("Connection failed: ${e.message}"))
            }

            // 2. Try to get current dataset (might be 404 if not set, which is OK)
            val response = api.getCurrentDataset()
            var dataset: DatasetInfo? = null
            
            if (response.isSuccessful) {
                dataset = response.body()
            } else if (response.code() == 404) {
                // 404 is acceptable here, means connected but no dataset selected
                dataset = null
            } else {
                // Other errors are real errors
                // But since Ping passed, we might still want to return success with null dataset?
                // For now, let's be strict about other errors, but lenient on 404.
            }

            // Save valid config even if dataset is null
            val config = ServerConfig(
                serverUrl = serverUrl,
                activeDatasetId = dataset?.id,
                activeDatasetName = dataset?.name
            )
            db.serverConfigDao().save(config)
            
            Result.success(dataset)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDatasets(serverUrl: String): Result<List<DatasetInfo>> {
        return try {
            val api = RetrofitClient.getApi(serverUrl)
            val response = api.getDatasets()
            if (response.isSuccessful) {
                // Backend returns list directly
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProgress(serverUrl: String, datasetId: Int): Result<ProgressInfo> {
        return try {
            val api = RetrofitClient.getApi(serverUrl)
            val response = api.getProgress(datasetId)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Result.success(
                    ProgressInfo(
                        scanned = body.scanned,
                        total = body.total,
                        percentage = body.percentage
                    )
                )
            } else {
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSavedConfig(): ServerConfig? {
        return db.serverConfigDao().getConfig()
    }

    fun getSavedConfigFlow(): Flow<ServerConfig?> {
        return db.serverConfigDao().getConfigFlow()
    }

    suspend fun activateDataset(serverUrl: String, datasetId: Int, datasetName: String?) {
        val config = ServerConfig(
            serverUrl = serverUrl,
            activeDatasetId = datasetId,
            activeDatasetName = datasetName
        )
        db.serverConfigDao().save(config)
    }

    suspend fun saveConfig(config: ServerConfig) {
        db.serverConfigDao().save(config)
    }
}
