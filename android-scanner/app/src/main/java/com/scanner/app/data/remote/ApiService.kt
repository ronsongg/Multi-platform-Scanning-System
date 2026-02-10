package com.scanner.app.data.remote

import com.scanner.app.data.remote.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Backend returns DatasetInfo directly: { "id":1, "name":"...", "total_count":100, ... }
    @GET("api/current-dataset")
    suspend fun getCurrentDataset(): Response<DatasetInfo>

    @GET("api/ping")
    suspend fun ping(): Response<Map<String, String>>

    @GET("api/datasets")
    suspend fun getDatasets(): Response<List<DatasetInfo>>

    @POST("api/scan")
    suspend fun submitScan(@Body request: ScanRequest): Response<ScanResponse>

    @GET("api/scan/recent")
    suspend fun getRecentRecords(@Query("limit") limit: Int = 10): Response<RecentRecordsResponse>

    @GET("api/scan/progress/{datasetId}")
    suspend fun getProgress(@Path("datasetId") datasetId: Int): Response<ProgressResponse>
}
