package ru.kafpin.comment.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import ru.kafpin.activities.CommentsActivity

@RunWith(AndroidJUnit4::class)
class CommentsActivityLaunchTest {

    @Test
    fun экран_комментариев_должен_запускаться() {
        val сценарий = ActivityScenario.launch(CommentsActivity::class.java)

        сценарий.onActivity { активность ->
            assertNotNull("Activity не должна быть null", активность)
            assertTrue("Activity должна быть активна", !активность.isFinishing)
        }

        сценарий.close()
    }
}