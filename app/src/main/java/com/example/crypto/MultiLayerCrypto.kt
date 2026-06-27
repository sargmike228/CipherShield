package com.example.crypto

import android.util.Log
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object MultiLayerCrypto {
    private const val TAG = "MultiLayerCrypto"

    // Magic header bytes to verify file format: "MCRY"
    private val MAGIC_BYTES = byteArrayOf(0x4D, 0x43, 0x52, 0x59)

    interface ProgressCallback {
        fun onStepStarted(stepIndex: Int, stepName: String)
        fun onStepFinished(stepIndex: Int, stepName: String, durationMs: Long)
        fun onLog(message: String)
    }

    // LFSR (Linear Feedback Shift Register) custom keystream generator (XOR Layer 1)
    class LfsrGenerator(seedString: String) {
        private var state: Int = 0

        init {
            // Seed must be non-zero. Generate 32-bit state using SHA-256 to ensure robust key spread.
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(seedString.toByteArray(Charsets.UTF_8))
            var derivedState = ((hash[0].toInt() and 0xFF) shl 24) or
                               ((hash[1].toInt() and 0xFF) shl 16) or
                               ((hash[2].toInt() and 0xFF) shl 8) or
                               (hash[3].toInt() and 0xFF)
            if (derivedState == 0) derivedState = 0x12345678
            state = derivedState
        }

        fun nextByte(): Byte {
            var value = 0
            for (bit in 0 until 8) {
                // Taps for standard 32-bit Fibonacci LFSR: bits 32, 22, 2, 1
                val b31 = (state ushr 31) and 1
                val b21 = (state ushr 21) and 1
                val b1 = (state ushr 1) and 1
                val b0 = state and 1
                val feedback = b31 xor b21 xor b1 xor b0
                state = (state shl 1) or feedback
                value = (value shl 1) or (state and 1)
            }
            return value.toByte()
        }
    }

    // LCG (Linear Congruential Generator) custom keystream generator (XOR Layer 2)
    class LcgGenerator(seedString: String) {
        private var state: Int = 0

        init {
            // Generate 32-bit state using SHA-256
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(seedString.toByteArray(Charsets.UTF_8))
            state = ((hash[4].toInt() and 0xFF) shl 24) or
                    ((hash[5].toInt() and 0xFF) shl 16) or
                    ((hash[6].toInt() and 0xFF) shl 8) or
                    (hash[7].toInt() and 0xFF)
        }

        fun nextByte(): Byte {
            // Knuth MMIX LCG values: a = 1664525, c = 1013904223
            state = state * 1664525 + 1013904223
            // Return upper 8 bits for better statistical distribution
            return (state ushr 24).toByte()
        }
    }

    /**
     * Derives a cryptographic key from a password and salt using PBKDF2
     */
    private fun deriveKey(password: String, salt: ByteArray, keySizeBytes: Int, algorithm: String): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, 1000, keySizeBytes * 8)
        val factory = SecretKeyFactory.getInstance(algorithm)
        return factory.generateSecret(spec).encoded
    }

    /**
     * Complete Multi-layered Encryption Pipeline
     */
    fun encrypt(
        plaintext: ByteArray,
        passwords: List<String>, // Exactly 7 passwords corresponding to 7 layers
        callback: ProgressCallback? = null
    ): ByteArray {
        require(passwords.size == 7) { "Must provide exactly 7 passwords for the 7 security layers" }

        val secRandom = SecureRandom()

        // Generate salts for the 3 encryption algorithms
        val saltA = ByteArray(16).apply { secRandom.nextBytes(this) }
        val saltB = ByteArray(16).apply { secRandom.nextBytes(this) }
        val saltC = ByteArray(16).apply { secRandom.nextBytes(this) }

        // Generate random initialization vectors (IVs)
        val ivA = ByteArray(12).apply { secRandom.nextBytes(this) } // AES-GCM (12 bytes is standard)
        val ivB = ByteArray(8).apply { secRandom.nextBytes(this) }  // Blowfish-CBC (8 bytes block size)
        val ivC = ByteArray(8).apply { secRandom.nextBytes(this) }  // TripleDES-CBC (8 bytes block size)

        callback?.onLog("--- Запуск шифрования: 7 уровней защиты ---")

        // LAYER 1: AES-GCM-256 Encryption
        var startTime = System.currentTimeMillis()
        callback?.onStepStarted(1, "Шифрование AES-GCM-256 (Уровень 1)")
        val aesKeyBytes = deriveKey(passwords[0], saltA, 32, "PBKDF2WithHmacSHA256")
        val aesKeySpec = SecretKeySpec(aesKeyBytes, "AES")
        val aesCipher = Cipher.getInstance("AES/GCM/NoPadding")
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKeySpec, GCMParameterSpec(128, ivA))
        var encryptedBytes = aesCipher.doFinal(plaintext)
        callback?.onStepFinished(1, "AES-GCM-256 завершено", System.currentTimeMillis() - startTime)
        callback?.onLog("AES-GCM-256: Размер payload = ${encryptedBytes.size} байт")

        // LAYER 2: Blowfish-CBC Encryption
        startTime = System.currentTimeMillis()
        callback?.onStepStarted(2, "Шифрование Blowfish-CBC (Уровень 2)")
        val blowfishKeyBytes = deriveKey(passwords[1], saltB, 16, "PBKDF2WithHmacSHA1")
        val blowfishKeySpec = SecretKeySpec(blowfishKeyBytes, "Blowfish")
        val blowfishCipher = Cipher.getInstance("Blowfish/CBC/PKCS5Padding")
        blowfishCipher.init(Cipher.ENCRYPT_MODE, blowfishKeySpec, IvParameterSpec(ivB))
        encryptedBytes = blowfishCipher.doFinal(encryptedBytes)
        callback?.onStepFinished(2, "Blowfish-CBC завершено", System.currentTimeMillis() - startTime)
        callback?.onLog("Blowfish: Размер payload = ${encryptedBytes.size} байт")

        // LAYER 3: DESede-CBC (Triple DES) Encryption
        startTime = System.currentTimeMillis()
        callback?.onStepStarted(3, "Шифрование Triple DES (Уровень 3)")
        val desKeyBytes = deriveKey(passwords[2], saltC, 24, "PBKDF2WithHmacSHA512")
        val desKeySpec = SecretKeySpec(desKeyBytes, "DESede")
        val desCipher = Cipher.getInstance("DESede/CBC/PKCS5Padding")
        desCipher.init(Cipher.ENCRYPT_MODE, desKeySpec, IvParameterSpec(ivC))
        encryptedBytes = desCipher.doFinal(encryptedBytes)
        callback?.onStepFinished(3, "Triple DES завершено", System.currentTimeMillis() - startTime)
        callback?.onLog("Triple DES: Размер payload = ${encryptedBytes.size} байт")

        // LAYER 4: LFSR XOR Noise Layer 1
        startTime = System.currentTimeMillis()
        callback?.onStepStarted(4, "XOR-наложение шума LFSR (Уровень 4)")
        val lfsr = LfsrGenerator(passwords[3])
        val xor1Bytes = ByteArray(encryptedBytes.size)
        for (i in encryptedBytes.indices) {
            xor1Bytes[i] = (encryptedBytes[i].toInt() xor lfsr.nextByte().toInt()).toByte()
        }
        encryptedBytes = xor1Bytes
        callback?.onStepFinished(4, "LFSR шум завершен", System.currentTimeMillis() - startTime)

        // LAYER 5: LCG XOR Noise Layer 2
        startTime = System.currentTimeMillis()
        callback?.onStepStarted(5, "XOR-наложение шума LCG (Уровень 5)")
        val lcg = LcgGenerator(passwords[4])
        val xor2Bytes = ByteArray(encryptedBytes.size)
        for (i in encryptedBytes.indices) {
            xor2Bytes[i] = (encryptedBytes[i].toInt() xor lcg.nextByte().toInt()).toByte()
        }
        encryptedBytes = xor2Bytes
        callback?.onStepFinished(5, "LCG шум завершен", System.currentTimeMillis() - startTime)

        // LAYER 6: Signature 1 HMAC-SHA512
        startTime = System.currentTimeMillis()
        callback?.onStepStarted(6, "Подпись HMAC-SHA512 (Уровень 6)")
        val sha512Digest = MessageDigest.getInstance("SHA-512")
        val hmac512Key = sha512Digest.digest(passwords[5].toByteArray(Charsets.UTF_8))
        val hmac512Spec = SecretKeySpec(hmac512Key, "HmacSHA512")
        val mac512 = Mac.getInstance("HmacSHA512")
        mac512.init(hmac512Spec)
        val signature1 = mac512.doFinal(encryptedBytes) // 64 bytes
        callback?.onStepFinished(6, "Подпись HMAC-SHA512 создана", System.currentTimeMillis() - startTime)
        callback?.onLog("HMAC-SHA512 подпись: ${signature1.take(8).map { String.format("%02X", it) }.joinToString("")}...")

        // LAYER 7: Signature 2 HMAC-SHA256 over (EncryptedBytes + Signature 1)
        startTime = System.currentTimeMillis()
        callback?.onStepStarted(7, "Подпись HMAC-SHA256 (Уровень 7)")
        val sha256Digest = MessageDigest.getInstance("SHA-256")
        val hmac256Key = sha256Digest.digest(passwords[6].toByteArray(Charsets.UTF_8))
        val hmac256Spec = SecretKeySpec(hmac256Key, "HmacSHA256")
        val mac256 = Mac.getInstance("HmacSHA256")
        mac256.init(hmac256Spec)
        
        val hmac256Payload = ByteArray(encryptedBytes.size + signature1.size)
        System.arraycopy(encryptedBytes, 0, hmac256Payload, 0, encryptedBytes.size)
        System.arraycopy(signature1, 0, hmac256Payload, encryptedBytes.size, signature1.size)
        val signature2 = mac256.doFinal(hmac256Payload) // 32 bytes
        callback?.onStepFinished(7, "Подпись HMAC-SHA256 создана", System.currentTimeMillis() - startTime)
        callback?.onLog("HMAC-SHA256 подпись: ${signature2.take(8).map { String.format("%02X", it) }.joinToString("")}...")

        // Construct complete file container package
        // Format: MAGIC (4) + SaltA(16) + IvA(12) + SaltB(16) + IvB(8) + SaltC(16) + IvC(8) + Length(4) + Ciphertext + Sig1(64) + Sig2(32)
        val headerSize = 4 + 16 + 12 + 16 + 8 + 16 + 8 + 4
        val totalSize = headerSize + encryptedBytes.size + 64 + 32
        val finalBuffer = ByteBuffer.allocate(totalSize)

        finalBuffer.put(MAGIC_BYTES)
        finalBuffer.put(saltA)
        finalBuffer.put(ivA)
        finalBuffer.put(saltB)
        finalBuffer.put(ivB)
        finalBuffer.put(saltC)
        finalBuffer.put(ivC)
        finalBuffer.putInt(encryptedBytes.size)
        finalBuffer.put(encryptedBytes)
        finalBuffer.put(signature1)
        finalBuffer.put(signature2)

        callback?.onLog("--- Шифрование успешно завершено! Итоговый объем: $totalSize байт ---")
        return finalBuffer.array()
    }

    /**
     * Complete Multi-layered Decryption Pipeline
     */
    fun decrypt(
        encryptedPackage: ByteArray,
        passwords: List<String>,
        callback: ProgressCallback? = null
    ): ByteArray {
        require(passwords.size == 7) { "Must provide exactly 7 passwords for the 7 security layers" }
        callback?.onLog("--- Запуск расшифрования: Извлечение и валидация слоев ---")

        if (encryptedPackage.size < 4 + 16 + 12 + 16 + 8 + 16 + 8 + 4 + 64 + 32) {
            throw IllegalArgumentException("Ошибка формата: Размер файла слишком мал.")
        }

        val buffer = ByteBuffer.wrap(encryptedPackage)

        // Verify Magic
        val magic = ByteArray(4)
        buffer.get(magic)
        if (!magic.contentEquals(MAGIC_BYTES)) {
            throw IllegalArgumentException("Ошибка формата: Неверный сигнатурный идентификатор (Magic). Возможно, файл поврежден или не зашифрован данным приложением.")
        }

        // Read salts and IVs
        val saltA = ByteArray(16).also { buffer.get(it) }
        val ivA = ByteArray(12).also { buffer.get(it) }
        val saltB = ByteArray(16).also { buffer.get(it) }
        val ivB = ByteArray(8).also { buffer.get(it) }
        val saltC = ByteArray(16).also { buffer.get(it) }
        val ivC = ByteArray(8).also { buffer.get(it) }
        val ciphertextLen = buffer.getInt()

        if (ciphertextLen <= 0 || ciphertextLen > buffer.remaining()) {
            throw IllegalArgumentException("Ошибка формата: Недопустимый размер зашифрованных данных.")
        }

        // Read ciphertext and signatures
        val encryptedBytes = ByteArray(ciphertextLen).also { buffer.get(it) }
        val signature1 = ByteArray(64).also { buffer.get(it) }
        val signature2 = ByteArray(32).also { buffer.get(it) }

        // LAYER 7: Validate Signature 2 (HMAC-SHA256)
        var startTime = System.currentTimeMillis()
        callback?.onStepStarted(7, "Проверка подписи HMAC-SHA256 (Уровень 7)")
        val sha256Digest = MessageDigest.getInstance("SHA-256")
        val hmac256Key = sha256Digest.digest(passwords[6].toByteArray(Charsets.UTF_8))
        val hmac256Spec = SecretKeySpec(hmac256Key, "HmacSHA256")
        val mac256 = Mac.getInstance("HmacSHA256")
        mac256.init(hmac256Spec)
        
        val hmac256Payload = ByteArray(encryptedBytes.size + signature1.size)
        System.arraycopy(encryptedBytes, 0, hmac256Payload, 0, encryptedBytes.size)
        System.arraycopy(signature1, 0, hmac256Payload, encryptedBytes.size, signature1.size)
        val calculatedSig2 = mac256.doFinal(hmac256Payload)

        if (!MessageDigest.isEqual(signature2, calculatedSig2)) {
            callback?.onStepFinished(7, "HMAC-SHA256 Ошибка", 0)
            throw SecurityException("Ошибка целостности: Подпись уровня 7 (HMAC-SHA256) не совпадает! Убедитесь, что Ключ 7 введен правильно.")
        }
        callback?.onStepFinished(7, "HMAC-SHA256 подтверждена", System.currentTimeMillis() - startTime)

        // LAYER 6: Validate Signature 1 (HMAC-SHA512)
        startTime = System.currentTimeMillis()
        callback?.onStepStarted(6, "Проверка подписи HMAC-SHA512 (Уровень 6)")
        val sha512Digest = MessageDigest.getInstance("SHA-512")
        val hmac512Key = sha512Digest.digest(passwords[5].toByteArray(Charsets.UTF_8))
        val hmac512Spec = SecretKeySpec(hmac512Key, "HmacSHA512")
        val mac512 = Mac.getInstance("HmacSHA512")
        mac512.init(hmac512Spec)
        val calculatedSig1 = mac512.doFinal(encryptedBytes)

        if (!MessageDigest.isEqual(signature1, calculatedSig1)) {
            callback?.onStepFinished(6, "HMAC-SHA512 Ошибка", 0)
            throw SecurityException("Ошибка целостности: Подпись уровня 6 (HMAC-SHA512) не совпадает! Убедитесь, что Ключ 6 введен правильно.")
        }
        callback?.onStepFinished(6, "HMAC-SHA512 подтверждена", System.currentTimeMillis() - startTime)

        // LAYER 5: LCG XOR Noise Layer 2 (Decryption is symmetric XOR)
        startTime = System.currentTimeMillis()
        callback?.onStepStarted(5, "Снятие шума LCG (Уровень 5)")
        val lcg = LcgGenerator(passwords[4])
        val xor2Bytes = ByteArray(encryptedBytes.size)
        for (i in encryptedBytes.indices) {
            xor2Bytes[i] = (encryptedBytes[i].toInt() xor lcg.nextByte().toInt()).toByte()
        }
        var decryptedBytes = xor2Bytes
        callback?.onStepFinished(5, "LCG шум снят", System.currentTimeMillis() - startTime)

        // LAYER 4: LFSR XOR Noise Layer 1
        startTime = System.currentTimeMillis()
        callback?.onStepStarted(4, "Снятие шума LFSR (Уровень 4)")
        val lfsr = LfsrGenerator(passwords[3])
        val xor1Bytes = ByteArray(decryptedBytes.size)
        for (i in decryptedBytes.indices) {
            xor1Bytes[i] = (decryptedBytes[i].toInt() xor lfsr.nextByte().toInt()).toByte()
        }
        decryptedBytes = xor1Bytes
        callback?.onStepFinished(4, "LFSR шум снят", System.currentTimeMillis() - startTime)

        // LAYER 3: Triple DES Decryption
        startTime = System.currentTimeMillis()
        callback?.onStepStarted(3, "Дешифрование Triple DES (Уровень 3)")
        val desKeyBytes = deriveKey(passwords[2], saltC, 24, "PBKDF2WithHmacSHA512")
        val desKeySpec = SecretKeySpec(desKeyBytes, "DESede")
        val desCipher = Cipher.getInstance("DESede/CBC/PKCS5Padding")
        desCipher.init(Cipher.DECRYPT_MODE, desKeySpec, IvParameterSpec(ivC))
        decryptedBytes = desCipher.doFinal(decryptedBytes)
        callback?.onStepFinished(3, "Triple DES дешифровано", System.currentTimeMillis() - startTime)

        // LAYER 2: Blowfish Decryption
        startTime = System.currentTimeMillis()
        callback?.onStepStarted(2, "Дешифрование Blowfish-CBC (Уровень 2)")
        val blowfishKeyBytes = deriveKey(passwords[1], saltB, 16, "PBKDF2WithHmacSHA1")
        val blowfishKeySpec = SecretKeySpec(blowfishKeyBytes, "Blowfish")
        val blowfishCipher = Cipher.getInstance("Blowfish/CBC/PKCS5Padding")
        blowfishCipher.init(Cipher.DECRYPT_MODE, blowfishKeySpec, IvParameterSpec(ivB))
        decryptedBytes = blowfishCipher.doFinal(decryptedBytes)
        callback?.onStepFinished(2, "Blowfish дешифровано", System.currentTimeMillis() - startTime)

        // LAYER 1: AES-GCM Decryption
        startTime = System.currentTimeMillis()
        callback?.onStepStarted(1, "Дешифрование AES-GCM-256 (Уровень 1)")
        val aesKeyBytes = deriveKey(passwords[0], saltA, 32, "PBKDF2WithHmacSHA256")
        val aesKeySpec = SecretKeySpec(aesKeyBytes, "AES")
        val aesCipher = Cipher.getInstance("AES/GCM/NoPadding")
        aesCipher.init(Cipher.DECRYPT_MODE, aesKeySpec, GCMParameterSpec(128, ivA))
        decryptedBytes = aesCipher.doFinal(decryptedBytes)
        callback?.onStepFinished(1, "AES-GCM дешифровано", System.currentTimeMillis() - startTime)

        callback?.onLog("--- Расшифрование успешно завершено! Восстановлено ${decryptedBytes.size} байт ---")
        return decryptedBytes
    }
}
