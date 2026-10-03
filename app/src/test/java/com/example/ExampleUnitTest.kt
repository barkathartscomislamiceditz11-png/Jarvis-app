package com.example

import com.example.data.ai.JarvisOfflineEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testJarvisOfflineLinearMathSolving() {
    val result = JarvisOfflineEngine.processQuery("solve 2x + 8 = 20")
    assertEquals("math", result.matchedIntent)
    assertNotNull(result.mathResult)
    assertTrue(result.mathResult!!.finalAnswer.contains("x = 6"))
  }

  @Test
  fun testJarvisOfflinePercentageSolving() {
    val result = JarvisOfflineEngine.processQuery("what is 15% of 200")
    assertEquals("math", result.matchedIntent)
    assertNotNull(result.mathResult)
    assertEquals("30", result.mathResult!!.finalAnswer)
  }

  @Test
  fun testJarvisOfflineArithmetic() {
    val result = JarvisOfflineEngine.processQuery("solve 45 * 10")
    assertEquals("math", result.matchedIntent)
    assertNotNull(result.mathResult)
    assertEquals("450", result.mathResult!!.finalAnswer)
  }

  @Test
  fun testJarvisPrivacyIntent() {
    val result = JarvisOfflineEngine.processQuery("check privacy vault")
    assertEquals("privacy", result.matchedIntent)
    assertTrue(result.responseText.contains("Privacy protocol active"))
  }

  @Test
  fun testVaultEncryptionAndDecryption() {
    val sampleNote = "Quadratic equation: x = (-b +- sqrt(b^2 - 4ac)) / 2a"
    val encrypted = com.example.data.local.VaultSecurityManager.encrypt(sampleNote)
    assertTrue(encrypted.startsWith("ENC:") || encrypted.startsWith("B64:"))
    val decrypted = com.example.data.local.VaultSecurityManager.decrypt(encrypted)
    assertEquals(sampleNote, decrypted)
  }
}
