package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.LockHistoryItem
import com.example.data.TabLockPreferences
import com.example.data.UnlockMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TabLock", appName)
    }

    @Test
    fun `test default in-app PIN is 1234`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = TabLockPreferences(context)
        assertEquals("1234", prefs.inAppPin)
    }

    @Test
    fun `test update in-app PIN and unlock mode`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = TabLockPreferences(context)
        prefs.inAppPin = "9876"
        assertEquals("9876", prefs.inAppPin)

        prefs.unlockMode = UnlockMode.SYSTEM_DEFAULT_PASSWORD
        assertEquals(UnlockMode.SYSTEM_DEFAULT_PASSWORD, prefs.unlockMode)
    }

    @Test
    fun `test lock history recording`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = TabLockPreferences(context)
        prefs.clearHistory()
        assertTrue(prefs.getHistory().isEmpty())

        prefs.addHistoryItem(
            LockHistoryItem(
                id = "test_1",
                timestamp = System.currentTimeMillis(),
                durationMinutes = 15,
                triggerSource = "Countdown Timer",
                unlockMethodUsed = "Device Password",
                completedSuccessfully = true
            )
        )

        val history = prefs.getHistory()
        assertEquals(1, history.size)
        assertEquals("Countdown Timer", history[0].triggerSource)
    }
}
