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
class LoginActivityInputTest {

    @Test
    fun поля_ввода_должны_принимать_текст_и_кнопка_должна_быть_кликабельной() {
        ActivityScenario.launch(LoginActivity::class.java).use {
            val testLogin = "testuser"
            val testPassword = "testpass123"

            onView(withId(R.id.etLogin)).perform(typeText(testLogin))
            onView(withId(R.id.etPassword)).perform(typeText(testPassword))

            onView(withId(R.id.etLogin)).check(matches(withText(testLogin)))
            onView(withId(R.id.etPassword)).check(matches(withText(testPassword)))

            onView(withId(R.id.btnLogin))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
                .check(matches(withText("Войти")))
        }
    }

    @Test
    fun кнопка_регистрации_должна_быть_видимой() {
        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(withId(R.id.btnRegistration))
                .check(matches(isDisplayed()))
                .check(matches(withText("Регистрация")))
        }
    }
}