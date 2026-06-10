package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.CarbonDao
import com.example.data.CarbonRepository
import com.example.data.LoggedAction
import com.example.data.TrackedDay
import com.example.ui.CarbonViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CarbonViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var viewModel: CarbonViewModel
    private lateinit var fakeDao: FakeCarbonDao
    private lateinit var fakeRepository: CarbonRepository
    private val activeJobs = mutableListOf<Job>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        fakeDao = FakeCarbonDao()
        fakeRepository = CarbonRepository(fakeDao)
        viewModel = CarbonViewModel(application, fakeRepository)

        // Properly wrap the dispatcher in a CoroutineScope to launch collectors
        val scope = CoroutineScope(testDispatcher)
        activeJobs.add(scope.launch {
            viewModel.uiState.collect {}
        })
        activeJobs.add(scope.launch {
            viewModel.loggedActions.collect {}
        })
        activeJobs.add(scope.launch {
            viewModel.trackingStreak.collect {}
        })
    }

    @After
    fun tearDown() {
        activeJobs.forEach { it.cancel() }
        activeJobs.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun testUpdateUserProfile() = runTest(testDispatcher) {
        viewModel.updateUserProfile("Eco Warrior", 15.5f)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Eco Warrior", viewModel.userName.value)
        assertEquals(15.5f, viewModel.dailyBudget.value)
    }

    @Test
    fun testTransportEmissionsCalculation() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()
        var state = viewModel.uiState.value
        assertEquals(0f, state.transportCo2)

        // Set transport inputs: 20 km on car, 2 hours on flight, 50 km on transit
        viewModel.updateCarKm(20f)
        viewModel.updateFlightHours(2.0f)
        viewModel.updateTransitKm(50f)
        
        testDispatcher.scheduler.advanceUntilIdle()
        state = viewModel.uiState.value

        // Formula: (carKm * 0.20) + (flightHours * 250 / 365) + (transitKm * 0.08)
        val expectedCarCo2 = 20f * 0.20f
        val expectedFlightCo2 = 2.0f * 250f / 365f
        val expectedTransitCo2 = 50f * 0.08f
        val expectedTotal = expectedCarCo2 + expectedFlightCo2 + expectedTransitCo2

        assertEquals(expectedTotal, state.transportCo2, 0.01f)
    }

    @Test
    fun testUtilityEmissionsCalculation() = runTest(testDispatcher) {
        viewModel.updateElectricityKwh(15f)
        viewModel.updateHeatingLevel("Low") // Low heating level adds 1.5f
 
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value

        // Formula: (elecKwh * 0.45) + heatingLevel
        val expectedElecCo2 = 15f * 0.45f
        val expectedHeatingCo2 = 1.5f
        val expectedTotal = expectedElecCo2 + expectedHeatingCo2

        assertEquals(expectedTotal, state.utilityCo2, 0.01f)
    }

    @Test
    fun testDietEmissionsCalculation() = runTest(testDispatcher) {
        // Default Not Set is 0.0
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0f, viewModel.uiState.value.dietCo2)

        // Meat Heavy adds 3.3
        viewModel.updateDietPreference("Meat Heavy")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(3.3f, viewModel.uiState.value.dietCo2, 0.01f)

        // Vegetarian is 1.4f
        viewModel.updateDietPreference("Vegetarian")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1.4f, viewModel.uiState.value.dietCo2, 0.01f)
    }

    @Test
    fun testWasteEmissionsAndRecyclingBonus() = runTest(testDispatcher) {
        viewModel.updateTrashBags(4f)
        viewModel.updateRecycled(false)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(6.0f, viewModel.uiState.value.wasteCo2, 0.01f) // 4 * 1.5f = 6.0f

        viewModel.updateRecycled(true) // Should apply 50% discount bonus for recycling
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(3.0f, viewModel.uiState.value.wasteCo2, 0.01f) // 6.0f * 0.5 = 3.0f
    }

    @Test
    fun testLogGreenHabitOffset() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()
        val initialOffset = viewModel.uiState.value.totalOffset
        assertEquals(0f, initialOffset)

        // Log a single green habit
        viewModel.logHabit("Rode a bicycle", 2.5f, "Transport")
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify the offset reactively propagates to UI State
        val nextState = viewModel.uiState.value
        assertEquals(2.5f, nextState.totalOffset, 0.01f)
    }

    @Test
    fun testStreakCalculation() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.trackingStreak.value)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        // Insert today's tracked day
        val todayStr = sdf.format(cal.time)
        val day1 = TrackedDay(date = todayStr, transportCo2 = 1.0f, utilityCo2 = 2.0f, dietCo2 = 1.0f, wasteCo2 = 1.0f, totalCo2 = 5.0f)
        fakeDao.insertTrackedDay(day1)

        // Insert yesterday's tracked day
        cal.add(Calendar.DATE, -1)
        val yesterdayStr = sdf.format(cal.time)
        val day2 = TrackedDay(date = yesterdayStr, transportCo2 = 1.0f, utilityCo2 = 2.0f, dietCo2 = 1.0f, wasteCo2 = 1.0f, totalCo2 = 5.0f)
        fakeDao.insertTrackedDay(day2)

        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.trackingStreak.value)
    }

    @Test
    fun testCopyYesterdayInputs() = runTest(testDispatcher) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        val yesterdayStr = sdf.format(cal.time)

        // Insert some inputs for yesterday
        val yesterdayDay = TrackedDay(
            date = yesterdayStr,
            transportCo2 = 1.0f,
            utilityCo2 = 1.0f,
            dietCo2 = 1.8f,
            wasteCo2 = 1.0f,
            totalCo2 = 4.8f,
            carKm = 25f,
            flightHours = 1f,
            transitKm = 10f,
            elecKwh = 12f,
            heatingLevel = "High",
            dietChoice = "Vegan",
            trashBags = 2f,
            recycled = true
        )
        fakeDao.insertTrackedDay(yesterdayDay)
        testDispatcher.scheduler.advanceUntilIdle()

        // Instruct the view model to carry forward yesterday's inputs to today
        viewModel.copyYesterdayInputs()
        testDispatcher.scheduler.advanceUntilIdle()

        val todayState = viewModel.uiState.value
        assertEquals(25f, todayState.carKm)
        assertEquals(1f, todayState.flightHoursYearly)
        assertEquals(12f, todayState.electricityKwh)
        assertEquals("High", todayState.heatingLevel)
        assertEquals("Vegan", todayState.dietPreference)
    }

    // High fidelity DAO fake definition to satisfy architecture and isolate test dependencies
    class FakeCarbonDao : CarbonDao {
        private val days = MutableStateFlow<Map<String, TrackedDay>>(emptyMap())
        private val actions = MutableStateFlow<List<LoggedAction>>(emptyList())
        private var actionIdCounter = 1L

        override fun getTrackedDay(date: String): Flow<TrackedDay?> {
            return days.map { it[date] }
        }

        override fun getAllTrackedDays(): Flow<List<TrackedDay>> {
            return days.map { it.values.toList() }
        }

        override suspend fun insertTrackedDay(day: TrackedDay) {
            days.update { it + (day.date to day) }
        }

        override fun getLoggedActionsForDate(date: String): Flow<List<LoggedAction>> {
            return actions.map { list -> list.filter { it.date == date } }
        }

        override fun getAllLoggedActions(): Flow<List<LoggedAction>> {
            return actions
        }

        override suspend fun insertLoggedAction(action: LoggedAction) {
            val actionWithId = if (action.id == 0L) action.copy(id = actionIdCounter++) else action
            actions.update { it + actionWithId }
        }

        override suspend fun deleteLoggedAction(id: Long) {
            actions.update { list -> list.filter { it.id != id } }
        }
    }
}
