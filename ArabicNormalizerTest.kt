package com.shafii.encyclopedia

import com.shafii.encyclopedia.util.ArabicNormalizer
import com.shafii.encyclopedia.util.DatabasePackManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicNormalizerTest {

    @Test
    fun stripsDiacriticsAndTatweel() {
        val raw = "لَحْمُ الْجَزُورِ وَالْإِبِـلِ"
        val norm = ArabicNormalizer.normalize(raw)
        assertEquals("لحم الجزور والابل", norm)
    }

    @Test
    fun normalizesHamzahTaMarbutaAndAlefMaqsura() {
        val raw = "زَكَاةُ الحُلِيِّ والفتوى عند النوويّ"
        val norm = ArabicNormalizer.normalize(raw)
        assertEquals("زكاه الحلي والفتوي عند النووي", norm)
    }

    @Test
    fun buildsSafeFts5PrefixQuery() {
        val q = ArabicNormalizer.buildFts5Query("الشفق الأحمر")
        assertEquals("الشفق* AND الاحمر*", q)

        val scoped = ArabicNormalizer.buildFts5Query("النووي", "scholars_norm")
        assertEquals("scholars_norm:النووي*", scoped)
    }

    @Test
    fun fallbackTextMatchesEditorialPolicy() {
        assertTrue(DatabasePackManager.FALLBACK_MISSING_TEXT == "لم يثبت لدينا نص موثق")
    }
}
