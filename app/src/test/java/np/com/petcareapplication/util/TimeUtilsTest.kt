package np.com.petcareapplication.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Checks the conversion between the clock picker's 24-hour numbers
// and the 12-hour text saved in a task's schedule, e.g. "07:05 AM"
class TimeUtilsTest {

    // formatTime: 24-hour numbers to 12-hour text
    @Test fun format_morning() = assertEquals("07:05 AM", TimeUtils.formatTime(7, 5))

    // Midnight and noon are the easy ones to get wrong, so they get their own tests
    @Test fun format_midnight_isTwelveAm() = assertEquals("12:00 AM", TimeUtils.formatTime(0, 0))

    @Test fun format_noon_isTwelvePm() = assertEquals("12:00 PM", TimeUtils.formatTime(12, 0))

    @Test fun format_afternoon() = assertEquals("01:30 PM", TimeUtils.formatTime(13, 30))

    // parseTime: 12-hour text back to 24-hour numbers
    @Test fun parse_pmTime() = assertEquals(18 to 45, TimeUtils.parseTime("06:45 PM"))

    @Test fun parse_twelveAm_isMidnight() = assertEquals(0 to 0, TimeUtils.parseTime("12:00 AM"))

    // Text that isn't a time (like old tasks typed by hand) should give null instead of crashing
    @Test fun parse_freeText_returnsNull() = assertNull(TimeUtils.parseTime("banana"))

    // There's no 13 on a 12-hour clock
    @Test fun parse_invalidHour_returnsNull() = assertNull(TimeUtils.parseTime("13:00 PM"))

    // Turn every hour (and a few minutes) into text and back again,
    // and check we end up with the same time we started with
    @Test fun formatThenParse_roundTrip() {
        for (h in 0..23) for (m in listOf(0, 15, 59)) {
            assertEquals(h to m, TimeUtils.parseTime(TimeUtils.formatTime(h, m)))
        }
    }
}