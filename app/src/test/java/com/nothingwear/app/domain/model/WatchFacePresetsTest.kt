package com.nothingwear.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchFacePresetsTest {

    @Test
    fun allSixCategoriesProduceDistinctValid466Presets() {
        val categories = WatchFaceCategory.entries
        assertEquals(6, categories.size)

        val presets = categories.map(WatchFacePresets::defaultFor)
        presets.forEach { preset ->
            assertTrue(preset.name.isNotBlank())
            assertTrue(preset.timeScale in 0.8f..1.25f)
        }
        assertEquals(6, presets.map { it.name }.toSet().size)
        assertEquals(466, WatchFaceProject.TARGET_RESOLUTION_PX)
    }

    @Test
    fun allFiveMiFitnessKustomTemplatesHaveDistinctNamesAndSafeZoneNotes() {
        val kustomTemplates = MiFitnessCustomTemplate.entries.filter { it != MiFitnessCustomTemplate.NONE }
        assertEquals(5, kustomTemplates.size)
        assertEquals(5, kustomTemplates.map { it.displayName }.toSet().size)
        kustomTemplates.forEach { template ->
            assertTrue(template.safeZoneNote.isNotBlank())
        }
    }
}
