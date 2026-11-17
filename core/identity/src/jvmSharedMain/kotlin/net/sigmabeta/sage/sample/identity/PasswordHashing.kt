package net.sigmabeta.sage.sample.identity

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** The JDK name of the only KDF the sample writes, recorded on every Users row. */
const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"

/** OWASP's 2023 floor for PBKDF2-HMAC-SHA256. Rows record their own count, so raising it is safe. */
const val DEFAULT_PBKDF2_ITERATIONS = 600_000

internal const val SALT_BYTES = 16
internal const val HASH_BITS = 256

/** Derives a password verifier: PBKDF2-HMAC-SHA256 with a [HASH_BITS]-bit output. */
internal typealias PasswordKdf = (password: CharArray, salt: ByteArray, iterations: Int) -> ByteArray

internal fun pbkdf2HmacSha256(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
    val spec = PBEKeySpec(password, salt, iterations, HASH_BITS)
    return try {
        SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
    } finally {
        spec.clearPassword()
    }
}

internal fun SecureRandom.nextBytes(count: Int): ByteArray = ByteArray(count).also(::nextBytes)

/** Stored verifier, salt and token-hash columns are standard, padded Base64. */
internal fun ByteArray.toBase64(): String = Base64.getEncoder().encodeToString(this)

internal fun String.fromBase64(): ByteArray = Base64.getDecoder().decode(this)
