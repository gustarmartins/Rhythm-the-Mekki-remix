/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Tokens and Sendspin identity are encrypted with a device-bound, non-exportable key. */
internal class MusicAssistantAccount(context: Context) {
    private val prefs = context.getSharedPreferences("music_assistant_native", Context.MODE_PRIVATE)
    private val legacy = context.getSharedPreferences("music_assistant", Context.MODE_PRIVATE)
    val server: String get() = prefs.getString("server", null) ?: legacy.getString("server", "").orEmpty()
    val username: String get() = prefs.getString("username", "").orEmpty()
    // This is a configuration check, not authentication. Never decrypt in UI predicates.
    val hasCredentials: Boolean get() = server.isNotBlank() &&
        !prefs.getString("secret.token", null).isNullOrBlank()
    val token: String get() = read("token")?.toString(Charsets.UTF_8).orEmpty()
    class Session(val server: String, val username: String, val token: String)
    fun snapshot() = synchronized(lock) { Session(server, username, token) }
    fun save(server: String, username: String, token: String) = synchronized(lock) {
        // Commit the server and encrypted token together: never send a new account's
        // bearer to the previous server during a concurrent catalog request.
        val encrypted = encrypt("token", token.toByteArray())
        check(prefs.edit().putString("server", server).putString("username", username)
            .putString("secret.token", encrypted).commit())
        legacy.edit().putString("server", server).apply()
    }
    fun clear() = synchronized(lock) { check(prefs.edit().clear().commit()) }
    fun read(name: String): ByteArray? = synchronized(lock) {
        val encoded = prefs.getString("secret.$name", null) ?: return@synchronized null
        val parts = encoded.split(':', limit = 2)
        require(parts.size == 2)
        Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
            updateAAD(name.toByteArray())
            doFinal(Base64.decode(parts[1], Base64.NO_WRAP))
        }
    }
    fun write(name: String, bytes: ByteArray) = synchronized(lock) {
        check(prefs.edit().putString("secret.$name", encrypt(name, bytes)).commit())
    }
    private fun encrypt(name: String, bytes: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key()); updateAAD(name.toByteArray())
        }
        val encrypted = cipher.doFinal(bytes)
        return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }
    fun delete(name: String) { check(prefs.edit().remove("secret.$name").commit()) }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generateKey()
        }
    }
    private companion object { val lock = Any(); const val ALIAS = "rhythm.music-assistant.native.v1" }
}
