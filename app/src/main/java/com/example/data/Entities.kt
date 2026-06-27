package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_key_profiles")
data class SavedKeyProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isCustom: Boolean, // false if single master passphrase, true if 7 distinct keys
    val key1: String, // master passphrase or Key 1
    val key2: String = "",
    val key3: String = "",
    val key4: String = "",
    val key5: String = "",
    val key6: String = "",
    val key7: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "crypto_history")
data class CryptoHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sourceName: String, // e.g., "secret_text.txt", "Plaintext Text Block"
    val operation: String, // "ENCRYPT" or "DECRYPT"
    val sizeBytes: Long,
    val success: Boolean,
    val details: String, // logs or error message
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "secure_files")
data class SecureFileItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalName: String,
    val virtualPath: String, // unique file name under app sandbox filesDir
    val isEncrypted: Boolean,
    val sizeBytes: Long,
    val timestamp: Long = System.currentTimeMillis()
)
