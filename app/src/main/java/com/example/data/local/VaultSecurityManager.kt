package com.example.data.local

import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object VaultSecurityManager {

  // Local device AES-128 key for securing on-device study notes & assignment vault
  private val AES_KEY = "StarkJarvisVault".toByteArray(StandardCharsets.UTF_8)
  private val INIT_VECTOR = "ArcReactorCore00".toByteArray(StandardCharsets.UTF_8)
  private const val CIPHER_ALGO = "AES/CBC/PKCS5Padding"

  fun encrypt(plainText: String): String {
    if (plainText.isEmpty()) return ""
    return try {
      val keySpec = SecretKeySpec(AES_KEY, "AES")
      val ivSpec = IvParameterSpec(INIT_VECTOR)
      val cipher = Cipher.getInstance(CIPHER_ALGO)
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
      val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
      "ENC:" + Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
    } catch (e: Exception) {
      // Fallback safe encoding
      "B64:" + Base64.encodeToString(plainText.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
    }
  }

  fun decrypt(cipherText: String): String {
    if (cipherText.isEmpty()) return ""
    return try {
      if (cipherText.startsWith("ENC:")) {
        val rawEnc = cipherText.removePrefix("ENC:")
        val encryptedBytes = Base64.decode(rawEnc, Base64.NO_WRAP)
        val keySpec = SecretKeySpec(AES_KEY, "AES")
        val ivSpec = IvParameterSpec(INIT_VECTOR)
        val cipher = Cipher.getInstance(CIPHER_ALGO)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        String(decryptedBytes, StandardCharsets.UTF_8)
      } else if (cipherText.startsWith("B64:")) {
        val raw = cipherText.removePrefix("B64:")
        String(Base64.decode(raw, Base64.NO_WRAP), StandardCharsets.UTF_8)
      } else {
        cipherText
      }
    } catch (e: Exception) {
      cipherText
    }
  }
}
