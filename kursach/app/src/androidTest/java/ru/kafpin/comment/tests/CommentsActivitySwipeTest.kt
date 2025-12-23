package ru.kafpin.comment.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import org.junit.Test
import org.junit.runner.RunWith
import ru.kafpin.R
import ru.kafpin.activities.CommentsActivity

@RunWith(AndroidJUnit4::class)
class CommentsActivitySwipeTest {

    @Test
    fun SwipeRefresh_должен_быть_настроен_правильно() {
        ActivityScenario.launch(CommentsActivity::class.java).use {
            onView(withId(R.id.swipeRefresh))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
        }
    }
}