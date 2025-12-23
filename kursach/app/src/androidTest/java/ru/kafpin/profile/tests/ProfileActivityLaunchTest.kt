package ru.kafpin.profile.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import ru.kafpin.activities.ProfileActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import ru.kafpin.R

@RunWith(AndroidJUnit4::class)
class ProfileActivityLaunchTest {

    @Test
    fun экран_профиля_должен_показывать_основные_элементы() {
        ActivityScenario.launch(ProfileActivity::class.java).use {
            onView(withText("Мой профиль")).check(matches(isDisplayed()))
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()))
            onView(withId(R.id.contentLayout)).check(matches(isDisplayed()))
        }
    }
}