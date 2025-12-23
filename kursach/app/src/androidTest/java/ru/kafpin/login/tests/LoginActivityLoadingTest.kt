package ru.kafpin.login.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import org.junit.Test
import org.junit.runner.RunWith
import ru.kafpin.R
import ru.kafpin.activities.LoginActivity

@RunWith(AndroidJUnit4::class)
class LoginActivityLoadingTest {

    @Test
    fun progressBar_должен_быть_скрыт_при_запуске() {
        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(withId(R.id.progressBar))
                .check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }

    @Test
    fun сообщения_об_ошибках_и_успехе_должны_быть_скрыты_при_запуске() {
        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(withId(R.id.tvError))
                .check(matches(withEffectiveVisibility(Visibility.GONE)))

            onView(withId(R.id.tvSuccess))
                .check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }
}