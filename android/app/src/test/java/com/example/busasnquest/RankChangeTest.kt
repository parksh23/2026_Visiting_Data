package com.example.busasnquest

import com.example.busasnquest.notification.RankChangeDirection
import com.example.busasnquest.notification.detectRankChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RankChangeTest {
    @Test
    fun firstSnapshotAndSameRankDoNotNotify() {
        assertNull(detectRankChange(null, 5))
        assertNull(detectRankChange(5, 5))
        assertNull(detectRankChange(5, 0))
    }

    @Test
    fun detectsUpAndDownChanges() {
        val up = requireNotNull(detectRankChange(8, 3))
        assertEquals(RankChangeDirection.UP, up.direction)
        assertEquals(5, up.difference)

        val down = requireNotNull(detectRankChange(3, 7))
        assertEquals(RankChangeDirection.DOWN, down.direction)
        assertEquals(4, down.difference)
    }
}
