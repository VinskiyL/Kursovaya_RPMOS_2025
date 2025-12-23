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
class ProfileActivityStatesTest {

    @Test
    fun layout_ошибки_должен_быть_скрыт_изначально() {
        ActivityScenario.launch(ProfileActivity::class.java).use {
            onView(withId(R.id.errorLayout))
                .check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }
}