package ru.kafpin.registration.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import ru.kafpin.activities.RegisterActivity

@RunWith(AndroidJUnit4::class)
class RegisterActivityLaunchTest {

    @Test
    fun экран_регистрации_должен_успешно_запускаться() {
        val сценарий = ActivityScenario.launch(RegisterActivity::class.java)

        сценарий.onActivity { активность ->
            assertNotNull("Activity не должна быть null", активность)
            assertTrue("Activity должна быть на экране", активность.isFinishing.not())
        }

        сценарий.close()
    }
}