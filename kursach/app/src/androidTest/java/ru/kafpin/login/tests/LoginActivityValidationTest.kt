package ru.kafpin.login.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import org.junit.Test
import org.junit.runner.RunWith
import ru.kafpin.R
import ru.kafpin.activities.LoginActivity

@RunWith(AndroidJUnit4::class)
class LoginActivityValidationTest {

    @Test
    fun кнопка_входа_должна_оставаться_кликабельной_при_вводе_данных() {
        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(withId(R.id.etLogin)).perform(clearText())
            onView(withId(R.id.etPassword)).perform(clearText())

            onView(withId(R.id.btnLogin))
                .check(matches(isEnabled()))
                .perform(click())
        }
    }
}