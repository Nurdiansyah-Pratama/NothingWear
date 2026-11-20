package com.nothingwear.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AccentColorOptionTest {

    @Test
    fun fromIdResolvesKnownOptionAndFallsBackToSignalRed() {
        assertEquals(AccentColorOption.CYBER_CYAN, AccentColorOption.fromId("cyber_cyan"))
        assertEquals(AccentColorOption.MONOCHROME, AccentColorOption.fromId("monochrome"))
        assertEquals(AccentColorOption.SIGNAL_RED, AccentColorOption.fromId("unknown_id"))
        assertEquals(AccentColorOption.SIGNAL_RED, AccentColorOption.fromId(null))
    }

    @Test
    fun allAccentOptionsHaveUniqueIds() {
        val ids = AccentColorOption.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
