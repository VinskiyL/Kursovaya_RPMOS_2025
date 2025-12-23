package ru.kafpin.profile.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import org.junit.Test
import org.junit.runner.RunWith
import ru.kafpin.R
import ru.kafpin.activities.ProfileActivity

@RunWith(AndroidJUnit4::class)
class ProfileActivityNavigationTest {

    @Test
    fun toolbar_должен_содержать_кнопки_назад_и_выхода() {
        ActivityScenario.launch(ProfileActivity::class.java).use {
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()))
            onView(withId(R.id.btnLogout)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun layout_контента_должен_быть_видимым_после_загрузки() {
        ActivityScenario.launch(ProfileActivity::class.java).use {
            onView(withId(R.id.contentLayout))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        }
    }
}