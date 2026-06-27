package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyProfileDao {
    @Query("SELECT * FROM saved_key_profiles ORDER BY timestamp DESC")
    fun getAllProfilesFlow(): Flow<List<SavedKeyProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: SavedKeyProfile)

    @Delete
    suspend fun deleteProfile(profile: SavedKeyProfile)
}

@Dao
interface CryptoHistoryDao {
    @Query("SELECT * FROM crypto_history ORDER BY timestamp DESC")
    fun getAllHistoryFlow(): Flow<List<CryptoHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: CryptoHistory)

    @Query("DELETE FROM crypto_history")
    suspend fun clearHistory()
}

@Dao
interface SecureFileDao {
    @Query("SELECT * FROM secure_files ORDER BY timestamp DESC")
    fun getAllFilesFlow(): Flow<List<SecureFileItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: SecureFileItem)

    @Delete
    suspend fun deleteFile(file: SecureFileItem)

    @Query("SELECT * FROM secure_files WHERE virtualPath = :path LIMIT 1")
    suspend fun getFileByVirtualPath(path: String): SecureFileItem?
}

@Database(
    entities = [SavedKeyProfile::class, CryptoHistory::class, SecureFileItem::class],
    version = 2, // Bump version to trigger fallbackToDestructiveMigration
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun keyProfileDao(): KeyProfileDao
    abstract fun cryptoHistoryDao(): CryptoHistoryDao
    abstract fun secureFileDao(): SecureFileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "crypto_shield_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
