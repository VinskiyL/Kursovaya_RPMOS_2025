package ru.kafpin.registration.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import ru.kafpin.R
import ru.kafpin.activities.RegisterActivity

@RunWith(AndroidJUnit4::class)
class DatePickerTest {

    @Test
    fun при_нажатии_на_кнопку_выбора_даты_должен_открываться_DatePicker() {
        val устройство = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        ActivityScenario.launch(RegisterActivity::class.java).use {
            onView(withId(R.id.btnBirthday)).perform(click())

            val найдено = устройство.wait(
                Until.hasObject(By.clazz("android.widget.DatePicker")),
                3000
            )

            assertTrue("DatePicker должен появиться после нажатия кнопки", найдено)
            
            устройство.findObject(By.text("OK")).click()
        }
    }
}