package io.music_assistant.sendspin.noise.crypto

import co.touchlab.kermit.Logger
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.ChaCha20Poly1305
import dev.whyoleg.cryptography.algorithms.HMAC
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.random.CryptographyRandom

/**
 * [NoiseCrypto] backed by cryptography-kotlin, which delegates to OS-native
 * crypto: the JCA (JDK provider) on Android and CryptoKit on iOS. The
 * provider artifacts are per-target runtime dependencies;
 * `CryptographyProvider.Default` resolves whichever one is registered on the
 * current platform.
 *
 * X25519 is the exception. It reaches the platform JCA only at API 33, below
 * this module's minimum, so it goes through [X25519Backend] instead — see
 * `defaultX25519Backend` for the per-target choice. ChaCha20-Poly1305,
 * SHA-256 and HMAC are available across the whole supported range and stay on
 * the platform provider.
 */
internal class CryptographyKotlinNoiseCrypto(
    private val provider: CryptographyProvider = CryptographyProvider.Default,
    private val x25519: X25519Backend = defaultX25519Backend(provider),
) : NoiseCrypto {
    private val logger = Logger.withTag("NoiseCrypto")

    // Resolved once — hashing hits these on the hot path. The AEAD
    // algorithm is deliberately NOT cached: the JDK provider pools Cipher
    // instances per algorithm handle, and a shared pool trips the JDK's
    // key+nonce-reuse guard when one process runs both Noise roles
    // (loopback tests) — encrypt and decrypt legitimately use the same
    // key and nonce there.
    private val sha256Hasher by lazy { provider.get(SHA256).hasher() }
    private val hmac by lazy { provider.get(HMAC) }

    override suspend fun generateX25519KeyPair(): X25519KeyPair = x25519.generateKeyPair()

    override suspend fun x25519PublicKey(privateKey: ByteArray): ByteArray =
        x25519.publicKey(privateKey)

    override suspend fun dh(privateKey: ByteArray, publicKey: ByteArray): ByteArray =
        x25519.sharedSecret(privateKey, publicKey)

    @OptIn(DelicateCryptographyApi::class)
    override suspend fun aeadEncrypt(
        key: ByteArray,
        nonce: ByteArray,
        associatedData: ByteArray,
        plaintext: ByteArray,
    ): ByteArray {
        return freshCipher(javax.crypto.Cipher.ENCRYPT_MODE, key, nonce, associatedData).doFinal(plaintext)
    }

    override suspend fun aeadDecrypt(
        key: ByteArray,
        nonce: ByteArray,
        associatedData: ByteArray,
        ciphertext: ByteArray,
    ): ByteArray? = try {
        freshCipher(javax.crypto.Cipher.DECRYPT_MODE, key, nonce, associatedData).doFinal(ciphertext)
    } catch (_: javax.crypto.AEADBadTagException) {
        null
    }

    // Never pool a ChaCha cipher across send/receive roles. JCA rejects initializing a
    // pooled instance twice with the same key/nonce, including a legitimate decrypt.
    // A new platform Cipher per message also keeps independent sessions isolated.
    private fun freshCipher(mode: Int, key: ByteArray, nonce: ByteArray, aad: ByteArray) =
        javax.crypto.Cipher.getInstance("ChaCha20-Poly1305").apply {
            init(mode, javax.crypto.spec.SecretKeySpec(key, "ChaCha20"), javax.crypto.spec.IvParameterSpec(nonce))
            updateAAD(aad)
        }

    override suspend fun sha256(data: ByteArray): ByteArray = sha256Hasher.hash(data)

    override suspend fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val hmacKey = hmac
            .keyDecoder(SHA256)
            .decodeFromByteArray(HMAC.Key.Format.RAW, key)
        return hmacKey.signatureGenerator().generateSignature(data)
    }

    override fun randomBytes(count: Int): ByteArray = CryptographyRandom.nextBytes(count)
}
