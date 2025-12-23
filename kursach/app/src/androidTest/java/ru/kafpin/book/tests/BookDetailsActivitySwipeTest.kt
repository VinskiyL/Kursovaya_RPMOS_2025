package ru.kafpin.book.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import org.junit.Test
import org.junit.runner.RunWith
import ru.kafpin.R
import ru.kafpin.activities.BookDetailsActivity

@RunWith(AndroidJUnit4::class)
class BookDetailsActivitySwipeTest {

    @Test
    fun swipeRefreshLayout_должен_быть_виден() {
        ActivityScenario.launch(BookDetailsActivity::class.java).use {
            onView(withId(R.id.swipeRefreshLayout))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
        }
    }
}