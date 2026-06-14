package com.example

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.CarbonDao
import com.example.data.CarbonRepository
import com.example.data.LoggedAction
import com.example.data.TrackedDay
import com.example.ui.CarbonDashboard
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
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w600dp-h3000dp", sdk = [36])
class CarbonDashboardUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

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
    fun testDashboardInitialLayoutAndDetails() {
        try {
            // Set content mapping
            composeTestRule.setContent {
                CarbonDashboard(viewModel = viewModel)
            }
            
            testDispatcher.scheduler.advanceUntilIdle()
            composeTestRule.waitForIdle()

            // Verify key widgets are visible or properly tagged
            composeTestRule.onNodeWithTag("date_selector").assertIsDisplayed()
            
            // Verify main navigation bar container in viewport
            composeTestRule.onNodeWithTag("tab_navigation_bar").assertIsDisplayed()
            
            // Verify child tabs in viewport
            composeTestRule.onNodeWithTag("tab_calculator").assertIsDisplayed()
            composeTestRule.onNodeWithTag("tab_habits").assertIsDisplayed()
            composeTestRule.onNodeWithTag("tab_ai").assertIsDisplayed()
            composeTestRule.onNodeWithTag("tab_history").assertIsDisplayed()
        } catch (t: Throwable) {
            println("TEST_DIAGNOSTIC_FAILURE: ${t.message}")
            try {
                val tree = composeTestRule.onRoot().printToString()
                println("COMPOSE_SEMANTIC_TREE:\n$tree")
            } catch (inner: Throwable) {
                println("Failed to print tree: ${inner.message}")
            }
            throw t
        }
    }

    @Test
    fun testSwitchingTabs() {
        composeTestRule.setContent {
            CarbonDashboard(viewModel = viewModel)
        }

        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()

        // 1. Switch to habits tab
        composeTestRule.onNodeWithTag("tab_habits").performClick()
        composeTestRule.waitForIdle()
        
        // Let's verify a habit-specific UI element displays
        composeTestRule.onNodeWithTag("add_custom_habit_button").assertIsDisplayed()

        // 2. Switch to history tab
        composeTestRule.onNodeWithTag("tab_history").performClick()
        composeTestRule.waitForIdle()

        // 3. Switch to AI insights tab
        composeTestRule.onNodeWithTag("tab_ai").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_insights_button").assertIsDisplayed()
    }

    @Test
    fun testOpenProfileDialogAndEditing() {
        composeTestRule.setContent {
            CarbonDashboard(viewModel = viewModel)
        }

        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()

        // Open profile dialog by clicking user profile header custom row
        composeTestRule.onNodeWithTag("user_profile_header").performClick()
        composeTestRule.waitForIdle()

        // The profile inputs should now be displayed in the modal
        composeTestRule.onNodeWithTag("profile_name_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_budget_input").assertIsDisplayed()

        // Modify profile details
        composeTestRule.onNodeWithTag("profile_name_input").performTextClearance()
        composeTestRule.onNodeWithTag("profile_name_input").performTextInput("Green Champion")

        composeTestRule.onNodeWithTag("profile_budget_input").performTextClearance()
        composeTestRule.onNodeWithTag("profile_budget_input").performTextInput("12.5")

        // Confirm change
        composeTestRule.onNodeWithTag("confirm_profile_button").performClick()
        composeTestRule.waitForIdle()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify state is updated in VM
        assert(viewModel.userName.value == "Green Champion")
        assert(viewModel.dailyBudget.value == 12.5f)
    }

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
