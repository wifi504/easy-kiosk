package top.lhlnb.easykiosk

import org.junit.Assert.*
import org.junit.Test

class TotpTest {
    private val key = Totp.decodeSecret("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")
    @Test fun rfc6238Vectors() {
        val vectors = listOf(59L to "287082", 1111111109L to "081804", 1111111111L to "050471", 1234567890L to "005924", 2000000000L to "279037", 20000000000L to "353130")
        for ((seconds, expected) in vectors) assertEquals(expected, Totp.code(key, seconds / 30))
    }
    @Test fun permitsOnlyAdjacentStepsAndSixDigits() {
        val step = 1234567L
        assertNotNull(Totp.matchingStep(key, Totp.code(key, step - 1), step * 30000))
        assertNotNull(Totp.matchingStep(key, Totp.code(key, step + 1), step * 30000))
        assertNull(Totp.matchingStep(key, Totp.code(key, step - 2), step * 30000))
        assertNull(Totp.matchingStep(key, "1234", step * 30000))
    }
    @Test fun refusesWeakAndInvalidBase32() {
        for (invalid in listOf("123456", "AAAAAAAA", "A".repeat(27), "A".repeat(104))) {
            try { Totp.decodeSecret(invalid); fail("Invalid key accepted") } catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun requiresTwoAgreeingNetworkSources() {
        assertEquals(1001500L, NetworkTimeConsensus.resolve(listOf(NetworkTimeSample(1000000, 100), NetworkTimeSample(1001000, 100)), 1100))
        try { NetworkTimeConsensus.resolve(listOf(NetworkTimeSample(1000000, 100)), 1100); fail("One source accepted") } catch (_: IllegalArgumentException) { }
        try { NetworkTimeConsensus.resolve(listOf(NetworkTimeSample(1000000, 100), NetworkTimeSample(1020000, 100)), 1100); fail("Disagreement accepted") } catch (_: IllegalStateException) { }
    }
}
