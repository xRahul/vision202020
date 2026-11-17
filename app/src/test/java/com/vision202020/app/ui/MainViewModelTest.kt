package com.vision202020.app.ui

import com.vision202020.app.data.EnforcementMode
import com.vision202020.app.data.PreferencesRepository
import com.vision202020.app.data.TimerProfile
import com.vision202020.app.data.UserPreferences
import com.vision202020.app.domain.TimerStatus
import com.vision202020.app.ui.main.MainViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var mockRepository: PreferencesRepository
    private val testPrefsFlow = MutableStateFlow(UserPreferences())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mockk(relaxed = true)
        every { mockRepository.preferencesFlow } returns testPrefsFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialPreferencesLoaded() = runTest(testDispatcher) {
        val viewModel = MainViewModel(mockRepository)

        val loadedPrefs = viewModel.userPreferences.first()
        assertEquals(20, loadedPrefs.workDurationMinutes)
        assertEquals(TimerProfile.MOBILE_SCREEN_AWARE, loadedPrefs.timerProfile)
    }

    @Test
    fun testUpdateProfileDispatchesToRepo() = runTest(testDispatcher) {
        val viewModel = MainViewModel(mockRepository)
        viewModel.setTimerProfile(TimerProfile.LAPTOP_DESK)

        coVerify { mockRepository.updateTimerProfile(TimerProfile.LAPTOP_DESK) }
    }

    @Test
    fun testUpdateEnforcementModeDispatchesToRepo() = runTest(testDispatcher) {
        val viewModel = MainViewModel(mockRepository)
        viewModel.setEnforcementMode(EnforcementMode.NOTIFICATION_ONLY)

        coVerify { mockRepository.updateEnforcementMode(EnforcementMode.NOTIFICATION_ONLY) }
    }
}
