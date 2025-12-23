package ru.kafpin.order.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import org.junit.Test
import org.junit.runner.RunWith
import ru.kafpin.R
import ru.kafpin.activities.OrdersDetailActivity

@RunWith(AndroidJUnit4::class)
class OrdersDetailActivityRetryTest {

    @Test
    fun кнопка_повтора_должна_быть_в_errorLayout() {
        ActivityScenario.launch(OrdersDetailActivity::class.java).use {
            onView(withId(R.id.btnRetry)).check(matches(isDisplayed()))
            onView(withId(R.id.btnRetry)).check(matches(isClickable()))
            onView(withId(R.id.btnRetry)).check(matches(withText("Повторить")))
        }
    }
}