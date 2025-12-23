package ru.kafpin.booking.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import ru.kafpin.activities.BookingDetailsActivity

@RunWith(AndroidJUnit4::class)
class BookingDetailsActivityLaunchTest {

    @Test
    fun экран_деталей_брони_должен_запускаться() {
        val сценарий = ActivityScenario.launch(BookingDetailsActivity::class.java)

        сценарий.onActivity { активность ->
            assertNotNull("Activity не должна быть null", активность)
        }

        сценарий.close()
    }
}