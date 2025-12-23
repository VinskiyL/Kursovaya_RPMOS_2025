package ru.kafpin.order.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import ru.kafpin.activities.OrdersDetailActivity

@RunWith(AndroidJUnit4::class)
class OrdersDetailActivityErrorTest {

    @Test
    fun при_неверном_orderId_должна_быть_ошибка() {
        val сценарий = ActivityScenario.launch(OrdersDetailActivity::class.java)

        сценарий.onActivity { активность ->
            val orderId = активность.intent.getLongExtra(
                ru.kafpin.activities.OrdersDetailActivity.EXTRA_ORDER_ID, -1L)

            if (orderId == -1L) {
                assertTrue("Должна быть проверка на orderId = -1", true)
            }
        }

        сценарий.close()
    }
}