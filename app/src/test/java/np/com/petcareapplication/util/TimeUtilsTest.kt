package np.com.petcareapplication.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests the conversion between the Material 3 TimePicker's 24-hour values
 * and the 12-hour text stored in a task's schedule.
 */
class TimeUtilsTest {

    @Test fun format_morning() = assertEquals("07:05 AM", TimeUtils.formatTime(7, 5))

    @Test fun format_midnight_isTwelveAm() = assertEquals("12:00 AM", TimeUtils.formatTime(0, 0))

    @Test fun format_noon_isTwelvePm() = assertEquals("12:00 PM", TimeUtils.formatTime(12, 0))

    @Test fun format_afternoon() = assertEquals("01:30 PM", TimeUtils.formatTime(13, 30))

    @Test fun parse_pmTime() = assertEquals(18 to 45, TimeUtils.parseTime("06:45 PM"))

    @Test fun parse_twelveAm_isMidnight() = assertEquals(0 to 0, TimeUtils.parseTime("12:00 AM"))

    @Test fun parse_freeText_returnsNull() = assertNull(TimeUtils.parseTime("banana"))

    @Test fun parse_invalidHour_returnsNull() = assertNull(TimeUtils.parseTime("13:00 PM"))

    @Test fun formatThenParse_roundTrip() {
        for (h in 0..23) for (m in listOf(0, 15, 59)) {
            assertEquals(h to m, TimeUtils.parseTime(TimeUtils.formatTime(h, m)))
        }
    }
}