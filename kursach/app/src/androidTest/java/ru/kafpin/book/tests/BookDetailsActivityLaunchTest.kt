package ru.kafpin.book.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import ru.kafpin.activities.BookDetailsActivity

@RunWith(AndroidJUnit4::class)
class BookDetailsActivityLaunchTest {

    @Test
    fun экран_деталей_книги_должен_запускаться() {
        val сценарий = ActivityScenario.launch(BookDetailsActivity::class.java)

        сценарий.onActivity { активность ->
            assertNotNull("Activity не должна быть null", активность)
        }

        сценарий.close()
    }
}