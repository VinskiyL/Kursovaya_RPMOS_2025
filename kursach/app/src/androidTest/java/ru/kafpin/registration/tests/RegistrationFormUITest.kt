package ru.kafpin.registration.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import org.junit.Test
import org.junit.runner.RunWith
import ru.kafpin.R
import ru.kafpin.activities.RegisterActivity

@RunWith(AndroidJUnit4::class)
class RegistrationFormUITest {

    @Test
    fun все_обязательные_поля_должны_отображаться() {
        ActivityScenario.launch(RegisterActivity::class.java).use {
            onView(withId(R.id.etSurname)).check(matches(isDisplayed()))
            onView(withId(R.id.etName)).check(matches(isDisplayed()))
            onView(withId(R.id.etBirthday)).check(matches(isDisplayed()))
            onView(withId(R.id.spinnerEducation)).check(matches(isDisplayed()))
            onView(withId(R.id.etCity)).check(matches(isDisplayed()))
            onView(withId(R.id.etStreet)).check(matches(isDisplayed()))
            onView(withId(R.id.etHouse)).check(matches(isDisplayed()))
            onView(withId(R.id.etPassportSeries)).check(matches(isDisplayed()))
            onView(withId(R.id.etPassportNumber)).check(matches(isDisplayed()))
            onView(withId(R.id.etIssuedByWhom)).check(matches(isDisplayed()))
            onView(withId(R.id.etDateIssue)).check(matches(isDisplayed()))
            onView(withId(R.id.etPhone)).check(matches(isDisplayed()))
            onView(withId(R.id.etMail)).check(matches(isDisplayed()))
            onView(withId(R.id.etLogin)).check(matches(isDisplayed()))
            onView(withId(R.id.etPassword)).check(matches(isDisplayed()))
            onView(withId(R.id.etConfirmPassword)).check(matches(isDisplayed()))

            onView(withId(R.id.btnRegister)).check(matches(isDisplayed()))
            onView(withId(R.id.btnRegister)).check(matches(withText("Зарегистрироваться")))
        }
    }
}