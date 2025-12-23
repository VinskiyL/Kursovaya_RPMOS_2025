package ru.kafpin.order.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import ru.kafpin.activities.OrdersDetailActivity

@RunWith(AndroidJUnit4::class)
class OrdersDetailActivityLaunchTest {

    @Test
    fun экран_деталей_заказа_должен_запускаться() {
        val сценарий = ActivityScenario.launch(OrdersDetailActivity::class.java)

        сценарий.onActivity { активность ->
            assertNotNull("Activity не должна быть null", активность)
        }

        сценарий.close()
    }
}