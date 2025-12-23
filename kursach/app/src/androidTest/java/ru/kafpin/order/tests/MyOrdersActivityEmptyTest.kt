package ru.kafpin.order.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import org.junit.Test
import org.junit.runner.RunWith
import ru.kafpin.R
import ru.kafpin.activities.MyOrdersActivity

@RunWith(AndroidJUnit4::class)
class MyOrdersActivityEmptyTest {

    @Test
    fun при_пустом_списке_должен_показываться_emptyView() {
        ActivityScenario.launch(MyOrdersActivity::class.java).use {
            onView(withId(R.id.emptyView))
                .check(matches(isDisplayed()))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            onView(withId(R.id.recyclerView))
                .check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }
}