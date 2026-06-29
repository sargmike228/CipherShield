package com.example.crypto

import android.util.Base64
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object PgpCrypto {

    // Generate RSA Key Pair
    fun generateRsaKeyPair(keySize: Int = 2048): KeyPair {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(keySize)
        return keyGen.generateKeyPair()
    }

    // Convert PublicKey to PEM String
    fun publicKeyToPem(publicKey: PublicKey): String {
        val encoded = Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
        return "-----BEGIN PUBLIC KEY-----\n" +
                encoded.chunked(64).joinToString("\n") +
                "\n-----END PUBLIC KEY-----"
    }

    // Convert PrivateKey to PEM String
    fun privateKeyToPem(privateKey: PrivateKey): String {
        val encoded = Base64.encodeToString(privateKey.encoded, Base64.NO_WRAP)
        return "-----BEGIN PRIVATE KEY-----\n" +
                encoded.chunked(64).joinToString("\n") +
                "\n-----END PRIVATE KEY-----"
    }

    // Parse PublicKey from PEM String
    fun pemToPublicKey(pem: String): PublicKey {
        val cleanPem = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\r", "")
            .replace("\n", "")
            .replace(" ", "")
            .trim()
        val decoded = Base64.decode(cleanPem, Base64.DEFAULT)
        val spec = X509EncodedKeySpec(decoded)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePublic(spec)
    }

    // Parse PrivateKey from PEM String
    fun pemToPrivateKey(pem: String): PrivateKey {
        val cleanPem = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\r", "")
            .replace("\n", "")
            .replace(" ", "")
            .trim()
        val decoded = Base64.decode(cleanPem, Base64.DEFAULT)
        val spec = PKCS8EncodedKeySpec(decoded)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePrivate(spec)
    }

    // RSA Asymmetric Encryption with Public Key (Text or Bytes)
    fun encryptWithPublicKey(data: ByteArray, publicKey: PublicKey): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        
        // RSA block size limit: encrypt in chunks if needed (highly PGP-like!)
        val maxBlockSize = (publicKey.encoded.size - 11).coerceAtLeast(117) // approx block size for key length
        val output = java.io.ByteArrayOutputStream()
        var offset = 0
        while (offset < data.size) {
            val size = (data.size - offset).coerceAtMost(maxBlockSize)
            val chunk = cipher.doFinal(data, offset, size)
            output.write(chunk)
            offset += size
        }
        return output.toByteArray()
    }

    // RSA Asymmetric Decryption with Private Key (Bytes)
    fun decryptWithPrivateKey(encryptedData: ByteArray, privateKey: PrivateKey): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        
        // Approximate RSA cipher chunk block size (256 bytes for 2048-bit key)
        val maxBlockSize = 256
        val output = java.io.ByteArrayOutputStream()
        var offset = 0
        while (offset < encryptedData.size) {
            val size = (encryptedData.size - offset).coerceAtMost(maxBlockSize)
            val chunk = cipher.doFinal(encryptedData, offset, size)
            output.write(chunk)
            offset += size
        }
        return output.toByteArray()
    }

    // RSA Digitally Sign Bytes with Private Key
    fun signWithPrivateKey(data: ByteArray, privateKey: PrivateKey): ByteArray {
        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(privateKey)
        signer.update(data)
        return signer.sign()
    }

    // RSA Verify Digital Signature with Public Key
    fun verifyWithPublicKey(data: ByteArray, signature: ByteArray, publicKey: PublicKey): Boolean {
        val verifier = Signature.getInstance("SHA256withRSA")
        verifier.initVerify(publicKey)
        verifier.update(data)
        return verifier.verify(signature)
    }

    // Pure Hashing Function (MD5, SHA-1, SHA-256, SHA-512)
    fun calculateHash(data: ByteArray, algorithm: String): ByteArray {
        val md = MessageDigest.getInstance(algorithm)
        return md.digest(data)
    }

    // Pure HMAC Function (HMAC-SHA256, HMAC-SHA512)
    fun calculateHmac(data: ByteArray, key: ByteArray, algorithm: String): ByteArray {
        val mac = Mac.getInstance(algorithm)
        val keySpec = SecretKeySpec(key, algorithm)
        mac.init(keySpec)
        return mac.doFinal(data)
    }

    // Helper to format byte array as HEX String
    fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02X", b))
        }
        return sb.toString()
    }
}
