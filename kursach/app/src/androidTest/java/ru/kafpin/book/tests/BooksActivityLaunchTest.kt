package ru.kafpin.book.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import ru.kafpin.activities.BooksActivity

@RunWith(AndroidJUnit4::class)
class BooksActivityLaunchTest {

    @Test
    fun экран_библиотеки_должен_запускаться() {
        val сценарий = ActivityScenario.launch(BooksActivity::class.java)

        сценарий.onActivity { активность ->
            assertNotNull("Activity не должна быть null", активность)
            assertTrue("Activity должна быть активна", !активность.isFinishing)
        }

        сценарий.close()
    }
}