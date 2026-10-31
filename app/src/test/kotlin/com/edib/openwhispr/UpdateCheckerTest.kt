package com.edib.openwhispr

import org.junit.Assert.*
import org.junit.Test

// [security] Covers the update checksum helpers.
class UpdateCheckerTest {

    private val hex = "a".repeat(64)

    @Test fun `parses github digest`() {
        assertEquals(hex, UpdateChecker.parseSha256Digest("sha256:$hex"))
    }

    @Test fun `parses digest case-insensitively`() {
        assertEquals(hex, UpdateChecker.parseSha256Digest("SHA256:${hex.uppercase()}"))
    }

    @Test fun `accepts bare hex`() {
        assertEquals(hex, UpdateChecker.parseSha256Digest(hex))
    }

    @Test fun `rejects missing or malformed digest`() {
        assertNull(UpdateChecker.parseSha256Digest(null))
        assertNull(UpdateChecker.parseSha256Digest(""))
        assertNull(UpdateChecker.parseSha256Digest("sha256:abc"))
        assertNull(UpdateChecker.parseSha256Digest("sha256:" + "g".repeat(64)))
        assertNull(UpdateChecker.parseSha256Digest("sha512:" + "a".repeat(128)))
    }

    @Test fun `checksum must match`() {
        assertTrue(UpdateChecker.checksumMatches(hex, hex))
        assertTrue(UpdateChecker.checksumMatches(hex, hex.uppercase()))
        assertFalse(UpdateChecker.checksumMatches(hex, "b".repeat(64)))
    }

    @Test fun `missing checksum is never a match`() {
        assertFalse(UpdateChecker.checksumMatches(null, hex))
    }

    @Test fun `recognizes only signed main-build release tags`() {
        assertEquals(24, UpdateChecker.dockBuildNumber("gboard-dock-main-24"))
        assertNull(UpdateChecker.dockBuildNumber("gboard-dock-v9-check-22"))
        assertNull(UpdateChecker.dockBuildNumber("v4.0.0"))
        assertNull(UpdateChecker.dockBuildNumber("gboard-dock-main-nope"))
    }

    @Test fun `compares release run number against installed version code`() {
        assertTrue(UpdateChecker.isNewerDockBuild("gboard-dock-main-24", 40023L))
        assertFalse(UpdateChecker.isNewerDockBuild("gboard-dock-main-23", 40023L))
        assertFalse(UpdateChecker.isNewerDockBuild("gboard-dock-main-22", 40023L))
        assertFalse(UpdateChecker.isNewerDockBuild("gboard-dock-v8-test", 40023L))
    }

}
