package eu.kanade.tachiyomi.core.security

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {

    private const val ITERATIONS = 60_000

    fun newSalt(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()

    fun hash(pin: String, salt: String): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt.toByteArray(), ITERATIONS, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded.toHex()
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

}
