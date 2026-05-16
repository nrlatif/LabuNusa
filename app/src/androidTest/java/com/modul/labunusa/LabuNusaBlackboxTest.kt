package com.modul.LabuNusa

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.modul.LabuNusa.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LabuNusaBlackboxTest {

    @Test
    fun testNavigationAndCamera() {
        ActivityScenario.launch(MainActivity::class.java)

        // Give it time to render Beranda
        Thread.sleep(3000)

        // 1. Click on 'Riwayat'
        onView(withId(R.id.nav_riwayat)).perform(click())
        Thread.sleep(2000)

        // 2. Click on 'Informasi'
        onView(withId(R.id.nav_informasi)).perform(click())
        Thread.sleep(2000)

        // 3. Click on 'Tentang'
        onView(withId(R.id.nav_tentang)).perform(click())
        Thread.sleep(2000)

        // 4. Go back to 'Beranda'
        onView(withId(R.id.nav_beranda)).perform(click())
        Thread.sleep(2000)

        // 5. Click the FAB Camera
        onView(withId(R.id.fab_scan)).perform(click())
        Thread.sleep(4000)
    }
}
