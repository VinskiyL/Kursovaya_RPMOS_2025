package ru.kafpin.registration.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import org.junit.Test
import org.junit.runner.RunWith
import ru.kafpin.R
import ru.kafpin.activities.RegisterActivity

@RunWith(AndroidJUnit4::class)
class ValidationErrorsTest {

    @Test
    fun при_пустых_обязательных_полях_должны_показываться_ошибки() {
        ActivityScenario.launch(RegisterActivity::class.java).use {
            onView(withId(R.id.btnRegister)).perform(click())
            onView(withId(R.id.tilSurname))
                .check(matches(hasErrorText("Введите фамилию")))

            onView(withId(R.id.tilName))
                .check(matches(hasErrorText("Введите имя")))

            onView(withId(R.id.tilCity))
                .check(matches(hasErrorText("Введите город")))

            onView(withId(R.id.tilStreet))
                .check(matches(hasErrorText("Введите улицу")))

            onView(withId(R.id.tilPassportSeries))
                .check(matches(hasErrorText("Введите серию паспорта")))

            onView(withId(R.id.tilPhone))
                .check(matches(hasErrorText("Введите телефон")))

            onView(withId(R.id.tilMail))
                .check(matches(hasErrorText("Введите email")))

            onView(withId(R.id.tilLogin))
                .check(matches(hasErrorText("Введите логин")))

            onView(withId(R.id.tilPassword))
                .check(matches(hasErrorText("Введите пароль")))
        }
    }
}