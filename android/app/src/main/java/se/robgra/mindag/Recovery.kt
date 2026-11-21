package se.robgra.mindag

import java.security.MessageDigest
import java.security.SecureRandom

/** Återställningskod för glömd PIN. Visas en gång; bara en hash sparas. */
object Recovery {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // inga 0/O/1/I
    fun newCode(rnd: SecureRandom = SecureRandom()): String =
        (1..8).map { ALPHABET[rnd.nextInt(ALPHABET.length)] }.joinToString("").let { it.substring(0, 4) + "-" + it.substring(4) }
    fun normalize(code: String) = code.uppercase().filter { it in ALPHABET }
    fun hash(code: String): String = MessageDigest.getInstance("SHA-256")
        .digest(("mindag-rec:" + normalize(code)).toByteArray()).joinToString("") { "%02x".format(it) }
    fun matches(code: String, h: String) = h.isNotEmpty() && normalize(code).length == 8 && hash(code) == h
}
