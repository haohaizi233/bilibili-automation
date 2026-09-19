package com.example.bilibiliautomation.data.secure

/**
 * Small crypto facade so JVM tests can use an in-memory AES key
 * while production uses Android Keystore via EncryptedSharedPreferences.
 */
interface CryptoBox {
    fun encrypt(plain: ByteArray): ByteArray
    fun decrypt(cipher: ByteArray): ByteArray
}
