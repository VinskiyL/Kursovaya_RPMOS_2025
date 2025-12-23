package ru.kafpin.registration.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import org.hamcrest.Matchers.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.kafpin.R
import ru.kafpin.activities.RegisterActivity

@RunWith(AndroidJUnit4::class)
class EducationSpinnerTest {

    @Test
    fun спиннер_образования_должен_содержать_9_вариантов() {
        ActivityScenario.launch(RegisterActivity::class.java).use {
            onView(withId(R.id.spinnerEducation)).perform(click())

            val вариантыОбразования = listOf(
                "Нет образования",
                "Начальное общее (9 классов)",
                "Среднее общее (11 классов)",
                "Среднее профессиональное (колледж/техникум)",
                "Неоконченное высшее",
                "Высшее (бакалавриат)",
                "Высшее (специалитет)",
                "Высшее (магистратура)",
                "Учёная степень (кандидат/доктор наук)"
            )

            for (вариант in вариантыОбразования) {
                onView(withText(вариант)).check(matches(isDisplayed()))
            }

            onView(withText("Нет образования")).perform(click())
        }
    }
}