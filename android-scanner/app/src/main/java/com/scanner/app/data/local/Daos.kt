package com.scanner.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CachedScanRecord): Long

    @Query("SELECT * FROM cached_scan_records WHERE dataset_id = :datasetId ORDER BY scanned_at DESC LIMIT :limit")
    fun getRecentRecords(datasetId: Int, limit: Int = 20): Flow<List<CachedScanRecord>>

    @Query("SELECT * FROM cached_scan_records WHERE dataset_id = :datasetId ORDER BY scanned_at DESC LIMIT :limit")
    suspend fun getRecentRecordsList(datasetId: Int, limit: Int = 20): List<CachedScanRecord>

    @Query("SELECT * FROM cached_scan_records WHERE uploaded = 0 ORDER BY scanned_at ASC")
    suspend fun getPendingUploads(): List<CachedScanRecord>

    @Query("SELECT COUNT(*) FROM cached_scan_records WHERE uploaded = 0")
    fun getPendingUploadCount(): Flow<Int>

    @Query("UPDATE cached_scan_records SET uploaded = 1 WHERE id = :id")
    suspend fun markUploaded(id: Long)

    @Query("DELETE FROM cached_scan_records WHERE dataset_id = :datasetId")
    suspend fun deleteByDataset(datasetId: Int)

    @Query("SELECT * FROM cached_scan_records WHERE dataset_id = :datasetId AND found = 0 ORDER BY scanned_at DESC")
    fun getErrorRecords(datasetId: Int): Flow<List<CachedScanRecord>>

    @Query("SELECT * FROM cached_scan_records WHERE dataset_id = :datasetId AND found = 1 ORDER BY scanned_at DESC")
    fun getSuccessRecords(datasetId: Int): Flow<List<CachedScanRecord>>
}

@Dao
interface ServerConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(config: ServerConfig)

    @Query("SELECT * FROM server_config WHERE id = 1")
    suspend fun getConfig(): ServerConfig?

    @Query("SELECT * FROM server_config WHERE id = 1")
    fun getConfigFlow(): Flow<ServerConfig?>

    @Query("UPDATE server_config SET active_dataset_id = :datasetId, active_dataset_name = :datasetName WHERE id = 1")
    suspend fun updateActiveDataset(datasetId: Int, datasetName: String)
}
