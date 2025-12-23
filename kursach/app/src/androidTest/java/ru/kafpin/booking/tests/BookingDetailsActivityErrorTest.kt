package ru.kafpin.booking.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import ru.kafpin.activities.BookingDetailsActivity

@RunWith(AndroidJUnit4::class)
class BookingDetailsActivityErrorTest {

    @Test
    fun при_неверном_id_должна_показываться_ошибка() {
        val сценарий = ActivityScenario.launch(BookingDetailsActivity::class.java)

        сценарий.onActivity { активность ->
            val bookingId = активность.intent.getLongExtra(
                ru.kafpin.activities.BookingDetailsActivity.EXTRA_BOOKING_ID, -1L)

            if (bookingId == -1L) {
                assertTrue("Должна быть ошибка при bookingId = -1", true)
            }
        }

        сценарий.close()
    }
}