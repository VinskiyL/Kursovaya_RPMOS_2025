package ru.kafpin.booking.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import ru.kafpin.activities.MyBookingsActivity

@RunWith(AndroidJUnit4::class)
class MyBookingsActivityLaunchTest {

    @Test
    fun экран_моих_бронирований_должен_запускаться() {
        val сценарий = ActivityScenario.launch(MyBookingsActivity::class.java)

        сценарий.onActivity { активность ->
            assertNotNull("Activity не должна быть null", активность)
            assertTrue("Activity должна быть активна", !активность.isFinishing)
        }

        сценарий.close()
    }
}