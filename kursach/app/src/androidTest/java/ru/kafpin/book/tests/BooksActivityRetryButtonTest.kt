package ru.kafpin.book.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import org.junit.Test
import org.junit.runner.RunWith
import ru.kafpin.R
import ru.kafpin.activities.BooksActivity

@RunWith(AndroidJUnit4::class)
class BooksActivityRetryButtonTest {

    @Test
    fun кнопка_повтора_должна_быть_правильно_настроена() {
        ActivityScenario.launch(BooksActivity::class.java).use {
            onView(withId(R.id.retryButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .check(matches(withText("Повторить")))
        }
    }
}