package com.mdarman0964.vela

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts Exness credentials at rest with a non-exportable Android Keystore AES key. */
class ExnessCredentialVault(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("vela_secure_credentials", Context.MODE_PRIVATE)

    data class Credentials(
        val gateway: String,
        val accountId: String,
        val apiKey: String,
        val privateKey: String,
        val instrument: String = "XAUUSDm",
    )

    fun save(credentials: Credentials) {
        require(credentials.gateway.isNotBlank() && credentials.accountId.isNotBlank())
        require(credentials.apiKey.isNotBlank() && credentials.privateKey.isNotBlank())
        val payload = listOf(credentials.gateway, credentials.accountId, credentials.apiKey, credentials.privateKey, credentials.instrument)
            .joinToString("\u001F")
            .toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(payload)
        val packed = ByteBuffer.allocate(4 + cipher.iv.size + encrypted.size)
            .putInt(cipher.iv.size).put(cipher.iv).put(encrypted).array()
        check(preferences.edit().putString(CREDENTIALS, Base64.encodeToString(packed, Base64.NO_WRAP)).commit()) {
            "Unable to persist encrypted credentials"
        }
    }

    fun load(): Credentials? {
        val encoded = preferences.getString(CREDENTIALS, null) ?: return null
        return try {
            val packed = Base64.decode(encoded, Base64.NO_WRAP)
            val buffer = ByteBuffer.wrap(packed)
            val ivLength = buffer.int
            require(ivLength in 12..16 && buffer.remaining() > ivLength)
            val iv = ByteArray(ivLength).also { buffer.get(it) }
            val encrypted = ByteArray(buffer.remaining()).also { buffer.get(it) }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            val fields = cipher.doFinal(encrypted).toString(Charsets.UTF_8).split("\u001F")
            require(fields.size == 5)
            Credentials(fields[0], fields[1], fields[2], fields[3], fields[4])
        } catch (_: Exception) {
            null
        }
    }

    fun clear() {
        check(preferences.edit().remove(CREDENTIALS).commit()) { "Unable to clear encrypted credentials" }
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(KEY_ALIAS) }
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "vela_exness_credentials_v1"
        const val CREDENTIALS = "encrypted_payload"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
