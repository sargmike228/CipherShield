package com.example.data

import kotlinx.coroutines.flow.Flow

class CryptoRepository(private val database: AppDatabase) {
    val keyProfiles: Flow<List<SavedKeyProfile>> = database.keyProfileDao().getAllProfilesFlow()
    val cryptoHistory: Flow<List<CryptoHistory>> = database.cryptoHistoryDao().getAllHistoryFlow()
    val secureFiles: Flow<List<SecureFileItem>> = database.secureFileDao().getAllFilesFlow()

    suspend fun insertProfile(profile: SavedKeyProfile) {
        database.keyProfileDao().insertProfile(profile)
    }

    suspend fun deleteProfile(profile: SavedKeyProfile) {
        database.keyProfileDao().deleteProfile(profile)
    }

    suspend fun insertHistory(history: CryptoHistory) {
        database.cryptoHistoryDao().insertHistory(history)
    }

    suspend fun clearHistory() {
        database.cryptoHistoryDao().clearHistory()
    }

    suspend fun insertFile(file: SecureFileItem) {
        database.secureFileDao().insertFile(file)
    }

    suspend fun deleteFile(file: SecureFileItem) {
        database.secureFileDao().deleteFile(file)
    }

    suspend fun getFileByVirtualPath(path: String): SecureFileItem? {
        return database.secureFileDao().getFileByVirtualPath(path)
    }
}
