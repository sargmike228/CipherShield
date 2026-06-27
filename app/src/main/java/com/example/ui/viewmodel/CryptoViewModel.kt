package com.example.ui.viewmodel

import android.app.Application
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.RetroSoundGenerator
import com.example.crypto.MultiLayerCrypto
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

sealed class CryptoStepStatus {
    object NotStarted : CryptoStepStatus()
    object Processing : CryptoStepStatus()
    data class Success(val durationMs: Long) : CryptoStepStatus()
    data class Failed(val error: String) : CryptoStepStatus()
}

class CryptoViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = CryptoRepository(database)
    private val context = application.applicationContext

    // Saved profiles and history from Room database
    val keyProfiles: StateFlow<List<SavedKeyProfile>> = repository.keyProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cryptoHistory: StateFlow<List<CryptoHistory>> = repository.cryptoHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val secureFiles: StateFlow<List<SecureFileItem>> = repository.secureFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Terminal/Console Logs
    private val _consoleLogs = MutableStateFlow<List<String>>(listOf("Система инициализирована. Ожидание ввода..."))
    val consoleLogs: StateFlow<List<String>> = _consoleLogs.asStateFlow()

    // 7 Pipeline step states for live UI updates
    private val _pipelineStates = MutableStateFlow<List<CryptoStepStatus>>(List(7) { CryptoStepStatus.NotStarted })
    val pipelineStates: StateFlow<List<CryptoStepStatus>> = _pipelineStates.asStateFlow()

    // UI Input fields
    var inputText = MutableStateFlow("")
    var outputText = MutableStateFlow("")

    // Key settings
    val isCustomKeys = MutableStateFlow(false)
    val masterPassword = MutableStateFlow("")
    
    // 7 Individual Passwords
    val customKeys = MutableStateFlow(List(7) { "" })

    // Operation loading state
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    init {
        // Load initial sandbox files from filesDir to database to sync
        syncWorkspaceFiles()
    }

    private fun logToConsole(message: String) {
        _consoleLogs.update { it + message }
    }

    fun clearConsole() {
        _consoleLogs.value = listOf("Консоль очищена.")
    }

    private fun resetPipeline() {
        _pipelineStates.value = List(7) { CryptoStepStatus.NotStarted }
    }

    /**
     * Resolves the 7 passwords based on the current mode (Master Password vs Expert custom keys)
     */
    private fun resolve7Passwords(): List<String> {
        return if (!isCustomKeys.value) {
            val master = masterPassword.value
            if (master.isEmpty()) {
                throw IllegalArgumentException("Ошибка: Мастер-пароль не может быть пустым.")
            }
            listOf(
                master + "::layer_aes",
                master + "::layer_blowfish",
                master + "::layer_3des",
                master + "::layer_lfsr",
                master + "::layer_lcg",
                master + "::layer_hmac512",
                master + "::layer_hmac256"
            )
        } else {
            val keys = customKeys.value
            for (i in 0..6) {
                if (keys[i].isEmpty()) {
                    throw IllegalArgumentException("Ошибка: Ключ для уровня ${i + 1} не может быть пустым.")
                }
            }
            keys
        }
    }

    /**
     * Text Encryption
     */
    fun encryptText() {
        if (inputText.value.trim().isEmpty()) {
            logToConsole("⚠️ Ошибка: Отсутствует текст для шифрования!")
            return
        }
        viewModelScope.launch {
            _isProcessing.value = true
            resetPipeline()
            logToConsole("⚡ Запуск шифрования текста...")
            RetroSoundGenerator.playDiceRoll() // start sound

            try {
                val passwords = resolve7Passwords()
                val plainBytes = inputText.value.toByteArray(Charsets.UTF_8)

                val resultBytes = withContext(Dispatchers.Default) {
                    MultiLayerCrypto.encrypt(plainBytes, passwords, createCallback())
                }

                val encodedOutput = Base64.encodeToString(resultBytes, Base64.DEFAULT)
                outputText.value = encodedOutput
                logToConsole("✅ Шифрование успешно завершено. Результат закодирован в Base64.")
                RetroSoundGenerator.playHeal() // success sound

                // Save to History
                repository.insertHistory(
                    CryptoHistory(
                        sourceName = "Текстовое сообщение (${inputText.value.take(20)}...)",
                        operation = "Шифрование",
                        sizeBytes = plainBytes.size.toLong(),
                        success = true,
                        details = "Успешно пройдено 7 уровней защиты. Base64 размер: ${encodedOutput.length} символов."
                    )
                )
            } catch (e: Exception) {
                logToConsole("❌ КРИТИЧЕСКАЯ ОШИБКА: ${e.message}")
                RetroSoundGenerator.playDamage() // failure sound
                repository.insertHistory(
                    CryptoHistory(
                        sourceName = "Текстовое сообщение",
                        operation = "Шифрование",
                        sizeBytes = inputText.value.length.toLong(),
                        success = false,
                        details = "Ошибка: ${e.message}"
                    )
                )
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Text Decryption
     */
    fun decryptText() {
        if (inputText.value.trim().isEmpty()) {
            logToConsole("⚠️ Ошибка: Отсутствует текст для расшифрования!")
            return
        }
        viewModelScope.launch {
            _isProcessing.value = true
            resetPipeline()
            logToConsole("⚡ Запуск расшифрования текста...")
            RetroSoundGenerator.playDiceRoll() // start sound

            try {
                val passwords = resolve7Passwords()
                val cipherBytes = Base64.decode(inputText.value, Base64.DEFAULT)

                val resultBytes = withContext(Dispatchers.Default) {
                    MultiLayerCrypto.decrypt(cipherBytes, passwords, createCallback())
                }

                val decodedText = String(resultBytes, Charsets.UTF_8)
                outputText.value = decodedText
                logToConsole("✅ Расшифрование успешно завершено. Исходный текст восстановлен.")
                RetroSoundGenerator.playHeal() // success sound

                repository.insertHistory(
                    CryptoHistory(
                        sourceName = "Зашифрованный текст",
                        operation = "Расшифрование",
                        sizeBytes = cipherBytes.size.toLong(),
                        success = true,
                        details = "Успешно проверены подписи и сняты слои шифрования."
                    )
                )
            } catch (e: Exception) {
                logToConsole("❌ КРИТИЧЕСКАЯ ОШИБКА: ${e.message}")
                RetroSoundGenerator.playDamage() // failure sound
                repository.insertHistory(
                    CryptoHistory(
                        sourceName = "Зашифрованный текст",
                        operation = "Расшифрование",
                        sizeBytes = inputText.value.length.toLong(),
                        success = false,
                        details = "Ошибка: ${e.message}"
                    )
                )
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Workspace File Encryption
     */
    fun encryptFile(fileItem: SecureFileItem) {
        viewModelScope.launch {
            _isProcessing.value = true
            resetPipeline()
            logToConsole("⚡ Запуск шифрования файла: ${fileItem.originalName}...")
            RetroSoundGenerator.playDiceRoll()

            try {
                val passwords = resolve7Passwords()
                val targetFile = File(context.filesDir, fileItem.virtualPath)
                if (!targetFile.exists()) {
                    throw java.io.FileNotFoundException("Файл не найден во внутреннем хранилище.")
                }

                val fileBytes = withContext(Dispatchers.IO) { targetFile.readBytes() }
                val encryptedBytes = withContext(Dispatchers.Default) {
                    MultiLayerCrypto.encrypt(fileBytes, passwords, createCallback())
                }

                // Save encrypted file with .enc extension
                val encVirtualPath = "${UUID.randomUUID()}.enc"
                val encFile = File(context.filesDir, encVirtualPath)
                withContext(Dispatchers.IO) { encFile.writeBytes(encryptedBytes) }

                // Save to DB
                val encryptedFileItem = SecureFileItem(
                    originalName = "${fileItem.originalName}.enc",
                    virtualPath = encVirtualPath,
                    isEncrypted = true,
                    sizeBytes = encryptedBytes.size.toLong()
                )
                repository.insertFile(encryptedFileItem)

                logToConsole("✅ Файл зашифрован успешно! Создан защищенный контейнер: ${encryptedFileItem.originalName}")
                RetroSoundGenerator.playHeal()

                repository.insertHistory(
                    CryptoHistory(
                        sourceName = fileItem.originalName,
                        operation = "Шифрование файла",
                        sizeBytes = fileBytes.size.toLong(),
                        success = true,
                        details = "Успешная обработка. Создан .enc контейнер размером ${encryptedBytes.size} байт."
                    )
                )
            } catch (e: Exception) {
                logToConsole("❌ Ошибка шифрования файла: ${e.message}")
                RetroSoundGenerator.playDamage()
                repository.insertHistory(
                    CryptoHistory(
                        sourceName = fileItem.originalName,
                        operation = "Шифрование файла",
                        sizeBytes = fileItem.sizeBytes,
                        success = false,
                        details = "Ошибка: ${e.message}"
                    )
                )
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Workspace File Decryption
     */
    fun decryptFile(fileItem: SecureFileItem) {
        viewModelScope.launch {
            _isProcessing.value = true
            resetPipeline()
            logToConsole("⚡ Запуск дешифрования файла-контейнера: ${fileItem.originalName}...")
            RetroSoundGenerator.playDiceRoll()

            try {
                val passwords = resolve7Passwords()
                val targetFile = File(context.filesDir, fileItem.virtualPath)
                if (!targetFile.exists()) {
                    throw java.io.FileNotFoundException("Файл контейнера отсутствует в хранилище.")
                }

                val encBytes = withContext(Dispatchers.IO) { targetFile.readBytes() }
                val decryptedBytes = withContext(Dispatchers.Default) {
                    MultiLayerCrypto.decrypt(encBytes, passwords, createCallback())
                }

                // Restore file name by removing .enc
                val decName = fileItem.originalName.removeSuffix(".enc").let {
                    if (it == fileItem.originalName) "$it.dec" else it
                }
                val decVirtualPath = UUID.randomUUID().toString()
                val decFile = File(context.filesDir, decVirtualPath)
                withContext(Dispatchers.IO) { decFile.writeBytes(decryptedBytes) }

                val decryptedFileItem = SecureFileItem(
                    originalName = decName,
                    virtualPath = decVirtualPath,
                    isEncrypted = false,
                    sizeBytes = decryptedBytes.size.toLong()
                )
                repository.insertFile(decryptedFileItem)

                logToConsole("✅ Файл расшифрован успешно! Восстановлен оригинальный файл: $decName")
                RetroSoundGenerator.playHeal()

                repository.insertHistory(
                    CryptoHistory(
                        sourceName = fileItem.originalName,
                        operation = "Расшифрование файла",
                        sizeBytes = encBytes.size.toLong(),
                        success = true,
                        details = "Контейнер полностью проверен и расшифрован. Размер: ${decryptedBytes.size} байт."
                    )
                )
            } catch (e: Exception) {
                logToConsole("❌ Ошибка дешифрования файла: ${e.message}")
                RetroSoundGenerator.playDamage()
                repository.insertHistory(
                    CryptoHistory(
                        sourceName = fileItem.originalName,
                        operation = "Расшифрование файла",
                        sizeBytes = fileItem.sizeBytes,
                        success = false,
                        details = "Ошибка: ${e.message}"
                    )
                )
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Creates new empty text file in internal safe
     */
    fun createTextFileInWorkspace(name: String, textContent: String) {
        viewModelScope.launch {
            try {
                val uniquePath = UUID.randomUUID().toString()
                val file = File(context.filesDir, uniquePath)
                val bytes = textContent.toByteArray(Charsets.UTF_8)
                withContext(Dispatchers.IO) { file.writeBytes(bytes) }

                val fileItem = SecureFileItem(
                    originalName = if (name.endsWith(".txt")) name else "$name.txt",
                    virtualPath = uniquePath,
                    isEncrypted = false,
                    sizeBytes = bytes.size.toLong()
                )
                repository.insertFile(fileItem)
                logToConsole("📁 Создан новый текстовый файл: ${fileItem.originalName}")
                RetroSoundGenerator.playHeal()
            } catch (e: Exception) {
                logToConsole("❌ Ошибка при создании файла: ${e.message}")
            }
        }
    }

    /**
     * Imports bytes from outer file (e.g. system picker)
     */
    fun importBytesToWorkspace(name: String, bytes: ByteArray) {
        viewModelScope.launch {
            try {
                val uniquePath = UUID.randomUUID().toString()
                val file = File(context.filesDir, uniquePath)
                withContext(Dispatchers.IO) { file.writeBytes(bytes) }

                val fileItem = SecureFileItem(
                    originalName = name,
                    virtualPath = uniquePath,
                    isEncrypted = name.endsWith(".enc"),
                    sizeBytes = bytes.size.toLong()
                )
                repository.insertFile(fileItem)
                logToConsole("📥 Файл успешно импортирован в сейф: $name")
                RetroSoundGenerator.playHeal()
            } catch (e: Exception) {
                logToConsole("❌ Ошибка импорта: ${e.message}")
            }
        }
    }

    /**
     * Delete file from local safe
     */
    fun deleteWorkspaceFile(fileItem: SecureFileItem) {
        viewModelScope.launch {
            try {
                val file = File(context.filesDir, fileItem.virtualPath)
                if (file.exists()) {
                    withContext(Dispatchers.IO) { file.delete() }
                }
                repository.deleteFile(fileItem)
                logToConsole("🗑️ Файл удален из сейфа: ${fileItem.originalName}")
                RetroSoundGenerator.playDamage()
            } catch (e: Exception) {
                logToConsole("❌ Ошибка при удалении: ${e.message}")
            }
        }
    }

    /**
     * Read content of workspace file as text
     */
    suspend fun readWorkspaceFileText(fileItem: SecureFileItem): String {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, fileItem.virtualPath)
                if (file.exists()) {
                    String(file.readBytes(), Charsets.UTF_8)
                } else {
                    "Ошибка: Файл физически удален."
                }
            } catch (e: Exception) {
                "Ошибка чтения: ${e.message}"
            }
        }
    }

    /**
     * Load keys/passwords preset
     */
    fun loadKeyProfile(profile: SavedKeyProfile) {
        viewModelScope.launch {
            isCustomKeys.value = profile.isCustom
            if (!profile.isCustom) {
                masterPassword.value = profile.key1
            } else {
                customKeys.value = listOf(
                    profile.key1, profile.key2, profile.key3,
                    profile.key4, profile.key5, profile.key6, profile.key7
                )
            }
            logToConsole("🗝️ Загружен ключевой пресет: \"${profile.name}\"")
            RetroSoundGenerator.playHeal()
        }
    }

    /**
     * Save current keys configuration as preset
     */
    fun saveCurrentKeyProfile(name: String) {
        if (name.trim().isEmpty()) return
        viewModelScope.launch {
            try {
                val profile = if (!isCustomKeys.value) {
                    SavedKeyProfile(
                        name = name,
                        isCustom = false,
                        key1 = masterPassword.value
                    )
                } else {
                    val k = customKeys.value
                    SavedKeyProfile(
                        name = name,
                        isCustom = true,
                        key1 = k[0], key2 = k[1], key3 = k[2],
                        key4 = k[3], key5 = k[4], key6 = k[5], key7 = k[6]
                    )
                }
                repository.insertProfile(profile)
                logToConsole("💾 Ключевой пресет сохранен: \"$name\"")
                RetroSoundGenerator.playHeal()
            } catch (e: Exception) {
                logToConsole("❌ Ошибка сохранения пресета: ${e.message}")
            }
        }
    }

    /**
     * Delete preset
     */
    fun deleteKeyProfile(profile: SavedKeyProfile) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
            logToConsole("🗑️ Удален ключевой пресет: \"${profile.name}\"")
            RetroSoundGenerator.playDamage()
        }
    }

    /**
     * Delete all activity history entries
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            logToConsole("🧹 История операций очищена.")
            RetroSoundGenerator.playDamage()
        }
    }

    /**
     * Helper to sync real files on disk with room database items
     */
    private fun syncWorkspaceFiles() {
        viewModelScope.launch {
            val filesOnDisk = context.filesDir.listFiles() ?: emptyArray()
            val dbFiles = database.secureFileDao().getAllFilesFlow().firstOrNull() ?: emptyList()

            // Remove items from DB that do not exist on disk
            for (dbItem in dbFiles) {
                val file = File(context.filesDir, dbItem.virtualPath)
                if (!file.exists()) {
                    repository.deleteFile(dbItem)
                }
            }

            // If there are no files at all, create an initial secure instructions file as sample text!
            val updatedDbFiles = database.secureFileDao().getAllFilesFlow().firstOrNull() ?: emptyList()
            if (updatedDbFiles.isEmpty()) {
                createTextFileInWorkspace(
                    "crypto_instructions.txt",
                    "ДОБРО ПОЖАЛОВАТЬ В CIPHERSHIELD V1.0!\n\n" +
                    "Данное приложение обеспечивает ультимативный уровень секретности благодаря 7 последовательным слоям обработки:\n" +
                    "1. AES-GCM-256 (Шифрование с аутентификацией, PBKDF2WithHmacSHA256)\n" +
                    "2. Blowfish-CBC (128-битное шифрование, PBKDF2WithHmacSHA1)\n" +
                    "3. Triple DES / DESede-CBC (192-битное шифрование, PBKDF2WithHmacSHA512)\n" +
                    "4. Спиральный XOR-шум LFSR (Фибоначчи-генератор, 32-битный сдвиговый регистр)\n" +
                    "5. Конгруэнтный XOR-шум LCG (MMIX Knuth, псевдослучайный поток)\n" +
                    "6. Цифровая подпись HMAC-SHA512 (Подтверждение целостности ядра)\n" +
                    "7. Цифровая подпись HMAC-SHA256 (Проверка внешнего контейнера)\n\n" +
                    "ИНСТРУКЦИЯ:\n" +
                    "- Напишите секретный текст во вкладке 'Текст' или выберите файл во вкладке 'Сейф'.\n" +
                    "- Введите пароли вручную или используйте 'Мастер-пароль'.\n" +
                    "- Нажмите 'Зашифровать' и наблюдайте интерактивную визуализацию работы крипто-конвейера!"
                )
            }
        }
    }

    /**
     * Create callback to interactively feed crypto progress back to Compose UI
     */
    private fun createCallback() = object : MultiLayerCrypto.ProgressCallback {
        override fun onStepStarted(stepIndex: Int, stepName: String) {
            _pipelineStates.update { current ->
                current.mapIndexed { idx, status ->
                    if (idx == stepIndex - 1) CryptoStepStatus.Processing else status
                }
            }
            logToConsole("▶️ Уровень $stepIndex: $stepName...")
        }

        override fun onStepFinished(stepIndex: Int, stepName: String, durationMs: Long) {
            _pipelineStates.update { current ->
                current.mapIndexed { idx, status ->
                    if (idx == stepIndex - 1) CryptoStepStatus.Success(durationMs) else status
                }
            }
            logToConsole("⏹️ Уровень $stepIndex завершен [${durationMs}мс].")
        }

        override fun onLog(message: String) {
            logToConsole(message)
        }
    }
}
