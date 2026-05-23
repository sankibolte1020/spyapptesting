package com.security.testapp

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val SECRET_KEY = "MySecretKey12345" // Must be 16 chars for AES-128
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"

    fun encrypt(plainText: String): Pair<String, String> {
        val cipher = Cipher.getInstance(ALGORITHM)
        val secretKey = SecretKeySpec(SECRET_KEY.toByteArray(), "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val encryptedBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        return Pair(encryptedBase64, ivBase64)
    }

    fun decrypt(encryptedBase64: String, ivBase64: String): String {
        val encrypted = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(ALGORITHM)
        val secretKey = SecretKeySpec(SECRET_KEY.toByteArray(), "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, Charsets.UTF_8)
    }
}
